package com.example.marvel.ui

fun ScreenRenderer.boosterReveal() {
    val pull = vm.currentPull
    if (pull == null || pull.second.isEmpty()) {
        title("No cards to reveal")
        state(
            "Booster reveal unavailable",
            "Open a booster from your collection to see its character cards.",
            retry = { vm.destination("booster-shop") },
        )
        return
    }
    val (type, cards) = pull
    val index = vm.revealIndex.coerceIn(0, cards.lastIndex)
    val card = cards[index]
    title("Card reveal")
    boosterArt(type, opened = true, height = 136).also { content.addView(it) }
    note("${type.title} · Card ${index + 1} of ${cards.size}")
    image(card)
    section(card.name)
    dataPair("Publisher", card.reference("publisher")?.name ?: "Not documented")
    val collectionLevel =
        vm.currentPullLevels[card.id]
            ?: vm.collectedCharacters.firstOrNull { it.item.id == card.id }?.level
            ?: 1L
    dataPair("Collection level · app-generated", collectionLevel.toString())
    note(
        "Level reflects repeated card copies in this app. It is not a canonical or official power ranking."
    )
    button("Open character dossier", primary = false) { open(card) }
    if (index < cards.lastIndex) {
        button("Reveal next card", primary = true) { vm.continueReveal() }
    } else {
        state(
            "Booster complete",
            "All ${cards.size} character cards have been added to your collection.",
        )
        button("View card collection", primary = true) { vm.finishReveal() }
        button("Get another booster", primary = false) {
            vm.currentPull = null
            vm.currentPullLevels = emptyMap()
            vm.revealIndex = 0
            vm.destination("booster-shop")
        }
    }
}
