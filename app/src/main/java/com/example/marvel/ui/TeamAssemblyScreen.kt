package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.assembly() {
    title("Team assembly")
    missionProgress(2)
    text(vm.mission.title)
    if (vm.roster.isEmpty()) {
        state(
            "No team selected",
            "Recruit a character before assembling your team.",
            retry = {
                vm.selectionPurpose = "mission"
                vm.navigate("picker")
            },
        )
        return
    }
    dataPair("Roster", "${vm.roster.size} of ${vm.mission.size} members")
    input("Team name", vm.teamName) { vm.teamName = it }
    button(
        if (vm.operationLoading) "Preparing simulation…" else "Run Simulation",
        !vm.operationLoading,
        primary = true,
    ) {
        activity.hideKeyboard()
        vm.runSimulation()
    }
    vm.simulationError?.let { error ->
        state("Simulation could not start", error, retry = { vm.runSimulation() })
    }
    note(
        "The app retrieves each member’s Comic Vine dossier, then runs the same visible criteria every time for this roster and mission."
    )
    section("Your recruits")
    vm.roster.toList().forEach { member ->
        character(member)
        button("Remove from team") { vm.removeRecruit(member) }.contentDescription =
            "Remove ${member.name} from team"
    }
    button("Add or change members") {
        vm.selectionPurpose = "mission"
        vm.navigate("picker")
    }
    section("How your report is evaluated")
    dataPair("Roster coverage", "40 points")
    dataPair("Documented power diversity", "40 points")
    dataPair("Mission fit", "20 points")
    note(
        "For custom missions, the final 20 points measure power-data coverage. Missing powers reduce the available evidence, not a character’s actual strength. These are app-generated criteria, not official Marvel statistics."
    )
}
