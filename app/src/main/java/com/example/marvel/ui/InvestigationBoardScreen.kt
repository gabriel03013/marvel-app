package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.investigateBoard() {
    title("Classified intel board")
    val a = vm.investigateA
    val b = vm.investigateB
    if (a == null || b == null) {
        state("Choose two operatives", "Return to investigation setup.", retry = { vm.back() })
        return
    }

    listOf(a, b).forEach {
        if (vm.details["character/${it.id}"] == null) vm.loadDetail("character", it.id)
    }
    val left = vm.details["character/${a.id}"]
    val right = vm.details["character/${b.id}"]

    if (left?.loading == true || right?.loading == true) {
        state(
            "Cross-referencing archives",
            "Retrieving classified records for ${a.name} and ${b.name}…",
            loading = true,
        )
    }
    if (left?.error != null || right?.error != null) {
        state(
            "Signal lost",
            left?.error ?: right?.error.orEmpty(),
            retry = {
                vm.loadDetail("character", a.id, true)
                vm.loadDetail("character", b.id, true)
            },
        )
    }

    val first = left?.value ?: return
    val second = right?.value ?: return

    comparisonImages(first, second)

    val analysis = analyzeConnections(first, second)
    val statusLabel =
        when (analysis.directRelation) {
            DirectRelation.ARCH_RIVALS -> "HOSTILE TIES · DOCUMENTED ARCH-RIVALS"
            DirectRelation.DIRECT_ALLIES -> "ALLIED TIES · CANONICAL PARTNERS"
            DirectRelation.NONE ->
                when (analysis.degreeOfSeparation) {
                    1 -> "DEGREE 1 · SHARED TASKFORCE OPERATIVES"
                    2 -> "DEGREE 2 · INTERCONNECTED NETWORK"
                    else -> "DEGREE 3+ · NO DIRECT CANONICAL OVERLAP"
                }
        }
    note(
        "EYES ONLY · LEVEL 7 ARCHIVE CLEARANCE\nCROSS-REFERENCE: ${first.name.uppercase()} × ${second.name.uppercase()}\nSTATUS: $statusLabel"
    )

    section("S.H.I.E.L.D. tactical assessment")
    scorePanel(analysis.convergenceScore, analysis.tacticalAssessment)
    note(
        "App-generated intelligence analysis based on Comic Vine archival data, not official Marvel canonical rankings."
    )

    section("Direct canonical ties")
    when (analysis.directRelation) {
        DirectRelation.ARCH_RIVALS ->
            dataPair(
                "Canonical rivalry",
                "${first.name} and ${second.name} are documented adversaries in comic canon.",
            )
        DirectRelation.DIRECT_ALLIES ->
            dataPair(
                "Canonical alliance",
                "${first.name} and ${second.name} are documented allies in comic canon.",
            )
        DirectRelation.NONE ->
            dataPair(
                "Direct relationship",
                "No direct personal alliance or rivalry documented in these archives.",
            )
    }

    section("Shared taskforces & teams")
    if (analysis.sharedTeams.isNotEmpty()) {
        analysis.sharedTeams.forEach { team ->
            actionRow(team.name, "Shared team · Open dossier") { open(team) }
        }
    } else {
        text("No shared team memberships documented.")
    }

    section("Mutual major operations & story arcs")
    if (analysis.sharedStoryArcs.isNotEmpty()) {
        analysis.sharedStoryArcs.forEach { arc ->
            actionRow(arc.name, "Mutual story arc · Open record") { open(arc) }
        }
    } else {
        text("No shared major story arcs documented.")
    }

    section("Shared allies")
    if (analysis.sharedFriends.isNotEmpty()) {
        analysis.sharedFriends.take(6).forEach { ally ->
            actionRow(ally.name, "Mutual ally · Open dossier") { open(ally) }
        }
        if (analysis.sharedFriends.size > 6) {
            note("${analysis.sharedFriends.size - 6} more documented mutual allies.")
        }
    } else {
        text("No mutual allies documented.")
    }

    section("Shared adversaries")
    if (analysis.sharedEnemies.isNotEmpty()) {
        analysis.sharedEnemies.take(6).forEach { enemy ->
            actionRow(enemy.name, "Mutual adversary · Open dossier") { open(enemy) }
        }
        if (analysis.sharedEnemies.size > 6) {
            note("${analysis.sharedEnemies.size - 6} more documented mutual adversaries.")
        }
    } else {
        text("No mutual adversaries documented.")
    }

    section("Field actions")
    actionRow("Change operatives", "Cross-reference another pair of agents.") { vm.back() }
    actionRow("Recruit both operatives", "Add ${first.name} and ${second.name} to active roster.") {
        vm.addFromDetail(first)
        vm.addFromDetail(second)
        vm.navigate("assembly")
    }
}
