package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.investigateSetup() {
    title("Investigation board")
    text("S.H.I.E.L.D. Intelligence Nexus · Level 7 clearance.")
    note(
        "Cross-reference classified field dossiers between two agents to reveal direct canon alliances, mutual taskforces, shared story arcs, and covert connections."
    )

    section("Operative Alpha")
    vm.investigateA?.let { character(it) } ?: text("Choose your first operative.")
    actionRow(if (vm.investigateA == null) "Select Operative Alpha" else "Change Operative Alpha") {
        vm.selectionPurpose = "investigateA"
        vm.navigate("character-select")
    }

    section("Operative Beta")
    vm.investigateB?.let { character(it) } ?: text("Choose your second operative.")
    actionRow(if (vm.investigateB == null) "Select Operative Beta" else "Change Operative Beta") {
        vm.selectionPurpose = "investigateB"
        vm.navigate("character-select")
    }

    actionRow("Swap operatives", "Exchange Alpha and Beta positions.") {
            val a = vm.investigateA
            vm.investigateA = vm.investigateB
            vm.investigateB = a
            vm.notifyChanged()
        }
        .apply {
            isEnabled = vm.investigateA != null || vm.investigateB != null
            alpha = if (isEnabled) 1f else .55f
        }

    section("Quick case inquiries")
    val presets =
        listOf(
            Triple("Civil War veterans", "Iron Man & Captain America", 1455 to 1442),
            Triple("Mystic & mutant tie", "Wolverine & Doctor Strange", 1440 to 1453),
            Triple("Asgardian rivalry", "Thor & Loki", 2268 to 4326),
            Triple("Symbiote war", "Spider-Man & Venom", 1443 to 2074),
        )
    presets.forEach { (caseTitle, operatives, ids) ->
        actionRow(caseTitle, operatives) {
            vm.investigateA = ArchiveItem(ids.first, "character", operatives.substringBefore(" & "))
            vm.investigateB = ArchiveItem(ids.second, "character", operatives.substringAfter(" & "))
            vm.navigate("investigate-board")
        }
    }

    val ready =
        vm.investigateA != null &&
            vm.investigateB != null &&
            vm.investigateA?.id != vm.investigateB?.id
    if (vm.investigateA != null && vm.investigateA?.id == vm.investigateB?.id) {
        note("Choose two different operatives to cross-reference.")
    }
    button("Analyze Field Connections", ready, primary = true) {
        vm.navigate("investigate-board")
    }
}
