package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.recruit() {
    title("Recruit your team")
    text("Choose a mission. Find your recruits. Bring their evidence together.")
    if (vm.hasRecruitmentDraft) {
        section("Your active mission")
        dataPair(vm.mission.title, "${vm.roster.size} of ${vm.mission.size} members selected")
        if (vm.teamName.isNotBlank()) text(vm.teamName)
        button("Continue Recruitment", primary = true) {
            vm.selectionPurpose = "mission"
            vm.navigate(if (vm.roster.isEmpty()) "picker" else "assembly")
        }
        if (vm.mission.id == "custom" && vm.customBriefing.isNotBlank())
            note("Objective: ${vm.customBriefing}")
        vm.roster.take(2).forEach { character(it) }
        if (vm.roster.size > 2) note("${vm.roster.size - 2} more members in your active team")
    }
    section("Choose a briefing")
    missionCard(missions.first(), featured = true)
    missions.drop(1).forEach { mission ->
        actionRow(
            mission.title,
            if (mission.id == "custom") "Write your own objective · Up to ${mission.size} members"
            else "${mission.focus.joinToString(" · ")} · Up to ${mission.size} members",
        ) {
            chooseMission(mission)
        }
    }
    section("Saved teams")
    syncStatus()
    if (vm.teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        text(
            if (vm.collectionCached)
                "No team reports are cached on this device. Firebase will retrieve your collection when it syncs."
            else "Save a completed report to keep your team here."
        )
    }
    vm.teams.take(3).forEach { team -> teamCard(team) { vm.navigate("saved-team", id = team.id) } }
}
