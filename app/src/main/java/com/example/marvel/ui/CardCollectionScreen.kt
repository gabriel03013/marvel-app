package com.example.marvel.ui

import com.example.marvel.data.boosterTypes

fun ScreenRenderer.cardCollection() {
    section("Booster packs")
    boosterTypes.forEach { type ->
        val count = vm.boosters[type.id]?.count ?: 0L
        actionRow(type.title, "$count owned · ${type.cardCount} character cards per pack") {
            vm.selectBooster(type)
        }
    }
    button("Get more boosters", primary = true) { vm.destination("booster-shop") }
    section("Character cards")
    val cards = vm.collectedCharacters
    if (cards.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        state(
            "No cards collected yet",
            "Open a booster to add real character dossiers to your collection.",
        )
        button("Browse boosters", primary = true) { vm.destination("booster-shop") }
    }
    cards.forEach { collected ->
        character(collected.item)
        dataPair("Collection level · app-generated", collected.level.toString())
        dataPair("Copies collected", collected.copies.toString())
    }
}
