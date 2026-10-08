package com.example.marvel.ui

import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun ArchiveViewModel.startMission(template: Mission) {
    cancelReportGeneration()
    mission = template
    roster.clear()
    teamName = ""
    customBriefing = ""
    report = null
    teamSuggestion = Remote()
    simulationSource = null
    navigate("briefing", id = template.id)
}

fun ArchiveViewModel.select(item: ArchiveItem) {
    cancelReportGeneration()
    when (selectionPurpose) {
        "compareA" -> {
            compareA = item
            back()
        }
        "compareB" -> {
            compareB = item
            back()
        }
        "investigateA" -> {
            investigateA = item
            back()
        }
        "investigateB" -> {
            investigateB = item
            back()
        }
        "timeline" -> {
            timelineCharacter = item
            timeline = Remote()
            back()
            loadTimeline()
        }
        else -> {
            report = null
            teamSuggestion = Remote()
            if (roster.any { it.id == item.id }) roster.removeAll { it.id == item.id }
            else if (roster.size < mission.size) roster += item
            else message = "This mission allows up to ${mission.size} members."
        }
    }
    notifyChanged()
}

fun ArchiveViewModel.addFromDetail(item: ArchiveItem) {
    cancelReportGeneration()
    teamSuggestion = Remote()
    if (roster.none { it.id == item.id } && roster.size < mission.size) {
        report = null
        roster += item
        message = "${item.name} added to your active team"
    } else
        message =
            if (roster.any { it.id == item.id }) "Already on your active team"
            else "Your active team is full. Remove a member in Recruit."
    notifyChanged()
}

fun ArchiveViewModel.removeRecruit(item: ArchiveItem) {
    cancelReportGeneration()
    teamSuggestion = Remote()
    report = null
    roster.removeAll { it.id == item.id }
    notifyChanged()
}

fun ArchiveViewModel.suggestMissionTeam() {
    if (teamSuggestion.loading) return
    val origin = route
    teamSuggestion = Remote(teamSuggestion.value, true)
    notifyChanged()
    viewModelScope.launch {
        val candidates =
            (favorites + recent + roster + pages.values.flatMap { it.value?.items.orEmpty() })
                .filter { it.kind == "character" && it.id > 0 }
                .distinctBy { it.id }
                .take(12)
        if (candidates.isEmpty()) {
            teamSuggestion =
                Remote(
                    error =
                        "Search for characters or save favorites first, then ask for a suggestion."
                )
            notifyChanged()
            return@launch
        }
        val dossiers = mutableListOf<ArchiveItem>()
        var failures = 0
        for (candidate in candidates) {
            if (route != origin) {
                teamSuggestion = Remote()
                notifyChanged()
                return@launch
            }
            try {
                dossiers += api.detail("character", candidate.id)
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                failures++
            }
        }
        if (route != origin) {
            teamSuggestion = Remote()
            notifyChanged()
            return@launch
        }
        val result = suggestTeam(dossiers, mission)
        pendingSuggestionIds = result.members.map { it.id }.toMutableSet()
        teamSuggestion =
            if (dossiers.isEmpty())
                Remote(
                    error =
                        "Character dossiers could not be loaded. Retry or continue recruiting manually."
                )
            else
                Remote(
                    result,
                    error =
                        if (result.members.isEmpty())
                            "The loaded dossiers do not contain enough documented power data for this mission. You can continue manually."
                        else if (failures > 0)
                            "Some dossiers could not be loaded. This suggestion uses the available records."
                        else null,
                )
        notifyChanged()
    }
}

fun ArchiveViewModel.acceptSuggestion() {
    val suggested = teamSuggestion.value ?: return
    if (suggested.members.isEmpty()) return
    roster.clear()
    roster.addAll(suggested.members.take(mission.size))
    report = null
    message = "Suggested roster added. Review, remove, or add members before continuing."
    notifyChanged()
}

fun ArchiveViewModel.reviseSuggestedMember(item: ArchiveItem) {
    val suggestion = teamSuggestion.value ?: return
    if (!pendingSuggestionIds.add(item.id)) pendingSuggestionIds.remove(item.id)
    if (pendingSuggestionIds.size > mission.size) {
        pendingSuggestionIds.remove(item.id)
        message = "This mission allows up to ${mission.size} members in a suggested roster."
    }
    teamSuggestion =
        teamSuggestion.copy(value = reviseSuggestion(suggestion, pendingSuggestionIds, mission))
    notifyChanged()
}

