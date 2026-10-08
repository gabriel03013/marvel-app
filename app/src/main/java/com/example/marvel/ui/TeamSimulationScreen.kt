package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.simulation() {
    val source = vm.simulationSource
    title("Tactical simulation")
    if (source == null) {
        state(
            "Simulation unavailable",
            "Return to team assembly and run the simulation again.",
            retry = { vm.back() },
        )
        return
    }
    val mission = missions.firstOrNull { it.id == source.missionId } ?: missions.last()
    val simulation = simulateMission(source.members, mission)
    section("App-generated tactical simulation")
    dataPair("Mission", mission.title)
    text(source.briefing.ifBlank { mission.description })
    dataPair("Participants", source.members.joinToString { it.name })
    scorePanel(
        simulation.evaluation.score,
        "Evidence score · ${simulation.evaluation.knownMembers}/${source.members.size} dossiers include documented powers",
    )
    simulation.steps.forEachIndexed { index, step ->
        section("${index + 1}. ${step.first}")
        text(step.second)
    }
    section("Evidence used")
    source.members.forEach { member ->
        actionRow(member.name, "Open Comic Vine dossier") { open(member) }
        dataPair(
            "Documented powers",
            member.related("powers").joinToString { it.name }.ifBlank { "Not documented" },
        )
    }
    note(
        "This is a reproducible app-generated tactical simulation based on the roster, mission template and documented powers. It is not canonical, predictive or an official Marvel statistic. Missing dossier data is unknown, not evidence that a character lacks an ability."
    )
    button("Generate Mission Report", primary = true) { vm.createReportFromSimulation() }
    actionRow("Edit team") { vm.back() }
}
