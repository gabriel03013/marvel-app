package com.example.marvel.ui

import com.example.marvel.data.boosterTypes

fun ScreenRenderer.boosterOpening() {
    val type = boosterTypes.firstOrNull { it.id == vm.route.id }
    if (type == null) {
        title("Booster unavailable")
        state(
            "Pack not found",
            "Return to the booster shop and choose a pack.",
            retry = { vm.destination("booster-shop") },
        )
        return
    }
    title(type.title)
    boosterArt(type, opened = false, height = 300).also { content.addView(it) }
    syncStatus()
    section("Sealed dossier pack")
    text(type.description)
    dataPair("Character cards inside", type.cardCount.toString())
    dataPair("Boosters owned", (vm.boosters[type.id]?.count ?: 0L).toString())
    if (vm.currentPull?.first?.id == type.id) {
        state(
            "Booster opened",
            "Your characters were added to the collection. Continue revealing the cards.",
        )
        button("Continue revealing cards", primary = true) {
            vm.navigate("booster-reveal", id = type.id)
        }
    } else {
        vm.boosterError?.let {
            state("Signal lost", it, loading = vm.boosterLoading, retry = { vm.openBooster(type) })
        }
        if (vm.boosterLoading)
            state(
                "Opening your booster",
                "Retrieving character records and saving your cards…",
                loading = true,
            )
        button(
            if (vm.boosterLoading) "Opening booster…" else "Open booster",
            enabled = !vm.boosterLoading && (vm.boosters[type.id]?.count ?: 0L) > 0L,
            primary = true,
        ) {
            vm.openBooster(type)
        }
        button("Get another booster", primary = false) { vm.destination("booster-shop") }
    }
}
