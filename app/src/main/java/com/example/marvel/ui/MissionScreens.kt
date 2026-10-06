package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.recruit() {
    title("Recruit your team")
    text("Choose a mission. Find your recruits. Bring their evidence together.")
    if(vm.hasRecruitmentDraft) {
        section("Your active mission")
        dataPair(vm.mission.title, "${vm.roster.size} of ${vm.mission.size} members selected")
        if(vm.teamName.isNotBlank()) text(vm.teamName)
        button("Continue Recruitment", primary = true) {
            vm.selectionPurpose = "mission"; vm.navigate(if(vm.roster.isEmpty()) "picker" else "assembly")
        }
        if(vm.mission.id == "custom" && vm.customBriefing.isNotBlank()) note("Objective: ${vm.customBriefing}")
        vm.roster.take(2).forEach { character(it) }
        if(vm.roster.size > 2) note("${vm.roster.size - 2} more members in your active team")
    }
    section("Choose a briefing")
    missionCard(missions.first(), featured = true)
    missions.drop(1).forEach { mission ->
        actionRow(mission.title, if(mission.id == "custom") "Write your own objective · Up to ${mission.size} members" else "${mission.focus.joinToString(" · ")} · Up to ${mission.size} members") { chooseMission(mission) }
    }
    section("Saved teams")
    syncStatus()
    if(vm.teams.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        text(if(vm.collectionCached) "No team reports are cached on this device. Firebase will retrieve your collection when it syncs." else "Save a completed report to keep your team here.")
    }
    vm.teams.take(3).forEach { team -> teamCard(team) { vm.navigate("saved-team", id = team.id) } }
}
fun ScreenRenderer.briefing() {
    val mission = vm.mission
    title(mission.title)
    missionProgress(1)
    heroArtwork(if(mission.id == "cosmic") "cosmic" else "heroes", height = 156)
    text(mission.description)
    dataPair("Recommended roster", "Up to ${mission.size} members")
    dataPair("Suggested abilities", mission.focus.joinToString(" · ").ifBlank { "Choose complementary abilities for your custom objective." })
    if(mission.id == "custom") input("Your mission briefing", vm.customBriefing, true) { vm.customBriefing = it }
    button("Begin Recruitment", primary = true) {
        if(mission.id == "custom" && vm.customBriefing.isBlank()) { vm.message = "Write a short briefing for your custom mission."; vm.notifyChanged() }
        else { vm.selectionPurpose = "mission"; vm.navigate("picker") }
    }
    note("You may use fewer members; your report will reflect roster coverage. Mission scenarios are app templates. Character facts come from Comic Vine.")
}
fun ScreenRenderer.picker() {
    val selecting = vm.selectionPurpose != "mission"
    title(when(vm.selectionPurpose) {
        "compareA" -> "Select Character A"
        "compareB" -> "Select Character B"
        "investigateA" -> "Select Operative Alpha"
        "investigateB" -> "Select Operative Beta"
        "timeline" -> "Select timeline character"
        else -> "Recruit your team"
    })
    if(!selecting) {
        missionProgress(2)
        dataPair(vm.mission.title, "${vm.roster.size} of ${vm.mission.size} members selected")
        note("Suggested abilities: ${vm.mission.focus.joinToString().ifBlank { "Defined by your custom briefing" }}")
        button("Continue to Assembly", vm.roster.isNotEmpty(), primary = true) { vm.navigate("assembly") }
        button(if(vm.teamSuggestion.loading) "Analyzing documented powers…" else "Suggest a team", !vm.teamSuggestion.loading) { vm.suggestMissionTeam() }
        vm.teamSuggestion.error?.let { note(it) }
        vm.teamSuggestion.value?.let { suggestion ->
            section("Suggested team · app generated")
            if (suggestion.members.isEmpty()) {
                text("No roster is selected. Tap a loaded candidate to add them before accepting, or continue manually.")
            } else {
                text("${suggestion.members.size} of ${vm.mission.size} suggested roster spots selected from ${suggestion.candidatesWithPowerData} candidates with documented powers.")
                suggestion.members.forEach { member ->
                    actionRow(member.name, suggestion.reasons[member.id].orEmpty().joinToString(" · ") + " · Selected for suggestion") { vm.reviseSuggestedMember(member) }
                }
            }
            suggestion.eligibleCandidates.filterNot { candidate -> suggestion.members.any { it.id == candidate.id } }.forEach { candidate ->
                val matches = candidate.related("powers").map { it.name }.filter { power -> vm.mission.focus.any { it.equals(power, true) } }
                actionRow(candidate.name, if (matches.isEmpty()) "No exact suggested-ability match documented" else "Documented match: ${matches.joinToString()}") { vm.reviseSuggestedMember(candidate) }
            }
            dataPair("Abilities covered", suggestion.coveredFocus.joinToString().ifBlank { "None documented" })
            dataPair("Still uncovered", suggestion.uncoveredFocus.joinToString().ifBlank { "All suggested abilities covered" })
            button("Use suggested roster", suggestion.members.isNotEmpty(), primary = true) { vm.acceptSuggestion() }
            note("Tap selected records to remove them or other loaded candidates to add them before accepting. This is a deterministic app suggestion based on loaded Comic Vine powers.")
        }
    } else text("Tap a character to confirm your selection.")
    section("Find characters")
    val context = "picker_${vm.selectionPurpose}"
    searchField(context, "Search characters…") { query -> vm.submittedQueries[context] = query; vm.loadPage("character", query, refresh = true) }
    val query = vm.submittedQueries[context].orEmpty()

    if(!selecting) {
        if(vm.roster.isNotEmpty()) {
            section("Selected team")
            vm.roster.toList().forEach { member ->
                actionRow(member.name, "Selected · Remove from team") { vm.select(member) }
            }
        } else note("Your team is empty. Tap a character below to recruit them.")
    }
    if(query.isBlank() && vm.favorites.isNotEmpty()) {
        section("From your favorites")
        syncStatus()
        vm.favorites.take(3).forEach { character(it, isCandidateSelected(it)) { vm.select(it) } }
    } else if(query.isBlank() && (vm.collectionLoading || vm.persistenceError != null || vm.collectionCached || vm.collectionPending)) {
        syncStatus()
    }
    section("Comic Vine candidates")
    if(vm.pages[vm.pageKey("character", query)] == null) vm.loadPage("character", query)
    results("character", query, selection = true)
}
fun ScreenRenderer.assembly() {
    title("Team assembly")
    missionProgress(2)
    text(vm.mission.title)
    if(vm.roster.isEmpty()) { state("No team selected", "Recruit a character before assembling your team.", retry = { vm.selectionPurpose = "mission"; vm.navigate("picker") }); return }
    dataPair("Roster", "${vm.roster.size} of ${vm.mission.size} members")
    input("Team name", vm.teamName) { vm.teamName = it }
    button(if(vm.operationLoading) "Preparing simulation…" else "Run Simulation", !vm.operationLoading, primary = true) { activity.hideKeyboard(); vm.runSimulation() }
    vm.simulationError?.let { error -> state("Simulation could not start", error, retry = { vm.runSimulation() }) }
    note("The app retrieves each member’s Comic Vine dossier, then runs the same visible criteria every time for this roster and mission.")
    section("Your recruits")
    vm.roster.toList().forEach { member ->
        character(member)
        button("Remove from team") { vm.removeRecruit(member) }.contentDescription = "Remove ${member.name} from team"
    }
    button("Add or change members") { vm.selectionPurpose = "mission"; vm.navigate("picker") }
    section("How your report is evaluated")
    dataPair("Roster coverage", "40 points")
    dataPair("Documented power diversity", "40 points")
    dataPair("Mission fit", "20 points")
    note("For custom missions, the final 20 points measure power-data coverage. Missing powers reduce the available evidence, not a character’s actual strength. These are app-generated criteria, not official Marvel statistics.")
}
fun ScreenRenderer.simulation() {
    val source = vm.simulationSource
    title("Tactical simulation")
    if (source == null) {
        state("Simulation unavailable", "Return to team assembly and run the simulation again.", retry = { vm.back() })
        return
    }
    val mission = missions.firstOrNull { it.id == source.missionId } ?: missions.last()
    val simulation = simulateMission(source.members, mission)
    section("App-generated tactical simulation")
    dataPair("Mission", mission.title)
    text(source.briefing.ifBlank { mission.description })
    dataPair("Participants", source.members.joinToString { it.name })
    scorePanel(simulation.evaluation.score, "Evidence score · ${simulation.evaluation.knownMembers}/${source.members.size} dossiers include documented powers")
    simulation.steps.forEachIndexed { index, step ->
        section("${index + 1}. ${step.first}")
        text(step.second)
    }
    section("Evidence used")
    source.members.forEach { member ->
        actionRow(member.name, "Open Comic Vine dossier") { open(member) }
        dataPair("Documented powers", member.related("powers").joinToString { it.name }.ifBlank { "Not documented" })
    }
    note("This is a reproducible app-generated tactical simulation based on the roster, mission template and documented powers. It is not canonical, predictive or an official Marvel statistic. Missing dossier data is unknown, not evidence that a character lacks an ability.")
    button("Generate Mission Report", primary = true) { vm.createReportFromSimulation() }
    actionRow("Edit team") { vm.back() }
}
fun ScreenRenderer.report() {
    val saved = vm.route.screen == "saved-team"
    val team = if(saved) (vm.teams + vm.history).firstOrNull { it.id == vm.route.id } else vm.report
    if(team == null) {
        title(if(saved) "Saved mission report" else "Mission report")
        if(saved) syncStatus()
        if(!saved || (!vm.collectionLoading && vm.persistenceError == null)) {
            if(saved && vm.collectionCached) state("Report not cached", "This report is not stored on this device. Connect and sync your collection to retrieve it.", retry = { vm.retryCollections() })
            else state("Report unavailable", "This report is no longer in your saved collection.")
            actionRow("View saved teams") { vm.collectionTab = "Saved Teams"; vm.destination("collection") }
        }
        return
    }
    val mission = missions.firstOrNull { it.id == team.missionId } ?: missions.last()
    title(team.name)
    missionProgress(3)
    dataPair("Mission", mission.title)
    note("${team.members.size} members · Created ${date(team.createdAt)}")
    if(saved) button("Share Report", primary = true) { activity.share(team) }
    else {
        button(if(vm.operationLoading) "Saving team…" else if(vm.teams.any { it.id == team.id }) "Team saved · Save again" else "Save Team", !vm.operationLoading, primary = true) { vm.saveReport() }
        actionRow("Share Report", "Share this team and its documented evidence as text.") { activity.share(team) }
    }
    syncStatus()
    section("Mission report")
    text(team.briefing.ifBlank { mission.description })
    val evaluation = evaluate(team.members, mission)
    section("App-generated evaluation")
    scorePanel(evaluation.score, "${evaluation.knownMembers}/${team.members.size} members with documented powers · ${evaluation.uniquePowers.size} distinct abilities")
    dataPair("Roster coverage", "${team.members.size} of ${mission.size} recommended members")
    dataPair("Matched suggested abilities", evaluation.matchedFocus.joinToString().ifBlank { if(mission.focus.isEmpty()) "Custom objective — no preset abilities" else "None documented" })
    val simulation = simulateMission(team.members, mission)
    dataPair("Uncovered suggested abilities", simulation.evaluation.let { result -> mission.focus.filterNot { focus -> result.matchedFocus.any { it.equals(focus, true) } }.joinToString().ifBlank { if (mission.focus.isEmpty()) "No preset abilities" else "All suggested abilities covered" } })
    section("Simulation outcome")
    simulation.steps.forEach { (label, evidence) -> dataPair(label, evidence) }
    note("The 100-point evaluation uses roster coverage (40), documented power diversity (40) and briefing fit (20). Custom missions use documented power coverage for fit. It is an app evaluation, not an official Marvel statistic or a combat prediction.")
    section("Team dossiers")
    team.members.forEach { member ->
        actionRow(member.name, "Open the dossier used as evidence") { open(member) }
        character(member)
        dataPair("Documented powers", member.related("powers").joinToString { it.name }.ifBlank { "Not documented" })
        dataPair("Suggested role · app inference", suggestedRole(member))
        dataPair("Documented teams", member.related("teams").joinToString { it.name }.ifBlank { "Not documented" })
        dataPair("First appearance", member.reference("first_appeared_in_issue")?.name ?: "Not documented")
        dataPair("Issue appearances", member.text("count_of_issue_appearances").ifBlank { member.text("count_of_isssue_appearances") }.ifBlank { "Not documented" })
    }
    section("Evidence and limits")
    note("Facts are taken from the Comic Vine dossiers retrieved when this report was generated. Saved reports are snapshots; reopen a character to refresh their record. Suggested roles are app inferences from named powers, not official character roles or strength ratings.")
    if(saved) button("Delete saved team") { confirmDelete(team) }
    actionRow("Start Another Mission") { vm.destination("recruit") }
    val revealKey = "${vm.route.screen}:${team.id}"
    if(vm.revealedReportId != revealKey && android.animation.ValueAnimator.areAnimatorsEnabled()) {
        vm.revealedReportId = revealKey
        content.alpha = 0f
        content.animate().alpha(1f).setDuration(420).setStartDelay(80).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    } else { vm.revealedReportId = revealKey; content.alpha = 1f }
}
