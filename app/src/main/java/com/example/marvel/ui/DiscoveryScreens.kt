package com.example.marvel.ui

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.marvel.R
import com.example.marvel.data.*

fun ScreenRenderer.search() {
    val global = vm.route.screen == "global-search"
    title(if (global) "Quick search" else "Find a character")
    if(vm.submittedQueries["search"] == null && !global) text("Search character names and open their dossiers.")
    val context = "search"
    searchField(context, "Search characters…") { query ->
        vm.submittedQueries[context] = query
        vm.loadPage("character", query, refresh = true)
    }
    tabs(
        listOf("All", "Marvel", "Heroes", "Villains"),
        if (vm.marvelOnly) "Marvel" else "All",
        setOf("Heroes", "Villains")
    ) {
        vm.marvelOnly = it == "Marvel"
        vm.notifyChanged()
    }

    val submitted = vm.submittedQueries[context]
    if (submitted != null) {
        if (submitted.isBlank()) {
            actionRow(if (vm.sort == "name:asc") "Sort: Name A–Z" else "Sort: Name Z–A", "Tap to reverse the alphabetical order.") {
                vm.sort = if (vm.sort == "name:asc") "name:desc" else "name:asc"
                vm.loadPage("character", "", refresh = true)
            }
        } else {
            note("Search results use Comic Vine relevance order.")
        }
        results("character", submitted)
        note("Alignment filters are unavailable: Comic Vine does not reliably document heroes and villains.")
        return
    }

    if (vm.searches.isNotEmpty()) {
        section("Recent searches")
        vm.searches.forEach { query ->
            actionRow(query, "Search again") {
                vm.queries[context] = query
                vm.submittedQueries[context] = query
                vm.loadPage("character", query)
            }
        }
    }
    section("Start with a name")
    listOf("Spider-Man", "Captain Marvel", "Black Panther").forEach { query ->
        actionRow(query, "Search Comic Vine") {
            vm.queries[context] = query
            vm.submittedQueries[context] = query
            vm.loadPage("character", query)
        }
    }
    category("Browse characters", "Explore the complete character archive.", "blue") {
        vm.submittedQueries[context] = ""
        vm.loadPage("character", "")
    }
    section("From your collection")
    syncStatus()
    vm.favorites.take(3).forEach { character(it) }
    if (vm.favorites.isEmpty() && !vm.collectionLoading && vm.persistenceError == null) {
        text("Favorite a character to keep a dossier close at hand.")
    }
}

fun ScreenRenderer.archives() {
    title("The archives")
    text("Characters, abilities and the stories that connect them. Data from Comic Vine.")
    val categories = listOf(
        Triple("character", "Characters", "Identities, histories and documented appearances."),
        Triple("power", "Powers", "Abilities and the characters linked to them."),
        Triple("team", "Teams", "Members, origins and shared comic appearances."),
        Triple("story_arc", "Story Arcs", "Connected stories, issues and participants."),
        Triple("issue", "Issues", "Comic covers, publication dates and credits."),
        Triple("volume", "Volumes", "Comic series and the issues they collect."),
        Triple("publisher", "Publishers", "Publishing records and their linked titles."),
        Triple("location", "Locations", "Places and their documented comic connections.")
    )
    categories.forEachIndexed { index, (kind, label, copy) ->
        when(index) { 1 -> section("Abilities & affiliations"); 3 -> section("On the comic shelf"); 6 -> section("People & places") }
        if(index == 0) category(label, copy, "red") { vm.navigate("archive-list", kind) }
        else actionRow(label, copy) { vm.navigate("archive-list", kind) }
    }
    section("Featured publisher")
    actionRow("Marvel", "Open the publisher record in Comic Vine.") {
        vm.navigate("detail", "publisher", "31")
    }
    section("Research tools")
    actionRow("Compare characters", "Place two dossiers side by side.") { vm.navigate("compare") }
    actionRow("Character timeline", "Follow appearances with documented dates.") { vm.navigate("timeline") }
    section("Recently viewed")
    if (vm.recent.isEmpty()) text("Opened character dossiers will appear here.")
    vm.recent.take(4).forEach { character(it) }
}

