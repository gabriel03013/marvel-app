package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.briefing() {
    val mission = vm.mission
    title(mission.title)
    missionProgress(1)
    heroArtwork(if (mission.id == "cosmic") "cosmic" else "heroes", height = 156)
    text(mission.description)
    dataPair("Recommended roster", "Up to ${mission.size} members")
    dataPair(
        "Suggested abilities",
        mission.focus.joinToString(" · ").ifBlank {
            "Choose complementary abilities for your custom objective."
        },
    )
    if (mission.id == "custom")
        input("Your mission briefing", vm.customBriefing, true) { vm.customBriefing = it }
    button("Begin Recruitment", primary = true) {
        if (mission.id == "custom" && vm.customBriefing.isBlank()) {
            vm.message = "Write a short briefing for your custom mission."
            vm.notifyChanged()
        } else {
            vm.selectionPurpose = "mission"
            vm.navigate("picker")
        }
    }
    note(
        "You may use fewer members; your report will reflect roster coverage. Mission scenarios are app templates. Character facts come from Comic Vine."
    )
}