fun ArchiveViewModel.runSimulation() {
    if (roster.isEmpty() || operationLoading) return
    simulationError = null
    val missionCopy = mission
    val origin = route
    val ids = roster.map { it.id }
    operationLoading = true
    notifyChanged()
    viewModelScope.launch {
        try {
            val members = ids.map { api.detail("character", it) }
            if (route != origin || mission != missionCopy || roster.map { it.id } != ids)
                return@launch
            roster.clear()
            roster.addAll(members)
            simulationSource =
                SavedTeam(
                    "simulation",
                    teamName.ifBlank { "Untitled team" },
                    missionCopy.id,
                    if (missionCopy.id == "custom") customBriefing else missionCopy.description,
                    members,
                    System.currentTimeMillis(),
                )
            navigate("simulation")
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            simulationError =
                "Simulation could not retrieve all dossiers. Check your connection and retry."
            message = simulationError
        } finally {
            operationLoading = false
            notifyChanged()
        }
    }
}

fun ArchiveViewModel.createReportFromSimulation() {
    val source = simulationSource ?: return
    if (teamName.isBlank()) {
        message = "Name your team before generating the mission report."
        notifyChanged()
        return
    }
    report =
        source.copy(
            id = UUID.randomUUID().toString(),
            name = teamName.trim(),
            createdAt = System.currentTimeMillis(),
        )
    navigate("report", id = report!!.id)
}

fun ArchiveViewModel.cancelReportGeneration() {
    reportAttempt++
    reportJob?.let { job ->
        reportJob = null
        job.cancel()
        operationLoading = false
        notifyChanged()
    }
}

fun ArchiveViewModel.generateReport() {
    if (operationLoading || route.screen != "assembly") return
    if (teamName.isBlank() || roster.isEmpty()) {
        message = "Name your team and recruit at least one member."
        notifyChanged()
        return
    }
    val uid = user?.uid ?: return
    val selected = roster.toList()
    val name = teamName.trim()
    val selectedMission = mission
    val briefing = customBriefing
    val attempt = ++reportAttempt
    val origin = route
    operationLoading = true
    notifyChanged()
    reportJob =
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val complete = selected.map { api.detail("character", it.id) }
                if (
                    user?.uid != uid ||
                        attempt != reportAttempt ||
                        route != origin ||
                        teamName.trim() != name ||
                        mission != selectedMission ||
                        customBriefing != briefing ||
                        roster.map { it.id } != selected.map { it.id }
                )
                    return@launch
                roster.clear()
                roster.addAll(complete)
                report =
                    SavedTeam(
                        UUID.randomUUID().toString(),
                        name,
                        selectedMission.id,
                        if (selectedMission.id == "custom") briefing
                        else selectedMission.description,
                        complete,
                        System.currentTimeMillis(),
                    )
                reportJob = null
                operationLoading = false
                navigate("report", id = report!!.id)
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                if (attempt == reportAttempt && user?.uid == uid)
                    message = "Report could not be generated. Check your connection and retry."
            } finally {
                if (attempt == reportAttempt) {
                    reportJob = null
                    operationLoading = false
                    notifyChanged()
                }
            }
        }
    reportJob?.start()
}

fun ArchiveViewModel.saveReport() {
    val uid = user?.uid ?: return
    val team = report ?: return
    if (operationLoading) return
    operationLoading = true
    notifyChanged()
    viewModelScope.launch {
        try {
            withTimeout(20_000) { users.saveTeam(uid, team) }
            if (user?.uid == uid) message = "Team and mission saved"
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            if (user?.uid == uid)
                message =
                    "Team has not synced yet. Your report is still here and changes may be queued locally; check your connection and retry."
        } finally {
            operationLoading = false
            notifyChanged()
        }
    }
}

fun ArchiveViewModel.deleteTeam(team: SavedTeam) {
    val uid = user?.uid ?: return
    viewModelScope.launch {
        try {
            withTimeout(20_000) { users.deleteTeam(uid, team.id) }
            if (user?.uid == uid) {
                message = "Saved team deleted. Mission history retained."
                if (route.screen == "saved-team" && route.id == team.id) back()
            }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            if (user?.uid == uid)
                message =
                    "Deletion has not synced yet. It may be queued locally; check your connection and retry."
        }
        notifyChanged()
    }
}

fun ArchiveViewModel.inspire(team: ArchiveItem) {
    val uid = user?.uid ?: return
    operationLoading = true
    notifyChanged()
    viewModelScope.launch {
        try {
            val ids =
                team.related("characters").ifEmpty { team.related("members") }.take(mission.size)
            check(ids.isNotEmpty()) { "No team members are documented in this record." }
            val members = ids.map { api.detail("character", it.id) }
            if (user?.uid != uid) return@launch
            roster.clear()
            roster.addAll(members)
            report = null
            selectionPurpose = "mission"
            navigate("picker")
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            message = e.message
        } finally {
            operationLoading = false
            notifyChanged()
        }
    }
}