fun ScreenRenderer.archiveList() {
    val kind = vm.route.kind
    val label = displayKind(ComicVineRepository.plural(kind))
    title("$label archive")
    if(vm.submittedQueries["archive_$kind"] == null) note(when (kind) {
        "power" -> "Explore documented abilities and their linked characters."
        "team" -> "Explore team histories and documented members."
        "story_arc" -> "Follow stories through their connected issues and characters."
        "issue" -> "Browse comic issues, publication dates and creator credits."
        "volume" -> "Find comic series and the issues collected within them."
        "publisher" -> "Explore publishers and their linked comic records."
        "location" -> "Explore places documented in the comic archive."
        else -> "Open a character dossier to discover their history and connections."
    })
    val context = "archive_$kind"
    searchField(context, "Search ${label.lowercase()}…") { query ->
        vm.submittedQueries[context] = query
        vm.loadPage(kind, query, refresh = true)
    }
    val query = vm.submittedQueries[context].orEmpty()
    if (vm.pages[vm.pageKey(kind, query)] == null) vm.loadPage(kind, query)
    results(kind, query)
}

fun ScreenRenderer.detail() {
    val kind = vm.route.kind
    val id = vm.route.id.toIntOrNull() ?: 0
    val key = "$kind/$id"
    if (vm.graphRecordKey != key) { vm.graphRecordKey = key; vm.graphExpanded = false }
    if (vm.details[key] == null) vm.loadDetail(kind, id)
    val remote = vm.details[key]
    title(remote?.value?.name ?: if (kind == "character") "Hero diary" else "${displayKind(kind)} dossier")
    if (remote?.loading == true) state("Opening dossier", "Retrieving the complete archive record…", loading = true)
    if (remote?.error != null) state("Signal lost", remote.error, retry = { vm.loadDetail(kind, id, true) })
    val item = remote?.value ?: return
    image(item, action = { vm.navigate("image", kind, id.toString()) })
    if (kind == "character") {
        button("Add to Team", primary = true) { vm.addFromDetail(item) }
        dataPair("Real name", item.text("real_name").ifBlank { "Not documented" })
        dataPair("Publisher", item.reference("publisher")?.name ?: "Not documented")
        dataPair("Origin", item.reference("origin")?.name ?: "Not documented")
        val favorite = vm.favorites.any { it.id == id }
        actionRow(if (favorite) "Remove Favorite" else "Favorite", if (favorite) "Remove this character from your favorites." else "Keep this character in your collection.") {
            vm.favorite(item)
        }.apply {
            isEnabled = !vm.operationLoading
            alpha = if (isEnabled) 1f else .55f
        }
        actionRow("Compare", "Place this dossier beside another character.") {
            vm.compareA = item
            vm.navigate("compare")
        }
        actionRow("Timeline", "Explore documented comic appearances.") {
            vm.timelineCharacter = item
            vm.timeline = Remote()
            vm.navigate("timeline")
            vm.loadTimeline()
        }
    }
    section("Overview")
    val description = plain(item.text("description").ifBlank { item.deck }).ifBlank { "No description available." }
    val overview = text(description)
    if (description.length > 900) {
        overview.maxLines = 12
        overview.ellipsize = TextUtils.TruncateAt.END
        val expand = actionRow("Read full overview") {}
        expand.setOnClickListener {
            val expanded = overview.maxLines == Int.MAX_VALUE
            overview.maxLines = if (expanded) 12 else Int.MAX_VALUE
            overview.ellipsize = if (expanded) TextUtils.TruncateAt.END else null
            expand.findViewById<TextView>(R.id.action_title).text = if (expanded) "Read full overview" else "Collapse overview"
        }
    }
    val first = item.reference("first_appeared_in_issue")
    when (kind) {
        "character" -> {
            val connections = buildList {
                item.related("powers").forEach { add(ConnectionNode("Power", it)) }
                item.related("teams").forEach { add(ConnectionNode("Team", it)) }
                item.related("character_friends").forEach { add(ConnectionNode("Ally", it)) }
                item.related("character_enemies").forEach { add(ConnectionNode("Enemy", it)) }
                item.reference("publisher")?.takeIf { it.id > 0 }?.let { add(ConnectionNode("Publisher", it)) }
                first?.takeIf { it.id > 0 }?.let { add(ConnectionNode("First issue", it)) }
                item.related("story_arc_credits").forEach { add(ConnectionNode("Story arc", it)) }
                item.related("issue_credits").forEach { add(ConnectionNode("Issue", it)) }
                item.related("volume_credits").forEach { add(ConnectionNode("Volume", it)) }
            }.filter { it.item.id > 0 }.distinctBy { "${it.item.kind}/${it.item.id}" }
            section("Explore connections")
            if (connections.isEmpty()) {
                state("No connections documented", "Comic Vine does not document enough direct relationships in this dossier to draw a connection map.")
            } else {
                val shown = connections.take(if (vm.graphExpanded) 24 else 8)
                content.addView(ConnectionGraphView(activity).apply {
                    centerLabel = item.name
                    nodes = shown
                    onNodeClick = { open(it) }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(176)).apply { bottomMargin = dp(8) })
                note("Connections shown from this Comic Vine dossier · up to 24 direct records. Tap a node or use the record list.")
                val relationKinds = shown.map { it.relation }.distinct()
                dataPair("Legend", relationKinds.joinToString(" · "))
                section("Connection records")
                shown.forEach { edge -> actionRow(edge.item.name, edge.relation) { open(edge.item) } }
                if (!vm.graphExpanded && connections.size > shown.size) button("Show more connections (${connections.size - shown.size} remaining)") { vm.graphExpanded = true; vm.notifyChanged() }
                else if (vm.graphExpanded && connections.size > 8) actionRow("Show fewer connections") { vm.graphExpanded = false; vm.notifyChanged() }
            }
            section("Archive intelligence")
            val metrics = buildList {
                item.related("powers").count { it.id > 0 }.takeIf { it > 0 }?.let { add(ArchiveMetric("Powers listed", it)) }
                item.related("teams").count { it.id > 0 }.takeIf { it > 0 }?.let { add(ArchiveMetric("Teams linked", it)) }
                item.text("count_of_issue_appearances").toIntOrNull()?.takeIf { it >= 0 }?.let { add(ArchiveMetric("Issue appearances", it)) }
                item.text("count_of_isssue_appearances").toIntOrNull()?.takeIf { count -> count >= 0 && none { it.label == "Issue appearances" } }?.let { add(ArchiveMetric("Issue appearances", it)) }
            }
            if (metrics.isEmpty() && first == null) text("No numeric archive counts or first appearance are documented in this dossier.")
            else {
                if (metrics.isNotEmpty()) content.addView(ArchiveMetricsView(activity).apply { this.metrics = metrics }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(metrics.size * 44).coerceAtLeast(48)))
                first?.let { dataPair("First appearance", it.name) }
                note("Counts reflect the data included in this Comic Vine dossier and may be incomplete. They describe archive records, not character strength.")
            }
            section("First appearance")
            if (first != null && first.id > 0) actionRow(first.name, "Open the issue record") { open(first) }
            else if (first != null) dataPair("First appearance", first.name)
            else text("No data available")
            section("Archive facts")
            dataPair("Issue appearances", item.text("count_of_issue_appearances").ifBlank { "Not documented" })
            links("Powers", item.related("powers"))
            links("Teams", item.related("teams"))
            links("Allies", item.related("character_friends"))
            links("Enemies", item.related("character_enemies"))
            links("Story arcs", item.related("story_arc_credits"))
            links("Issues", item.related("issue_credits"))
            links("Volumes", item.related("volume_credits"))
        }
        "power" -> {
            dataPair("Linked characters", item.related("characters").size.toString())
            links("Characters with this power", item.related("characters"))
        }
        "team" -> {
            section("Team facts")
            dataPair("Aliases", item.text("aliases").ifBlank { "Not documented" })
            val issues = item.text("count_of_issue_appearances").ifBlank { item.text("count_of_isssue_appearances") }
            dataPair("Issue appearances", issues.ifBlank { "Not documented" })
            if (first != null && first.id > 0) actionRow(first.name, "First appearance · Open issue") { open(first) }
            else if (first != null) dataPair("First appearance", first.name)
            button("Use this team as inspiration", !vm.operationLoading, primary = true) {
                if (vm.hasRecruitmentDraft) {
                    android.app.AlertDialog.Builder(activity)
                        .setTitle("Replace your active team?")
                        .setMessage("Using ${item.name} as inspiration clears your current recruitment draft. You can keep it and continue recruiting.")
                        .setNegativeButton("Keep current team", null)
                        .setPositiveButton("Use inspiration") { _, _ -> vm.inspire(item) }
                        .show()
                } else {
                    vm.inspire(item)
                }
            }
            links("Members", item.related("characters").ifEmpty { item.related("members") })
            links("Issues", item.related("issue_credits"))
            links("Volumes", item.related("volume_credits"))
        }
        "story_arc" -> {
            links("Issues in this arc", item.related("issues"))
            links("Characters", item.related("characters").ifEmpty { item.related("character_credits") })
            links("Teams", item.related("teams").ifEmpty { item.related("team_credits") })
        }
        "issue" -> {
            section("Publication facts")
            dataPair("Issue number", item.text("issue_number").ifBlank { "Not documented" })
            dataPair("Cover date", item.text("cover_date").ifBlank { "Not documented" })
            dataPair("Release date", item.text("store_date").ifBlank { "Not documented" })
            item.reference("volume")?.let {
                if (it.id > 0) actionRow(it.name, "Open volume") { open(it) }
                else dataPair("Volume", it.name)
            }
            links("Characters appearing", item.related("character_credits"))
            section("Writers and artists")
            val people = item.related("person_credits")
            if (people.isEmpty()) text("No data available")
            people.forEach { dataPair(it.text("role").ifBlank { "Contributor" }, it.name) }
        }
        "volume" -> {
            section("Publication facts")
            dataPair("Issues", item.text("count_of_issues").ifBlank { "Not documented" })
            dataPair("Start year", item.text("start_year").ifBlank { "Not documented" })
            item.reference("publisher")?.let {
                if (it.id > 0) actionRow(it.name, "Open publisher") { open(it) }
                else dataPair("Publisher", it.name)
            }
            links("Issues", item.related("issues"))
            links("Characters", item.related("characters"))
        }
        "publisher" -> {
            links("Characters", item.related("characters"))
            links("Volumes", item.related("volumes"))
            links("Issues", item.related("issues"))
        }
        "location" -> {
            links("Characters", item.related("characters"))
            links("Issues", item.related("issue_credits"))
        }
    }
    note("Source: Comic Vine. Records may be incomplete.")
    actionRow("Refresh dossier", "Retrieve the latest record from Comic Vine.") { vm.loadDetail(kind, id, true) }.apply {
        isEnabled = remote.loading.not()
        alpha = if (isEnabled) 1f else .55f
    }
}

