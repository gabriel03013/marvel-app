package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.archives() {
    title("The archives")
    text("Characters, abilities and the stories that connect them. Data from Comic Vine.")
    val categories =
        listOf(
            Triple("character", "Characters", "Identities, histories and documented appearances."),
            Triple("power", "Powers", "Abilities and the characters linked to them."),
            Triple("team", "Teams", "Members, origins and shared comic appearances."),
            Triple("story_arc", "Story Arcs", "Connected stories, issues and participants."),
            Triple("issue", "Issues", "Comic covers, publication dates and credits."),
            Triple("volume", "Volumes", "Comic series and the issues they collect."),
            Triple("publisher", "Publishers", "Publishing records and their linked titles."),
            Triple("location", "Locations", "Places and their documented comic connections."),
        )
    categories.forEachIndexed { index, (kind, label, copy) ->
        when (index) {
            1 -> section("Abilities & affiliations")
            3 -> section("On the comic shelf")
            6 -> section("People & places")
        }
        if (index == 0) category(label, copy, "red") { vm.navigate("archive-list", kind) }
        else actionRow(label, copy) { vm.navigate("archive-list", kind) }
    }
    section("Featured publisher")
    actionRow("Marvel", "Open the publisher record in Comic Vine.") {
        vm.navigate("detail", "publisher", "31")
    }
    section("Research tools")
    actionRow("Investigation board", "Cross-reference classified ties between two agents.") {
        vm.navigate("investigate")
    }
    actionRow("Compare characters", "Place two dossiers side by side.") { vm.navigate("compare") }
    actionRow("Character timeline", "Follow appearances with documented dates.") {
        vm.navigate("timeline")
    }
    section("Recently viewed")
    if (vm.recent.isEmpty()) text("Opened character dossiers will appear here.")
    vm.recent.take(4).forEach { character(it) }
}
