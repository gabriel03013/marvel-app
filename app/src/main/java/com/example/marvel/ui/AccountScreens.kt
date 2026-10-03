package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.home() {
    val user = vm.user
    title("Welcome,\n${user?.displayName?.substringBefore(' ') ?: "Agent"}.")
    text("Your next mission starts here.")
    profileRow { vm.navigate("profile") }
    if(vm.roster.isNotEmpty() || vm.teamName.isNotBlank()) {
        actionRow("Continue Recruitment", "${vm.mission.title} · ${vm.roster.size}/${vm.mission.size} members") {
            vm.selectionPurpose = "mission"; vm.navigate(if(vm.roster.isEmpty()) "picker" else "assembly")
        }
    }
    missionCard(missions.first(), true)
    actionRow("Search Characters", "Find a name. Open a dossier.") { vm.destination("search") }
    actionRow("Explore Archives", "Powers, teams, issues and the links between them.") { vm.destination("archives") }
    syncStatus()
    section("Recent dossiers")
    if(vm.recent.isEmpty()) text("Open a character dossier to start your archive trail.")
    vm.recent.take(3).forEach { character(it) }
    section("Favorites")
    if(vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) text("Keep your favorite characters close. Save one from its Hero Diary.")
    vm.favorites.take(3).forEach { character(it) }
    section("Latest saved team")
    vm.teams.firstOrNull()?.let { team -> teamCard(team) { vm.navigate("saved-team", id = team.id) } }
        ?: if(!vm.collectionLoading && vm.persistenceError == null) text("Finish and save a mission to create your first team report.") else null
    actionRow("Open My Collection", "Your saved characters and mission reports.") { vm.destination("collection") }
}
fun ScreenRenderer.profile() {
    val user = vm.user ?: return
    val firstRun = vm.route.screen == "first-run-profile"
    title(if(firstRun) "Your agent profile" else "Agent profile")
    profileRow()
    syncStatus()
    section("Your archive")
    dataPair("Favorite dossiers", collectionCount(vm.favorites.size))
    dataPair("Saved teams", collectionCount(vm.teams.size))
    dataPair("Completed missions", collectionCount(vm.history.size))
    if(firstRun) button("Enter the Archives", primary = true) { vm.destination("home") }
    actionRow("Settings", "Account, privacy and data credits.") { vm.navigate("settings") }
    button("Sign out") { activity.signOut() }
}
fun ScreenRenderer.settings() {
    title("Settings & about")
    section("Account")
    text(vm.user?.email ?: "Email unavailable")
    section("Language")
    text("English. Comic Vine names and descriptions are displayed as supplied.")
    section("Visual style")
    text("A comic archive assembled from paper, ink and collected dossiers. Built for searching, discovering and recruiting.")
    section("Data attribution")
    text("Comic data and character imagery supplied by Comic Vine. This educational app is not affiliated with Marvel. Mission scenarios and evaluations are created by the app.")
    section("Privacy")
    text("Your account name, email and optional Google photo identify your archive. Favorites, saved teams and mission history are stored privately under your Firebase account. Recently viewed characters and searches are stored on this device. Signing out ends your local authentication session; it does not delete your saved collection.")
    button("Clear recently viewed and recent searches") { vm.clearLocalHistory() }
    button("Sign out") { activity.signOut() }
}
fun ScreenRenderer.collection() {
    title("My collection")
    text("Your private archive of characters and completed missions.")
    syncStatus()
    val fixedTab = when(vm.route.screen) { "favorites" -> "Favorites"; "saved-teams" -> "Saved Teams"; "mission-history" -> "Mission History"; "recent" -> "Recently Viewed"; else -> null }
    val currentTab = fixedTab ?: vm.collectionTab
    tabs(listOf("Favorites", "Saved Teams", "Mission History", "Recently Viewed"), currentTab) { tab -> vm.collectionTab = tab; vm.destination("collection") }
    section(currentTab)
    val count = when(currentTab) { "Favorites" -> vm.favorites.size; "Saved Teams" -> vm.teams.size; "Mission History" -> vm.history.size; else -> vm.recent.size }
    note(if(currentTab == "Recently Viewed") "$count dossiers stored on this device" else "${collectionCount(count)} records")
    when(currentTab) {
        "Favorites" -> {
            if(vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) { state("Your archive is empty", "Find a character and tap Favorite to save a dossier."); button("Search Characters", primary = true) { vm.destination("search") } }
            vm.favorites.forEach { member -> character(member); button("Remove ${member.name} from favorites", !vm.operationLoading) { vm.favorite(member) } }
        }
        "Saved Teams", "Mission History" -> {
            val teams = if(currentTab == "Saved Teams") vm.teams else vm.history
            if(teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) { state(if(currentTab == "Saved Teams") "No saved teams yet" else "No completed missions", "Complete recruitment and save a report to keep your mission here."); button("Start a Mission", primary = true) { vm.destination("recruit") } }
            teams.forEach { team ->
                teamCard(team) { vm.navigate("saved-team", id = team.id) }
                if(currentTab == "Saved Teams") button("Delete ${team.name}") { confirmDelete(team) }
            }
        }
        "Recently Viewed" -> {
            note("Stored on this device. These dossier snapshots may be out of date.")
            if(vm.recent.isEmpty()) { state("No dossiers viewed yet", "Open a character to start a local reading history."); button("Search Characters", primary = true) { vm.destination("search") } }
            vm.recent.forEach { character(it) }
        }
    }
}
private fun ScreenRenderer.collectionCount(size: Int): String = when {
    vm.collectionLoading -> if(size > 0) "$size loaded · syncing…" else "Loading…"
    vm.persistenceError != null -> if(size > 0) "$size loaded · sync interrupted" else "Unavailable until sync completes"
    else -> size.toString()
}
