package com.example.marvel.ui

fun ScreenRenderer.collection() {
    title("My collection")

    val fixedTab =
        when (vm.route.screen) {
            "favorites" -> "Favorites"
            "saved-teams" -> "Saved Teams"
            "mission-history" -> "Mission History"
            "recent" -> "Recently Viewed"
            else -> null
        }
    val currentTab = fixedTab ?: vm.collectionTab

    tabs(
        listOf("Cards", "Favorites", "Saved Teams", "Mission History", "Recently Viewed"),
        currentTab,
    ) { tab ->
        vm.collectionTab = tab
        vm.destination("collection")
    }

    val count = collectionCountForTab(currentTab)
    dataPair(
        collectionLabelForTab(currentTab),
        if (currentTab == "Recently Viewed") count.toString() else collectionCount(count),
    )
    if (currentTab != "Recently Viewed") syncStatus()

    when (currentTab) {
        "Cards" -> cardCollection()
        "Favorites" -> showFavorites()
        "Saved Teams",
        "Mission History" -> showTeams(currentTab)
        "Recently Viewed" -> showRecentlyViewed()
    }
}

fun ScreenRenderer.collectionCount(size: Long): String =
    when {
        vm.collectionLoading -> if (size > 0) "$size loaded · syncing…" else "Loading…"
        vm.persistenceError != null ->
            if (size > 0) "$size loaded · sync interrupted" else "Unavailable until sync completes"
        vm.collectionPending -> "$size on this device · waiting to sync"
        vm.collectionCached -> "$size cached · may be out of date"
        else -> size.toString()
    }

private fun ScreenRenderer.collectionCountForTab(tab: String): Long =
    when (tab) {
        "Cards" -> vm.collectedCharacters.sumOf { it.copies }
        "Favorites" -> vm.favorites.size.toLong()
        "Saved Teams" -> vm.teams.size.toLong()
        "Mission History" -> vm.history.size.toLong()
        else -> vm.recent.size.toLong()
    }

private fun collectionLabelForTab(tab: String): String =
    when (tab) {
        "Cards" -> "Character cards"
        "Favorites" -> "Favorite dossiers"
        "Saved Teams" -> "Saved teams"
        "Mission History" -> "Completed missions"
        else -> "Dossiers on this device"
    }

private fun ScreenRenderer.showFavorites() {
    if (vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        val heading = if (vm.collectionCached) "No favorites cached" else "Your archive is empty"
        val message =
            if (vm.collectionCached)
                "Your latest favorites will appear when Firebase syncs. Search the live archive in the meantime."
            else "Find a character and tap Favorite to save their dossier."
        state(heading, message)
        button("Search Characters", primary = true) { vm.destination("search") }
    }

    vm.favorites.forEach { member ->
        character(member)
        button("Remove favorite", !vm.operationLoading) { vm.favorite(member) }.contentDescription =
            "Remove ${member.name} from favorites"
    }
}

private fun ScreenRenderer.showTeams(tab: String) {
    val teams = if (tab == "Saved Teams") vm.teams else vm.history
    if (teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        val heading =
            when {
                vm.collectionCached -> "No reports cached"
                tab == "Saved Teams" -> "No saved teams yet"
                else -> "No completed missions"
            }
        val message =
            if (vm.collectionCached)
                "Your current reports will appear when Firebase syncs. You can begin a new mission now."
            else "Recruit a team and save its report to keep the mission here."
        state(heading, message)
        button("Start a Mission", primary = true) { vm.destination("recruit") }
    }

    teams.forEach { team ->
        teamCard(team) { vm.navigate("saved-team", id = team.id) }
        if (tab == "Saved Teams") {
            button("Delete saved team") { confirmDelete(team) }.contentDescription =
                "Delete saved team ${team.name}"
        }
    }
}

private fun ScreenRenderer.showRecentlyViewed() {
    note("Stored on this device. These dossier snapshots may be out of date.")
    if (vm.recent.isEmpty()) {
        state("No dossiers viewed yet", "Open a character to start a local reading history.")
        button("Search Characters", primary = true) { vm.destination("search") }
    }
    vm.recent.forEach { character(it) }
}