fun ScreenRenderer.imageViewer() {
    val item = vm.details["${vm.route.kind}/${vm.route.id}"]?.value
    title(item?.name ?: "Archive image")
    if (item == null) {
        state("Image unavailable", "Open the record again to load its image.")
        actionRow("Return to dossier") { vm.back() }
        return
    }
    val status = text("Loading image…").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
    val retry = button("Retry image", primary = true) {}.apply { visibility = View.GONE }
    val update: (ArchiveImageState) -> Unit = { state ->
        status.visibility = if(state == ArchiveImageState.READY) View.GONE else View.VISIBLE
        status.text = when(state) {
            ArchiveImageState.LOADING -> "Loading image…"
            ArchiveImageState.MISSING -> "No image available for this record."
            ArchiveImageState.FAILED -> "Could not load this image. Check your connection and retry."
            ArchiveImageState.READY -> ""
        }
        retry.visibility = if(state == ArchiveImageState.FAILED) View.VISIBLE else View.GONE
    }
    val picture = image(item, onState = update)
    retry.setOnClickListener { loadImage(picture, item, update) }
    note("Image supplied by Comic Vine.")
    actionRow("Return to dossier") { vm.back() }
}

fun ScreenRenderer.compareSetup() {
    title("Compare dossiers")
    text("Place two characters side by side. Compare documented facts, with missing data shown as unknown.")
    section("Character A")
    vm.compareA?.let { character(it) } ?: text("Choose your first character.")
    actionRow(if (vm.compareA == null) "Select Character A" else "Change Character A") {
        vm.selectionPurpose = "compareA"
        vm.navigate("character-select")
    }
    section("Character B")
    vm.compareB?.let { character(it) } ?: text("Choose your second character.")
    actionRow(if (vm.compareB == null) "Select Character B" else "Change Character B") {
        vm.selectionPurpose = "compareB"
        vm.navigate("character-select")
    }
    actionRow("Swap characters", "Exchange the A and B positions.") {
        val a = vm.compareA
        vm.compareA = vm.compareB
        vm.compareB = a
        vm.notifyChanged()
    }.apply {
        isEnabled = vm.compareA != null || vm.compareB != null
        alpha = if (isEnabled) 1f else .55f
    }
    val ready = vm.compareA != null && vm.compareB != null && vm.compareA?.id != vm.compareB?.id
    if (vm.compareA != null && vm.compareA?.id == vm.compareB?.id) note("Choose two different characters.")
    button("Compare Profiles", ready, primary = true) { vm.navigate("compare-result") }
}

