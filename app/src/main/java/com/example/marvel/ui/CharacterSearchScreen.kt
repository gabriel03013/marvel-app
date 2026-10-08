package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.search() {
    val global = vm.route.screen == "global-search"
    title(if (global) "Quick search" else "Find a character")
    if (vm.submittedQueries["search"] == null && !global)
        text("Search character names and open their dossiers.")
    val context = "search"
    searchField(context, "Search characters…") { query ->
        vm.submittedQueries[context] = query
        vm.loadPage("character", query, refresh = true)
    }
    tabs(
        listOf("All", "Marvel", "Heroes", "Villains"),
        if (vm.marvelOnly) "Marvel" else "All",
        setOf("Heroes", "Villains"),
    ) {
        vm.marvelOnly = it == "Marvel"
        vm.notifyChanged()
    }

    val submitted = vm.submittedQueries[context]
    if (submitted != null) {
        if (submitted.isBlank()) {
            actionRow(
                if (vm.sort == "name:asc") "Sort: Name A–Z" else "Sort: Name Z–A",
                "Tap to reverse the alphabetical order.",
            ) {
                vm.sort = if (vm.sort == "name:asc") "name:desc" else "name:asc"
                vm.loadPage("character", "", refresh = true)
            }
        } else {
            note("Search results use Comic Vine relevance order.")
        }
        results("character", submitted)
        note(
            "Alignment filters are unavailable: Comic Vine does not reliably document heroes and villains."
        )
        return
    }

    if (vm.searches.isNotEmpty()) {
        section("Recent searches")
        vm.searches.forEach { query ->
            actionRow(query, "Search again") {
                vm.queries[context] = query
                vm.submittedQueries[context] = query
                vm.loadPage("character", query)
            }
        }
    }
    section("Start with a name")
    listOf("Spider-Man", "Captain Marvel", "Black Panther").forEach { query ->
        actionRow(query, "Search Comic Vine") {
            vm.queries[context] = query
            vm.submittedQueries[context] = query
            vm.loadPage("character", query)
        }
    }
    category("Browse characters", "Explore the complete character archive.", "blue") {
        vm.submittedQueries[context] = ""
        vm.loadPage("character", "")
    }
    section("From your collection")
    syncStatus()
    vm.favorites.take(3).forEach { character(it) }
    if (vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        text("Favorite a character to keep a dossier close at hand.")
    }
}
