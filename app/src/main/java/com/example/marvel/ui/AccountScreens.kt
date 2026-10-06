package com.example.marvel.ui

import android.app.AlertDialog
import com.example.marvel.data.*

fun ScreenRenderer.home() {
    val user = vm.user
    val firstName = user?.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "Agent"
    title("Welcome, $firstName.")
    profileRow { vm.navigate("profile") }
    button("Search Characters", primary = true) { vm.destination("search") }
    note("Open a character dossier, discover their story and recruit them to your team.")

    if (vm.hasRecruitmentDraft) {
        section("Your active mission")
        actionRow("Continue Recruitment", "${vm.mission.title} · ${vm.roster.size} of ${vm.mission.size} members") {
            vm.selectionPurpose = "mission"; vm.navigate(if(vm.roster.isEmpty()) "picker" else "assembly")
        }
    }
    section("Answer the call")
    missionCard(missions.first(), true)
    actionRow("Explore Archives", "Follow powers, teams, issues and story arcs.") { vm.destination("archives") }

    section("Your archive")
    syncStatus()
    if (vm.recent.isEmpty() && vm.favorites.isEmpty() && vm.teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null && !vm.collectionCached) {
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
    actionRow("Open My Collection", "Favorites, saved teams and mission history.") { vm.destination("collection") }
}
fun ScreenRenderer.profile() {
    if (vm.user == null) return
    val firstRun = vm.route.screen == "first-run-profile"
    title("Your profile")
    profileRow()
    if(vm.user?.isAnonymous == true) note("Guest session · Your saved archive is linked to this anonymous account. Signing out ends access to these saved items.")
    if(firstRun) button("Enter the Archives", primary = true) { vm.destination("home") }
    section("Your collection")
    syncStatus()
    dataPair("Favorite dossiers", collectionCount(vm.favorites.size))
    dataPair("Saved teams", collectionCount(vm.teams.size))
    dataPair("Completed missions", collectionCount(vm.history.size))
    actionRow("Settings", "Account, privacy and data credits.") { vm.navigate("settings") }
    button(if(vm.user?.isAnonymous == true) "End guest session" else "Sign out") { confirmSignOut() }
}
fun ScreenRenderer.settings() {
    title("Settings")
    section("Account")
    dataPair("Signed in as", if(vm.user?.isAnonymous == true) "Guest session" else vm.user?.email?.takeIf { it.isNotBlank() } ?: "Email unavailable")
    dataPair("App language", "English")
    note("Comic Vine names and descriptions are displayed as supplied.")

    section("Your data")
    text("Favorites, saved teams and mission history are stored privately under your Firebase account. Recently viewed dossiers and searches stay on this device.")
    note(if(vm.user?.isAnonymous == true) "Your guest collection is stored under this anonymous Firebase account. Signing out ends access to its saved collection." else "Your account name, email and optional Google photo identify your archive. Signing out ends the local authentication session and keeps your saved collection.")
    button("Clear recent dossiers and searches") { vm.clearLocalHistory() }
    button(if(vm.user?.isAnonymous == true) "End guest session" else "Sign out") { confirmSignOut() }

    section("About the Archives")
    text("A comic archive assembled from paper, ink and collected dossiers. Search, discover and recruit.")
    section("Data credits")
    text("Comic data and character imagery supplied by Comic Vine. This educational app is not affiliated with Marvel. Mission scenarios and evaluations are created by the app.")
}
private fun ScreenRenderer.confirmSignOut() {
    if(vm.user?.isAnonymous != true) { activity.signOut(); return }
    AlertDialog.Builder(activity)
        .setTitle("End guest session?")
        .setMessage("Your saved favorites, teams and mission history are tied to this guest account. Signing out ends access to them.")
        .setNegativeButton("Keep browsing", null)
        .setPositiveButton("End session") { _, _ -> activity.signOut() }
        .show()
}
fun ScreenRenderer.collection() {
    title("My collection")
    val fixedTab = when(vm.route.screen) { "favorites" -> "Favorites"; "saved-teams" -> "Saved Teams"; "mission-history" -> "Mission History"; "recent" -> "Recently Viewed"; else -> null }
    val currentTab = fixedTab ?: vm.collectionTab
    tabs(listOf("Favorites", "Saved Teams", "Mission History", "Recently Viewed"), currentTab) { tab -> vm.collectionTab = tab; vm.destination("collection") }
    val count = when(currentTab) { "Favorites" -> vm.favorites.size; "Saved Teams" -> vm.teams.size; "Mission History" -> vm.history.size; else -> vm.recent.size }
    val recordLabel = when(currentTab) { "Favorites" -> "Favorite dossiers"; "Saved Teams" -> "Saved teams"; "Mission History" -> "Completed missions"; else -> "Dossiers on this device" }
    dataPair(recordLabel, if(currentTab == "Recently Viewed") count.toString() else collectionCount(count))
    if(currentTab != "Recently Viewed") syncStatus()
    when(currentTab) {
        "Favorites" -> {
            if(vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
                state(if(vm.collectionCached) "No favorites cached" else "Your archive is empty", if(vm.collectionCached) "Your latest favorites will appear when Firebase syncs. Search the live archive in the meantime." else "Find a character and tap Favorite to save their dossier.")
                button("Search Characters", primary = true) { vm.destination("search") }
            }
            vm.favorites.forEach { member ->
                character(member)
                button("Remove favorite", !vm.operationLoading) { vm.favorite(member) }.contentDescription = "Remove ${member.name} from favorites"
            }
        }
        "Saved Teams", "Mission History" -> {
            val teams = if(currentTab == "Saved Teams") vm.teams else vm.history
            if(teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
                state(if(vm.collectionCached) "No reports cached" else if(currentTab == "Saved Teams") "No saved teams yet" else "No completed missions", if(vm.collectionCached) "Your current reports will appear when Firebase syncs. You can begin a new mission now." else "Recruit a team and save its report to keep the mission here.")
                button("Start a Mission", primary = true) { vm.destination("recruit") }
            }
            teams.forEach { team ->
                teamCard(team) { vm.navigate("saved-team", id = team.id) }
                if(currentTab == "Saved Teams") button("Delete saved team") { confirmDelete(team) }.contentDescription = "Delete saved team ${team.name}"
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
    vm.collectionPending -> "$size on this device · waiting to sync"
    vm.collectionCached -> "$size cached · may be out of date"
    else -> size.toString()
}
