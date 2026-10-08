package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.archiveList() {
    val kind = vm.route.kind
    val label = displayKind(ComicVineRepository.plural(kind))
    title("$label archive")
    if (vm.submittedQueries["archive_$kind"] == null)
        note(
            when (kind) {
                "power" -> "Explore documented abilities and their linked characters."
                "team" -> "Explore team histories and documented members."
                "story_arc" -> "Follow stories through their connected issues and characters."
                "issue" -> "Browse comic issues, publication dates and creator credits."
                "volume" -> "Find comic series and the issues collected within them."
                "publisher" -> "Explore publishers and their linked comic records."
                "location" -> "Explore places documented in the comic archive."
                else -> "Open a character dossier to discover their history and connections."
            }
        )
    val context = "archive_$kind"
    searchField(context, "Search ${label.lowercase()}…") { query ->
        vm.submittedQueries[context] = query
        vm.loadPage(kind, query, refresh = true)
    }
    val query = vm.submittedQueries[context].orEmpty()
    if (vm.pages[vm.pageKey(kind, query)] == null) vm.loadPage(kind, query)
    results(kind, query)
}