fun ScreenRenderer.compareResult() {
    title("Profile comparison")
    val a = vm.compareA
    val b = vm.compareB
    if (a == null || b == null) {
        state("Choose two characters", "Return to comparison setup.", retry = { vm.back() })
        return
    }
    listOf(a, b).forEach {
        if (vm.details["character/${it.id}"] == null) vm.loadDetail("character", it.id)
    }
    val left = vm.details["character/${a.id}"]
    val right = vm.details["character/${b.id}"]
    if (left?.loading == true || right?.loading == true) state("Comparing profiles", "Retrieving both complete dossiers…", true)
    if (left?.error != null || right?.error != null) {
        state("Signal lost", left?.error ?: right?.error.orEmpty(), retry = {
            vm.loadDetail("character", a.id, true)
            vm.loadDetail("character", b.id, true)
        })
    }
    val first = left?.value ?: return
    val second = right?.value ?: return
    comparisonImages(first, second)
    note("A: ${first.name} · B: ${second.name}")
    val rows = listOf(
        "Real name" to (first.text("real_name") to second.text("real_name")),
        "Publisher" to (first.reference("publisher")?.name.orEmpty() to second.reference("publisher")?.name.orEmpty()),
        "First appearance" to (first.reference("first_appeared_in_issue")?.name.orEmpty() to second.reference("first_appeared_in_issue")?.name.orEmpty()),
        "Issue appearances" to (first.text("count_of_issue_appearances") to second.text("count_of_issue_appearances")),
        "Powers" to (first.related("powers").joinToString { it.name } to second.related("powers").joinToString { it.name }),
        "Teams" to (first.related("teams").joinToString { it.name } to second.related("teams").joinToString { it.name })
    )
    rows.forEach { (heading, pair) ->
        section(heading)
        comparisonRow("A\n${pair.first.ifBlank { "Not documented" }}", "B\n${pair.second.ifBlank { "Not documented" }}")
    }
    val p1 = first.related("powers").map { it.name }.toSet()
    val p2 = second.related("powers").map { it.name }.toSet()
    section("Shared documented powers")
    text(p1.intersect(p2).joinToString().ifBlank { "No shared powers documented in these records." })
    section("Differences")
    dataPair("A · ${first.name}", p1.minus(p2).joinToString().ifBlank { "No additional powers documented" })
    dataPair("B · ${second.name}", p2.minus(p1).joinToString().ifBlank { "No additional powers documented" })
    val allPowers = p1.union(p2)
    val sharedPowers = p1.intersect(p2)
    val affinityScore = if (allPowers.isEmpty()) 0 else (sharedPowers.size * 100) / allPowers.size
    section("App-generated comparison")
    scorePanel(affinityScore, "${sharedPowers.size} shared of ${allPowers.size} combined documented powers")
    note("This compares Comic Vine records, not combat strength or official Marvel rankings.")
    actionRow("Change characters") { vm.back() }
}

