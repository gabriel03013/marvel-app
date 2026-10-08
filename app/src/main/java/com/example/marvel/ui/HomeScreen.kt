package com.example.marvel.ui

import com.example.marvel.data.missions

fun ScreenRenderer.home() {
    val user = vm.user
    val firstName =
        user?.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "Agent"
    title("Welcome, $firstName.")
    profileRow { vm.navigate("profile") }
    button("Search Characters", primary = true) { vm.destination("search") }
    note("Open a character dossier, discover their story and recruit them to your team.")

    if (vm.hasRecruitmentDraft) {
        section("Your active mission")
        actionRow(
            "Continue Recruitment",
            "${vm.mission.title} · ${vm.roster.size} of ${vm.mission.size} members",
        ) {
            vm.selectionPurpose = "mission"
            vm.navigate(if (vm.roster.isEmpty()) "picker" else "assembly")
        }
    }

    section("Answer the call")
    missionCard(missions.first(), true)
    actionRow("Explore Archives", "Follow powers, teams, issues and story arcs.") {
        vm.destination("archives")
    }

    section("Your archive")
    actionRow(
        "Collect Booster Cards",
        "Open original packs and save live Comic Vine character dossiers.",
    ) {
        vm.destination("booster-shop")
    }
    syncStatus()

    val hasNoCollectionData =
        vm.recent.isEmpty() &&
            vm.favorites.isEmpty() &&
            vm.teams.isEmpty() &&
            vm.collectedCharacters.isEmpty() &&
            !vm.collectionLoading &&
            vm.persistenceError == null &&
            !vm.collectionCached
    if (hasNoCollectionData) {
        text("Save a favorite or complete a mission to begin your collection.")
    }

    vm.teams.firstOrNull()?.let { team ->
        section("Latest saved team")
        teamCard(team) { vm.navigate("saved-team", id = team.id) }
    }

    if (vm.favorites.isNotEmpty()) {
        section("Favorite characters")
        vm.favorites.take(3).forEach { character(it) }
    }

    if (vm.recent.isNotEmpty()) {
        section("Recently opened")
        note("Dossier snapshots stored on this device. Open one to retrieve its latest record.")
        vm.recent.take(3).forEach { character(it) }
    }

    actionRow("Open My Collection", "Favorites, saved teams and mission history.") {
        vm.destination("collection")
    }
}
