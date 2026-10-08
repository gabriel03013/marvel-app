package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.compareSetup() {
    title("Compare dossiers")
    text(
        "Place two characters side by side. Compare documented facts, with missing data shown as unknown."
    )
    section("Character A")
    vm.compareA?.let { character(it) } ?: text("Choose your first character.")
    actionRow(if (vm.compareA == null) "Select Character A" else "Change Character A") {
        vm.selectionPurpose = "compareA"
        vm.navigate("character-select")
    }
    section("Character B")
    vm.compareB?.let { character(it) } ?: text("Choose your second character.")
    actionRow(if (vm.compareB == null) "Select Character B" else "Change Character B") {
        vm.selectionPurpose = "compareB"
        vm.navigate("character-select")
    }
    actionRow("Swap characters", "Exchange the A and B positions.") {
            val a = vm.compareA
            vm.compareA = vm.compareB
            vm.compareB = a
            vm.notifyChanged()
        }
        .apply {
            isEnabled = vm.compareA != null || vm.compareB != null
            alpha = if (isEnabled) 1f else .55f
        }
    val ready = vm.compareA != null && vm.compareB != null && vm.compareA?.id != vm.compareB?.id
    if (vm.compareA != null && vm.compareA?.id == vm.compareB?.id)
        note("Choose two different characters.")
    button("Compare Profiles", ready, primary = true) { vm.navigate("compare-result") }
}

fun ScreenRenderer.compareResult() {
    title("Profile comparison")
    val a = vm.compareA
    val b = vm.compareB
    if (a == null || b == null) {
        state("Choose two characters", "Return to comparison setup.", retry = { vm.back() })
        return
    }
    listOf(a, b).forEach {
        if (vm.details["character/${it.id}"] == null) vm.loadDetail("character", it.id)
    }
    val left = vm.details["character/${a.id}"]
    val right = vm.details["character/${b.id}"]
    if (left?.loading == true || right?.loading == true)
        state("Comparing profiles", "Retrieving both complete dossiers…", true)
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
    note("A: ${first.name} · B: ${second.name}")
    val rows =
        listOf(
            "Real name" to (first.text("real_name") to second.text("real_name")),
            "Publisher" to
                (first.reference("publisher")?.name.orEmpty() to
                    second.reference("publisher")?.name.orEmpty()),
            "First appearance" to
                (first.reference("first_appeared_in_issue")?.name.orEmpty() to
                    second.reference("first_appeared_in_issue")?.name.orEmpty()),
            "Issue appearances" to
                (first.text("count_of_issue_appearances") to
                    second.text("count_of_issue_appearances")),
            "Powers" to
                (first.related("powers").joinToString { it.name } to
                    second.related("powers").joinToString { it.name }),
            "Teams" to
                (first.related("teams").joinToString { it.name } to
                    second.related("teams").joinToString { it.name }),
        )
    rows.forEach { (heading, pair) ->
        section(heading)
        comparisonRow(
            "A\n${pair.first.ifBlank { "Not documented" }}",
            "B\n${pair.second.ifBlank { "Not documented" }}",
        )
    }
    val p1 = first.related("powers").map { it.name }.toSet()
    val p2 = second.related("powers").map { it.name }.toSet()
    section("Shared documented powers")
    text(
        p1.intersect(p2).joinToString().ifBlank { "No shared powers documented in these records." }
    )
    section("Differences")
    dataPair(
        "A · ${first.name}",
        p1.minus(p2).joinToString().ifBlank { "No additional powers documented" },
    )
    dataPair(
        "B · ${second.name}",
        p2.minus(p1).joinToString().ifBlank { "No additional powers documented" },
    )
    val allPowers = p1.union(p2)
    val sharedPowers = p1.intersect(p2)
    val affinityScore = if (allPowers.isEmpty()) 0 else (sharedPowers.size * 100) / allPowers.size
    section("App-generated comparison")
    scorePanel(
        affinityScore,
        "${sharedPowers.size} shared of ${allPowers.size} combined documented powers",
    )
    note("This compares Comic Vine records, not combat strength or official Marvel rankings.")
    actionRow("Change characters") { vm.back() }
}