fun ScreenRenderer.timeline() {
    title("Character timeline")
    text("Follow the issues behind a character’s story, using documented publication dates.")
    actionRow(if (vm.timelineCharacter == null) "Select a character" else "Change character") {
        vm.selectionPurpose = "timeline"
        vm.navigate("character-select")
    }
    val hero = vm.timelineCharacter ?: run {
        state("No character selected", "Choose a character to explore documented appearances.")
        return
    }
    section(hero.name)
    character(hero)
    if (vm.timeline.value == null && !vm.timeline.loading && vm.timeline.error == null) vm.loadTimeline()
    if (vm.timeline.loading) state("Building timeline", "Retrieving dated issues. Up to 20 appearances per character.", true)
    if (vm.timeline.error != null) {
        val errorTitle = if (vm.timeline.value.isNullOrEmpty()) "Signal lost" else "Partial timeline"
        state(errorTitle, vm.timeline.error!!, retry = { vm.loadTimeline(true) })
    }
    tabs(listOf("All", "Dated issues", "First appearance"), vm.timelineFilter) {
        vm.timelineFilter = it
        vm.notifyChanged()
    }
    note("Up to 20 documented appearances are included in this timeline.")
    val firstId = hero.reference("first_appeared_in_issue")?.id
    val entries = vm.timeline.value.orEmpty().filter {
        when (vm.timelineFilter) {
            "Dated issues" -> it.text("cover_date").isNotBlank()
            "First appearance" -> it.id == firstId
            else -> true
        }
    }
    if (entries.isEmpty() && !vm.timeline.loading && vm.timeline.error == null) {
        state("No chronological data available", "This record has no issues for the selected filter. Try All or another character.")
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
            expand.findViewById<TextView>(R.id.action_title).text = if (expanded) "Expand entry" else "Collapse entry"
        }
    }
    section("Related records without chronology")
    text("Comic Vine does not date team memberships, arc participation or individual volume appearances. These records are shown separately rather than assigned invented dates.")
    links("Volumes", hero.related("volume_credits"))
    links("Story arcs", hero.related("story_arc_credits"))
    links("Teams", hero.related("teams"))
}

fun ScreenRenderer.related() {
    title(vm.relatedHeading)
    note("${vm.relatedItems.size} linked records")
    if (vm.relatedItems.isEmpty()) state("No linked records", "Open a dossier and choose a related section.")
    vm.relatedItems.take(vm.relatedLimit).forEach { character(it) }
    if (vm.relatedItems.size > vm.relatedLimit) {
        button("Load more linked records") { vm.loadMoreRelated() }
    }
}
