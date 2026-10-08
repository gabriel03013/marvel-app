package com.example.marvel.ui

import com.example.marvel.data.boosterTypes

fun ScreenRenderer.boosterShop() {
    title("Booster shop")
    text(
        "Collect five character dossiers in every pack. Add as many boosters as you like; no credits are required."
    )
    note(
        "Cards are selected from live Comic Vine character records. Pack themes and collection levels are app-made labels, not official character rankings."
    )
    button("View card collection", primary = false) {
        vm.collectionTab = "Cards"
        vm.destination("collection")
    }
    syncStatus()
    vm.boosterError?.let { error ->
        state("Booster update failed", error, retry = { vm.retryBoosterAction() })
    }
    section("Choose a pack")
    boosterTypes.forEach { type ->
        val inventory = vm.boosters[type.id]?.count ?: 0L
        boosterTile(type, inventory)
    }
}
