package com.example.marvel.ui

import android.view.View
import android.widget.TextView
import com.example.marvel.R
import com.example.marvel.data.*

fun ScreenRenderer.timeline() {
    title("Character timeline")
    text("Follow the issues behind a character’s story, using documented publication dates.")
    actionRow(if (vm.timelineCharacter == null) "Select a character" else "Change character") {
        vm.selectionPurpose = "timeline"
        vm.navigate("character-select")
    }
    val hero =
        vm.timelineCharacter
            ?: run {
                state(
                    "No character selected",
                    "Choose a character to explore documented appearances.",
                )
                return
            }
    section(hero.name)
    character(hero)
    if (vm.timeline.value == null && !vm.timeline.loading && vm.timeline.error == null)
        vm.loadTimeline()
    if (vm.timeline.loading)
        state(
            "Building timeline",
            "Retrieving dated issues. Up to 20 appearances per character.",
            true,
        )
    if (vm.timeline.error != null) {
        val errorTitle =
            if (vm.timeline.value.isNullOrEmpty()) "Signal lost" else "Partial timeline"
        state(errorTitle, vm.timeline.error!!, retry = { vm.loadTimeline(true) })
    }
    tabs(listOf("All", "Dated issues", "First appearance"), vm.timelineFilter) {
        vm.timelineFilter = it
        vm.notifyChanged()
    }
    note("Up to 20 documented appearances are included in this timeline.")
    val firstId = hero.reference("first_appeared_in_issue")?.id
    val entries =
        vm.timeline.value.orEmpty().filter {
            when (vm.timelineFilter) {
                "Dated issues" -> it.text("cover_date").isNotBlank()
                "First appearance" -> it.id == firstId
                else -> true
            }
        }
    if (entries.isEmpty() && !vm.timeline.loading && vm.timeline.error == null) {
        state(
            "No chronological data available",
            "This record has no issues for the selected filter. Try All or another character.",
        )
    }
    entries.forEach { item ->
        section(item.text("cover_date").ifBlank { "Date unavailable" })
        if (item.id == firstId) note("First appearance")
        character(item)
        val summary = text(plain(item.deck).ifBlank { "Open this issue for its complete record." })
        summary.visibility = View.GONE
        val expand = actionRow("Expand entry") {}
        expand.setOnClickListener {
            val expanded = summary.visibility == View.VISIBLE
            summary.visibility = if (expanded) View.GONE else View.VISIBLE
            expand.findViewById<TextView>(R.id.action_title).text =
                if (expanded) "Expand entry" else "Collapse entry"
        }
    }
    section("Related records without chronology")
    text(
        "Comic Vine does not date team memberships, arc participation or individual volume appearances. These records are shown separately rather than assigned invented dates."
    )
    links("Volumes", hero.related("volume_credits"))
    links("Story arcs", hero.related("story_arc_credits"))
    links("Teams", hero.related("teams"))
}
