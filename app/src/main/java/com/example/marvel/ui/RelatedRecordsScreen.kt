package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.related() {
    title(vm.relatedHeading)
    note("${vm.relatedItems.size} linked records")
    if (vm.relatedItems.isEmpty())
        state("No linked records", "Open a dossier and choose a related section.")
    vm.relatedItems.take(vm.relatedLimit).forEach { character(it) }
    if (vm.relatedItems.size > vm.relatedLimit) {
        button("Load more linked records") { vm.loadMoreRelated() }
    }
}
