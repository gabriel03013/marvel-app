package com.example.marvel.data

data class TeamSuggestion(
    val members: List<ArchiveItem>,
    val reasons: Map<Int, List<String>>,
    val coveredFocus: List<String>,
    val uncoveredFocus: List<String>,
    val candidatesWithPowerData: Int,
    val eligibleCandidates: List<ArchiveItem> = members,
)

fun suggestTeam(candidates: List<ArchiveItem>, mission: Mission): TeamSuggestion {
    val usable =
        candidates
            .asSequence()
            .filter { it.id > 0 && it.kind == "character" }
            .distinctBy { it.id }
            .map { item ->
                item to
                    item
                        .related("powers")
                        .filter { it.id > 0 }
                        .map { it.name }
                        .filter(String::isNotBlank)
                        .distinctBy { it.lowercase() }
                        .sortedBy { it.lowercase() }
            }
            .toList()
    val remaining = mission.focus.distinctBy { it.lowercase() }.toMutableList()
    val chosen = mutableListOf<ArchiveItem>()
    val reasons = linkedMapOf<Int, List<String>>()
    while (chosen.size < mission.size && usable.isNotEmpty()) {
        val best =
            usable
                .asSequence()
                .filter { (item, powers) ->
                    chosen.none { it.id == item.id } &&
                        (mission.focus.isEmpty() || powers.isNotEmpty())
                }
                .map { (item, powers) ->
                    Triple(
                        item,
                        powers,
                        remaining.filter { focus -> powers.any { it.equals(focus, true) } },
                    )
                }
                .filter { mission.focus.isEmpty() || it.third.isNotEmpty() }
                .sortedWith(
                    compareByDescending<Triple<ArchiveItem, List<String>, List<String>>> {
                            it.third.size
                        }
                        .thenBy { it.first.name.lowercase() }
                        .thenBy { it.first.id }
                )
                .firstOrNull() ?: break
        chosen += best.first
        reasons[best.first.id] =
            if (mission.focus.isEmpty())
                listOf(
                    "Has ${best.second.size} documented ${if (best.second.size == 1) "power" else "powers"}"
                )
            else
                best.third.map { focus ->
                    "Matches suggested ability: ${best.second.first { it.equals(focus, true) }}"
                }
        remaining.removeAll(best.third.toSet())
    }
    val covered = mission.focus.filter { focus -> remaining.none { it.equals(focus, true) } }
    return TeamSuggestion(
        chosen,
        reasons,
        covered,
        remaining.toList(),
        usable.count { it.second.isNotEmpty() },
        usable.map { it.first },
    )
}

fun reviseSuggestion(
    suggestion: TeamSuggestion,
    selectedIds: Set<Int>,
    mission: Mission,
): TeamSuggestion {
    val members = suggestion.eligibleCandidates.filter { it.id in selectedIds }.take(mission.size)
    val powers = members.associate { member ->
        member.id to member.related("powers").map { it.name }.filter(String::isNotBlank)
    }
    val reasons = members.associate { member ->
        val documented = powers[member.id].orEmpty()
        member.id to
            if (mission.focus.isEmpty())
                listOf(
                    "Has ${documented.size} documented ${if (documented.size == 1) "power" else "powers"}"
                )
            else
                mission.focus
                    .filter { focus -> documented.any { it.equals(focus, true) } }
                    .map { focus ->
                        "Matches suggested ability: ${documented.first { it.equals(focus, true) }}"
                    }
    }
    val covered =
        mission.focus.filter { focus -> powers.values.flatten().any { it.equals(focus, true) } }
    return suggestion.copy(
        members = members,
        reasons = reasons,
        coveredFocus = covered,
        uncoveredFocus =
            mission.focus.filterNot { focus -> covered.any { it.equals(focus, true) } },
    )
}

data class TacticalSimulation(
    val evaluation: Evaluation,
    val steps: List<Pair<String, String>>,
    val unknownMembers: List<String>,
)

fun simulateMission(members: List<ArchiveItem>, mission: Mission): TacticalSimulation {
    val evaluation = evaluate(members, mission)
    val unknown =
        members
            .filter {
                it.related("powers").none { power -> power.id > 0 && power.name.isNotBlank() }
            }
            .map { it.name }
    val coverage =
        if (mission.focus.isEmpty()) "Custom briefing has no preset abilities to compare."
        else
            "Documented coverage: ${evaluation.matchedFocus.joinToString().ifBlank { "none" }}. Uncovered: ${mission.focus.filterNot { focus -> evaluation.matchedFocus.any { it.equals(focus, true) } }.joinToString().ifBlank { "none" }}."
    val diversity =
        if (evaluation.uniquePowers.isEmpty())
            "No documented powers were available for this roster."
        else
            "${evaluation.uniquePowers.size} distinct documented ${if (evaluation.uniquePowers.size == 1) "power" else "powers"}: ${evaluation.uniquePowers.joinToString()}"
    val known =
        "${members.size - unknown.size} of ${members.size} dossiers include documented powers.${if (unknown.isEmpty()) "" else " Unknown power data: ${unknown.joinToString()}"}"
    return TacticalSimulation(
        evaluation,
        listOf(
            "Objective review" to mission.title,
            "Ability coverage" to coverage,
            "Documented power diversity" to diversity,
            "Evidence gaps" to known,
        ),
        unknown,
    )
}
