package com.example.marvel.ui

import com.example.marvel.data.*

fun ScreenRenderer.picker() {
    val selecting = vm.selectionPurpose != "mission"
    title(
        when (vm.selectionPurpose) {
            "compareA" -> "Select Character A"
            "compareB" -> "Select Character B"
            "investigateA" -> "Select Operative Alpha"
            "investigateB" -> "Select Operative Beta"
            "timeline" -> "Select timeline character"
            else -> "Recruit your team"
        }
    )
    if (!selecting) {
        missionProgress(2)
        dataPair(vm.mission.title, "${vm.roster.size} of ${vm.mission.size} members selected")
        note(
            "Suggested abilities: ${vm.mission.focus.joinToString().ifBlank { "Defined by your custom briefing" }}"
        )
        button("Continue to Assembly", vm.roster.isNotEmpty(), primary = true) {
            vm.navigate("assembly")
        }
        button(
            if (vm.teamSuggestion.loading) "Analyzing documented powers…" else "Suggest a team",
            !vm.teamSuggestion.loading,
        ) {
            vm.suggestMissionTeam()
        }
        vm.teamSuggestion.error?.let { note(it) }
        vm.teamSuggestion.value?.let { suggestion ->
            section("Suggested team · app generated")
            if (suggestion.members.isEmpty()) {
                text(
                    "No roster is selected. Tap a loaded candidate to add them before accepting, or continue manually."
                )
            } else {
                text(
                    "${suggestion.members.size} of ${vm.mission.size} suggested roster spots selected from ${suggestion.candidatesWithPowerData} candidates with documented powers."
                )
                suggestion.members.forEach { member ->
                    actionRow(
                        member.name,
                        suggestion.reasons[member.id].orEmpty().joinToString(" · ") +
                            " · Selected for suggestion",
                    ) {
                        vm.reviseSuggestedMember(member)
                    }
                }
            }
            suggestion.eligibleCandidates
                .filterNot { candidate -> suggestion.members.any { it.id == candidate.id } }
                .forEach { candidate ->
                    val matches =
                        candidate
                            .related("powers")
                            .map { it.name }
                            .filter { power -> vm.mission.focus.any { it.equals(power, true) } }
                    actionRow(
                        candidate.name,
                        if (matches.isEmpty()) "No exact suggested-ability match documented"
                        else "Documented match: ${matches.joinToString()}",
                    ) {
                        vm.reviseSuggestedMember(candidate)
                    }
                }
            dataPair(
                "Abilities covered",
                suggestion.coveredFocus.joinToString().ifBlank { "None documented" },
            )
            dataPair(
                "Still uncovered",
                suggestion.uncoveredFocus.joinToString().ifBlank {
                    "All suggested abilities covered"
                },
            )
            button("Use suggested roster", suggestion.members.isNotEmpty(), primary = true) {
                vm.acceptSuggestion()
            }
            note(
                "Tap selected records to remove them or other loaded candidates to add them before accepting. This is a deterministic app suggestion based on loaded Comic Vine powers."
            )
        }
    } else text("Tap a character to confirm your selection.")
    section("Find characters")
    val context = "picker_${vm.selectionPurpose}"
    searchField(context, "Search characters…") { query ->
        vm.submittedQueries[context] = query
        vm.loadPage("character", query, refresh = true)
    }
    val query = vm.submittedQueries[context].orEmpty()

    if (!selecting) {
        if (vm.roster.isNotEmpty()) {
            section("Selected team")
            vm.roster.toList().forEach { member ->
                actionRow(member.name, "Selected · Remove from team") { vm.select(member) }
            }
        } else note("Your team is empty. Tap a character below to recruit them.")
    }
    if (query.isBlank() && vm.favorites.isNotEmpty()) {
        section("From your favorites")
        syncStatus()
        vm.favorites.take(3).forEach { character(it, isCandidateSelected(it)) { vm.select(it) } }
    } else if (
        query.isBlank() &&
            (vm.collectionLoading ||
                vm.persistenceError != null ||
                vm.collectionCached ||
                vm.collectionPending)
    ) {
        syncStatus()
    }
    section("Comic Vine candidates")
    if (vm.pages[vm.pageKey("character", query)] == null) vm.loadPage("character", query)
    results("character", query, selection = true)
}
