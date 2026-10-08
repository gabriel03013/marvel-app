package com.example.marvel.ui

import android.text.TextUtils
import android.widget.TextView
import com.example.marvel.R
import com.example.marvel.data.*

private data class CharacterConnection(val relation: String, val item: ArchiveItem)

fun ScreenRenderer.detail() {
    val kind = vm.route.kind
    val id = vm.route.id.toIntOrNull() ?: 0
    val key = "$kind/$id"
    if (vm.connectionRecordKey != key) {
        vm.connectionRecordKey = key
        vm.connectionsExpanded = false
    }
    if (vm.details[key] == null) vm.loadDetail(kind, id)
    val remote = vm.details[key]
    title(
        remote?.value?.name
            ?: if (kind == "character") "Hero diary" else "${displayKind(kind)} dossier"
    )
    if (remote?.loading == true)
        state("Opening dossier", "Retrieving the complete archive record…", loading = true)
    if (remote?.error != null)
        state("Signal lost", remote.error, retry = { vm.loadDetail(kind, id, true) })
    val item = remote?.value ?: return
    image(item, action = { vm.navigate("image", kind, id.toString()) })
    if (kind == "character") {
        button("Add to Team", primary = true) { vm.addFromDetail(item) }
        dataPair("Real name", item.text("real_name").ifBlank { "Not documented" })
        dataPair("Publisher", item.reference("publisher")?.name ?: "Not documented")
        dataPair("Origin", item.reference("origin")?.name ?: "Not documented")
        val favorite = vm.favorites.any { it.id == id }
        actionRow(
                if (favorite) "Remove Favorite" else "Favorite",
                if (favorite) "Remove this character from your favorites."
                else "Keep this character in your collection.",
            ) {
                vm.favorite(item)
            }
            .apply {
                isEnabled = !vm.operationLoading
                alpha = if (isEnabled) 1f else .55f
            }
        actionRow("Investigate connections", "Cross-reference ties with another agent.") {
            vm.investigateA = item
            vm.navigate("investigate")
        }
        actionRow("Compare", "Place this dossier beside another character.") {
            vm.compareA = item
            vm.navigate("compare")
        }
        actionRow("Timeline", "Explore documented comic appearances.") {
            vm.timelineCharacter = item
            vm.timeline = Remote()
            vm.navigate("timeline")
            vm.loadTimeline()
        }
    }
    section("Overview")
    val description =
        plain(item.text("description").ifBlank { item.deck }).ifBlank {
            "No description available."
        }
    val overview = text(description)
    if (description.length > 900) {
        overview.maxLines = 12
        overview.ellipsize = TextUtils.TruncateAt.END
        val expand = actionRow("Read full overview") {}
        expand.setOnClickListener {
            val expanded = overview.maxLines == Int.MAX_VALUE
            overview.maxLines = if (expanded) 12 else Int.MAX_VALUE
            overview.ellipsize = if (expanded) TextUtils.TruncateAt.END else null
            expand.findViewById<TextView>(R.id.action_title).text =
                if (expanded) "Read full overview" else "Collapse overview"
        }
    }
    val first = item.reference("first_appeared_in_issue")
    when (kind) {
        "character" -> {
            val connections =
                buildList {
                        item.related("powers").forEach { add(CharacterConnection("Power", it)) }
                        item.related("teams").forEach { add(CharacterConnection("Team", it)) }
                        item.related("character_friends").forEach {
                            add(CharacterConnection("Ally", it))
                        }
                        item.related("character_enemies").forEach {
                            add(CharacterConnection("Enemy", it))
                        }
                        item
                            .reference("publisher")
                            ?.takeIf { it.id > 0 }
                            ?.let { add(CharacterConnection("Publisher", it)) }
                        first?.takeIf { it.id > 0 }?.let { add(CharacterConnection("First issue", it)) }
                        item.related("story_arc_credits").forEach {
                            add(CharacterConnection("Story arc", it))
                        }
                        item.related("issue_credits").forEach { add(CharacterConnection("Issue", it)) }
                        item.related("volume_credits").forEach { add(CharacterConnection("Volume", it)) }
                    }
                    .filter { it.item.id > 0 }
                    .distinctBy { "${it.item.kind}/${it.item.id}" }
                    .take(24)
            section("Connection records")
            if (connections.isEmpty()) {
                state(
                    "No connections documented",
                    "Comic Vine does not document direct relationships for this character.",
                )
            } else {
                val shown = connections.take(if (vm.connectionsExpanded) 24 else 8)
                shown.forEach { edge ->
                    actionRow(edge.item.name, edge.relation) { open(edge.item) }
                }
                if (!vm.connectionsExpanded && connections.size > shown.size)
                    button("Show more connections (${connections.size - shown.size} remaining)") {
                        vm.connectionsExpanded = true
                        vm.notifyChanged()
                    }
                else if (vm.connectionsExpanded && connections.size > 8)
                    actionRow("Show fewer connections") {
                        vm.connectionsExpanded = false
                        vm.notifyChanged()
                    }
            }
            section("Archive intelligence")
            val powerCount = item.related("powers").count { it.id > 0 }
            val teamCount = item.related("teams").count { it.id > 0 }
            val issueCount =
                item.text("count_of_issue_appearances")
                    .ifBlank { item.text("count_of_isssue_appearances") }
            if (powerCount > 0) dataPair("Powers listed", powerCount.toString())
            if (teamCount > 0) dataPair("Teams linked", teamCount.toString())
            if (issueCount.isNotBlank()) dataPair("Issue appearances", issueCount)
            if (first != null) dataPair("First appearance", first.name)
            section("First appearance")
            if (first != null && first.id > 0)
                actionRow(first.name, "Open the issue record") { open(first) }
            else if (first != null) dataPair("First appearance", first.name)
            else text("No data available")
            section("Archive facts")
            dataPair(
                "Issue appearances",
                item.text("count_of_issue_appearances").ifBlank { "Not documented" },
            )
            links("Powers", item.related("powers"))
            links("Teams", item.related("teams"))
            links("Allies", item.related("character_friends"))
            links("Enemies", item.related("character_enemies"))
            links("Story arcs", item.related("story_arc_credits"))
            links("Issues", item.related("issue_credits"))
            links("Volumes", item.related("volume_credits"))
        }
        "power" -> {
            dataPair("Linked characters", item.related("characters").size.toString())
            links("Characters with this power", item.related("characters"))
        }
        "team" -> {
            section("Team facts")
            dataPair("Aliases", item.text("aliases").ifBlank { "Not documented" })
            val issues =
                item.text("count_of_issue_appearances").ifBlank {
                    item.text("count_of_isssue_appearances")
                }
            dataPair("Issue appearances", issues.ifBlank { "Not documented" })
            if (first != null && first.id > 0)
                actionRow(first.name, "First appearance · Open issue") { open(first) }
            else if (first != null) dataPair("First appearance", first.name)
            button("Use this team as inspiration", !vm.operationLoading, primary = true) {
                if (vm.hasRecruitmentDraft) {
                    android.app.AlertDialog.Builder(activity)
                        .setTitle("Replace your active team?")
                        .setMessage(
                            "Using ${item.name} as inspiration clears your current recruitment draft. You can keep it and continue recruiting."
                        )
                        .setNegativeButton("Keep current team", null)
                        .setPositiveButton("Use inspiration") { _, _ -> vm.inspire(item) }
                        .show()
                } else {
                    vm.inspire(item)
                }
            }
            links("Members", item.related("characters").ifEmpty { item.related("members") })
            links("Issues", item.related("issue_credits"))
            links("Volumes", item.related("volume_credits"))
        }
        "story_arc" -> {
            links("Issues in this arc", item.related("issues"))
            links(
                "Characters",
                item.related("characters").ifEmpty { item.related("character_credits") },
            )
            links("Teams", item.related("teams").ifEmpty { item.related("team_credits") })
        }
        "issue" -> {
            section("Publication facts")
            dataPair("Issue number", item.text("issue_number").ifBlank { "Not documented" })
            dataPair("Cover date", item.text("cover_date").ifBlank { "Not documented" })
            dataPair("Release date", item.text("store_date").ifBlank { "Not documented" })
            item.reference("volume")?.let {
                if (it.id > 0) actionRow(it.name, "Open volume") { open(it) }
                else dataPair("Volume", it.name)
            }
            links("Characters appearing", item.related("character_credits"))
            section("Writers and artists")
            val people = item.related("person_credits")
            if (people.isEmpty()) text("No data available")
            people.forEach { dataPair(it.text("role").ifBlank { "Contributor" }, it.name) }
        }
        "volume" -> {
            section("Publication facts")
            dataPair("Issues", item.text("count_of_issues").ifBlank { "Not documented" })
            dataPair("Start year", item.text("start_year").ifBlank { "Not documented" })
            item.reference("publisher")?.let {
                if (it.id > 0) actionRow(it.name, "Open publisher") { open(it) }
                else dataPair("Publisher", it.name)
            }
            links("Issues", item.related("issues"))
            links("Characters", item.related("characters"))
        }
        "publisher" -> {
            links("Characters", item.related("characters"))
            links("Volumes", item.related("volumes"))
            links("Issues", item.related("issues"))
        }
        "location" -> {
            links("Characters", item.related("characters"))
            links("Issues", item.related("issue_credits"))
        }
    }
    note("Source: Comic Vine. Records may be incomplete.")
    actionRow("Refresh dossier", "Retrieve the latest record from Comic Vine.") {
            vm.loadDetail(kind, id, true)
        }
        .apply {
            isEnabled = remote.loading.not()
            alpha = if (isEnabled) 1f else .55f
        }
}
