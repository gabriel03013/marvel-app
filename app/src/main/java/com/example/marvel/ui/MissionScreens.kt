package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.recruit() {
    title("Build a team.\nAnswer the call.")
    text("Choose a briefing, recruit from real character dossiers and create your mission report.")
    if(vm.roster.isNotEmpty()) {
        section("Active team · ${vm.roster.size}/${vm.mission.size}")
        text(vm.mission.title)
        vm.roster.forEach { character(it) }
        button("Continue Recruitment") { vm.selectionPurpose = "mission"; vm.navigate("picker") }
    }
    section("Mission briefings")
    missions.forEach { missionCard(it) }
    section("Saved teams")
    syncStatus()
    if(vm.teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) text("Your saved teams will appear here after your first mission.")
    vm.teams.take(3).forEach { team -> button("${team.name} · ${missions.firstOrNull { it.id == team.missionId }?.title ?: "Custom Mission"}") { vm.navigate("saved-team", id = team.id) } }
}
fun ScreenRenderer.briefing() {
    val mission = vm.mission
    title(mission.title)
    missionProgress(1)
    section("Mission briefing")
    text(mission.description)
    dataPair("Recommended roster", "Up to ${mission.size} members")
    note("You may generate a report with fewer members; roster coverage will reflect that.")
    section("Suggested abilities")
    dataPair("Mission fit", mission.focus.joinToString(" · ").ifBlank { "Choose complementary abilities for your custom objective." })
    if(mission.id == "custom") input("Your mission briefing", vm.customBriefing, true) { vm.customBriefing = it }
    note("Mission scenarios are app templates. Character facts come from Comic Vine.")
    button("Begin Recruitment", primary = true) {
        if(mission.id == "custom" && vm.customBriefing.isBlank()) { vm.message = "Write a short briefing for your custom mission."; vm.notifyChanged() }
        else { vm.selectionPurpose = "mission"; vm.navigate("picker") }
    }
}
fun ScreenRenderer.picker() {
    val selecting = vm.selectionPurpose != "mission"
    title(when(vm.selectionPurpose) { "compareA" -> "Select Character A"; "compareB" -> "Select Character B"; "timeline" -> "Select timeline character"; else -> "Recruit your team" })
    if(!selecting) {
        missionProgress(2)
        dataPair(vm.mission.title, "${vm.roster.size} of ${vm.mission.size} members selected")
        text("Suggested abilities: ${vm.mission.focus.joinToString().ifBlank { "Defined by your custom briefing" }}")
        section("Selected members")
        if(vm.roster.isEmpty()) state("No recruits yet", "Search the archive and tap a character to select them.")
        vm.roster.toList().forEach { member -> character(member, true) { vm.select(member) } }
        button("Continue to Assembly", vm.roster.isNotEmpty(), primary = true) { vm.navigate("assembly") }
    } else text("Tap a character to confirm your selection.")
    section("Find characters")
    val context = "picker_${vm.selectionPurpose}"
    searchField(context, "Search characters…") { query -> vm.submittedQueries[context] = query; vm.loadPage("character", query, refresh = true) }
    section("From your collection")
    syncStatus()
    if(vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) text("No favorites yet. Browse the live archive below.")
    vm.favorites.take(6).forEach { character(it, isCandidateSelected(it)) { vm.select(it) } }
    section("Comic Vine candidates")
    val query = vm.submittedQueries[context].orEmpty()
    if(vm.pages[vm.pageKey("character", query)] == null) vm.loadPage("character", query)
    results("character", query, selection = true)
}
fun ScreenRenderer.assembly() {
    title("Team assembly")
    missionProgress(2)
    text(vm.mission.title)
    if(vm.roster.isEmpty()) { state("No team selected", "Recruit a character before assembling your team.", retry = { vm.selectionPurpose = "mission"; vm.navigate("picker") }); return }
    input("Team name", vm.teamName) { vm.teamName = it }
    section("Your recruits")
    vm.roster.toList().forEach { member -> character(member); button("Remove ${member.name}") { vm.removeRecruit(member) } }
    button("Add or change members") { vm.selectionPurpose = "mission"; vm.navigate("picker") }
    section("How your report is evaluated")
    dataPair("Roster coverage", "40 points")
    dataPair("Documented power diversity", "40 points")
    dataPair("Mission fit", "20 points")
    note("For custom missions, the final 20 points measure power-data coverage. Missing powers reduce the available evidence, not a character’s actual strength. These are app-generated criteria, not official Marvel statistics.")
    button(if(vm.operationLoading) "Retrieving full dossiers…" else "Generate Report", !vm.operationLoading, primary = true) { activity.hideKeyboard(); vm.generateReport() }
}
fun ScreenRenderer.report() {
    val saved = vm.route.screen == "saved-team"
    val team = if(saved) (vm.teams + vm.history).firstOrNull { it.id == vm.route.id } else vm.report
    title(if(saved) "Saved mission report" else "Mission report")
    if(team == null) {
        if(saved) syncStatus()
        if(!saved || (!vm.collectionLoading && vm.persistenceError == null)) {
            state("Report unavailable", "This report is no longer in your saved collection.")
            actionRow("View saved teams") { vm.collectionTab = "Saved Teams"; vm.destination("collection") }
        }
        return
    }
    val mission = missions.firstOrNull { it.id == team.missionId } ?: missions.last()
    missionProgress(3)
    section(team.name)
    text(mission.title)
    text(team.briefing.ifBlank { mission.description })
    note("Created ${date(team.createdAt)}")
    val evaluation = evaluate(team.members, mission)
    scorePanel(evaluation.score, "${evaluation.knownMembers}/${team.members.size} members with documented powers · ${evaluation.uniquePowers.size} distinct abilities")
    note("Based on roster coverage (40), documented power diversity (40) and briefing fit (20). Custom missions use documented power coverage for fit. This is not an official Marvel statistic or a combat prediction.")
    dataPair("Matched suggested abilities", evaluation.matchedFocus.joinToString().ifBlank { if(mission.focus.isEmpty()) "Custom objective — no preset abilities" else "None documented" })
    section("Team dossiers")
    team.members.forEach { member ->
        character(member)
        text("${member.name} · Powers: ${member.related("powers").joinToString { it.name }.ifBlank { "Not documented" }}")
        dataPair("Suggested role · app inference", suggestedRole(member))
        text("Documented teams: ${member.related("teams").joinToString { it.name }.ifBlank { "Not documented" }}")
    }
    section("Evidence and limits")
    note("Facts are taken from the Comic Vine dossiers retrieved when this report was generated. Saved reports are snapshots; reopen a character to refresh their record. Suggested roles are app inferences from named powers, not official character roles or strength ratings.")
    syncStatus()
    if(saved) button("Delete saved team") { confirmDelete(team) }
    else button(if(vm.teams.any { it.id == team.id }) "Team saved · Save again" else if(vm.operationLoading) "Saving team…" else "Save Team", !vm.operationLoading, primary = true) { vm.saveReport() }
    actionRow("Share Report", "Send this team and its evidence as text.") { activity.share(team) }
    actionRow("Start Another Mission") { vm.destination("recruit") }
}
