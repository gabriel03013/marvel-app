package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.report() {
    val saved = vm.route.screen == "saved-team"
    val team =
        if (saved) (vm.teams + vm.history).firstOrNull { it.id == vm.route.id } else vm.report
    if (team == null) {
        title(if (saved) "Saved mission report" else "Mission report")
        if (saved) syncStatus()
        if (!saved || (!vm.collectionLoading && vm.persistenceError == null)) {
            if (saved && vm.collectionCached)
                state(
                    "Report not cached",
                    "This report is not stored on this device. Connect and sync your collection to retrieve it.",
                    retry = { vm.retryCollections() },
                )
            else state("Report unavailable", "This report is no longer in your saved collection.")
            actionRow("View saved teams") {
                vm.collectionTab = "Saved Teams"
                vm.destination("collection")
            }
        }
        return
    }
    val mission = missions.firstOrNull { it.id == team.missionId } ?: missions.last()
    title(team.name)
    missionProgress(3)
    dataPair("Mission", mission.title)
    note("${team.members.size} members · Created ${date(team.createdAt)}")
    if (saved) button("Share Report", primary = true) { activity.share(team) }
    else {
        button(
            if (vm.operationLoading) "Saving team…"
            else if (vm.teams.any { it.id == team.id }) "Team saved · Save again" else "Save Team",
            !vm.operationLoading,
            primary = true,
        ) {
            vm.saveReport()
        }
        actionRow("Share Report", "Share this team and its documented evidence as text.") {
            activity.share(team)
        }
    }
    syncStatus()
    section("Mission report")
    text(team.briefing.ifBlank { mission.description })
    val evaluation = evaluate(team.members, mission)
    section("App-generated evaluation")
    scorePanel(
        evaluation.score,
        "${evaluation.knownMembers}/${team.members.size} members with documented powers · ${evaluation.uniquePowers.size} distinct abilities",
    )
    dataPair("Roster coverage", "${team.members.size} of ${mission.size} recommended members")
    dataPair(
        "Matched suggested abilities",
        evaluation.matchedFocus.joinToString().ifBlank {
            if (mission.focus.isEmpty()) "Custom objective — no preset abilities"
            else "None documented"
        },
    )
    val simulation = simulateMission(team.members, mission)
    dataPair(
        "Uncovered suggested abilities",
        simulation.evaluation.let { result ->
            mission.focus
                .filterNot { focus -> result.matchedFocus.any { it.equals(focus, true) } }
                .joinToString()
                .ifBlank {
                    if (mission.focus.isEmpty()) "No preset abilities"
                    else "All suggested abilities covered"
                }
        },
    )
    section("Simulation outcome")
    simulation.steps.forEach { (label, evidence) -> dataPair(label, evidence) }
    note(
        "The 100-point evaluation uses roster coverage (40), documented power diversity (40) and briefing fit (20). Custom missions use documented power coverage for fit. It is an app evaluation, not an official Marvel statistic or a combat prediction."
    )
    section("Team dossiers")
    team.members.forEach { member ->
        actionRow(member.name, "Open the dossier used as evidence") { open(member) }
        character(member)
        dataPair(
            "Documented powers",
            member.related("powers").joinToString { it.name }.ifBlank { "Not documented" },
        )
        dataPair("Suggested role · app inference", suggestedRole(member))
        dataPair(
            "Documented teams",
            member.related("teams").joinToString { it.name }.ifBlank { "Not documented" },
        )
        dataPair(
            "First appearance",
            member.reference("first_appeared_in_issue")?.name ?: "Not documented",
        )
        dataPair(
            "Issue appearances",
            member
                .text("count_of_issue_appearances")
                .ifBlank { member.text("count_of_isssue_appearances") }
                .ifBlank { "Not documented" },
        )
    }
    section("Evidence and limits")
    note(
        "Facts are taken from the Comic Vine dossiers retrieved when this report was generated. Saved reports are snapshots; reopen a character to refresh their record. Suggested roles are app inferences from named powers, not official character roles or strength ratings."
    )
    if (saved) button("Delete saved team") { confirmDelete(team) }
    actionRow("Start Another Mission") { vm.destination("recruit") }
    val revealKey = "${vm.route.screen}:${team.id}"
    if (vm.revealedReportId != revealKey && android.animation.ValueAnimator.areAnimatorsEnabled()) {
        vm.revealedReportId = revealKey
        content.alpha = 0f
        content
            .animate()
            .alpha(1f)
            .setDuration(420)
            .setStartDelay(80)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    } else {
        vm.revealedReportId = revealKey
        content.alpha = 1f
    }
}
