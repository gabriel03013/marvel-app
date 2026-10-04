package com.example.marvel.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import com.example.marvel.MainActivity
import com.example.marvel.R
import com.example.marvel.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.DateFormat
import java.util.Date

/** All screen and row structures are inflated from XML; Kotlin binds data and actions. */
class ScreenRenderer(val activity: MainActivity, val vm: ArchiveViewModel) {
    lateinit var content: LinearLayout
    var dark = false
    private val images = ArchiveImages()
    private var imageScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun render(host: FrameLayout) {
        imageScope.cancel(); imageScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        host.removeAllViews()
        if (vm.route.screen in listOf("welcome", "sign-in")) { welcome(host); return }
        if (vm.route.screen in listOf("email-sign-in", "sign-up", "password-reset")) { emailAuth(host); return }
        dark = false
        val page = activity.layoutInflater.inflate(R.layout.screen_page, host, false)
        content = page.findViewById(R.id.page_content); host.addView(page)
        if(activity.resources.configuration.screenWidthDp >= 600) {
            content.layoutParams = FrameLayout.LayoutParams(dp(640).coerceAtMost(dp(activity.resources.configuration.screenWidthDp - 104)), ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
        }
        when(vm.route.screen) {
            "splash" -> { title("S.H.I.E.L.D.\nArchives"); text("SEARCH. DISCOVER. RECRUIT."); state("Opening the archive", "Restoring your session…", loading = true) }
            "home" -> home()
            "search", "global-search" -> search()
            "archives" -> archives()
            "archive-list" -> archiveList()
            "detail" -> detail()
            "related" -> related()
            "image" -> imageViewer()
            "recruit" -> recruit()
            "briefing" -> briefing()
            "picker", "character-select" -> picker()
            "assembly" -> assembly()
            "report", "saved-team" -> report()
            "collection", "favorites", "saved-teams", "mission-history", "recent" -> collection()
            "profile", "first-run-profile" -> profile()
            "settings" -> settings()
            "compare" -> compareSetup()
            "compare-result" -> compareResult()
            "timeline" -> timeline()
            else -> state("Dossier unavailable", "Return to the archive and select a record.", retry = { vm.destination("home") })
        }
    }
    fun renderNavigation(nav: LinearLayout) {
        nav.removeAllViews()
        val tabs = listOf(Triple("home", "Home", R.drawable.ic_home), Triple("search", "Search", R.drawable.ic_search), Triple("recruit", "Recruit", R.drawable.ic_recruit), Triple("archives", "Archives", R.drawable.ic_archives), Triple("collection", "Collection", R.drawable.ic_collection))
        fun destination(route: Route): String? = when(route.screen) {
            "home", "search", "recruit", "archives", "collection" -> route.screen
            "briefing", "picker", "assembly", "report" -> "recruit"
            "archive-list", "detail", "image" -> if(route.kind == "character") "search" else "archives"
            "saved-team", "favorites", "saved-teams", "mission-history", "recent" -> "collection"
            else -> null
        }
        val active = vm.stack.asReversed().firstNotNullOfOrNull(::destination) ?: "home"
        val expanded = activity.resources.configuration.screenWidthDp >= 600
        nav.orientation = if(expanded) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        for((route, label, icon) in tabs) {
            val item = activity.layoutInflater.inflate(R.layout.nav_item, nav, false)
            if(expanded) item.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val color = color(R.color.paper_cream)
            if(route == active) item.setBackgroundResource(R.drawable.nav_selected)
            item.findViewById<ImageView>(R.id.nav_icon).apply { setImageResource(icon); imageTintList = ColorStateList.valueOf(color) }
            item.findViewById<TextView>(R.id.nav_label).apply { text = label; setTextColor(color) }
            item.isSelected = route == active; item.contentDescription = label + if(route == active) ", selected" else ""
            item.setOnClickListener { activity.hideKeyboard(); vm.destination(route) }
            nav.addView(item)
        }
    }
    private fun welcome(host: FrameLayout) {
        val view = activity.layoutInflater.inflate(R.layout.screen_welcome, host, false); host.addView(view)
        if(activity.resources.configuration.screenWidthDp >= 600) {
            view.findViewById<LinearLayout>(R.id.welcome_content)?.layoutParams = LinearLayout.LayoutParams(dp(520), ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
        }
        view.findViewById<Button>(R.id.sign_in).apply {
            text = if(vm.authBusy) "Please wait…" else if(vm.authError != null) "Retry Google sign-in" else "Continue with Google"
            isEnabled = !vm.authBusy
            setOnClickListener { if(vm.route.screen == "welcome") vm.navigate("sign-in"); activity.signIn() }
        }
        view.findViewById<Button>(R.id.email_sign_in).apply {
            isEnabled = !vm.authBusy
            setOnClickListener { vm.openAuth("email-sign-in") }
        }
        view.findViewById<Button>(R.id.create_account).apply {
            isEnabled = !vm.authBusy
            setOnClickListener { vm.openAuth("sign-up") }
        }
        view.findViewById<ProgressBar>(R.id.sign_in_loading).visibility = if(vm.authBusy) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.sign_in_error).text = vm.authError.orEmpty()
    }
    fun color(id: Int) = activity.getColor(id)
    fun <T : View> block(layout: Int, parent: LinearLayout = content): T {
        @Suppress("UNCHECKED_CAST") val view = activity.layoutInflater.inflate(layout, parent, false) as T
        parent.addView(view); return view
    }
    fun title(value: String): TextView {
        val panel = block<LinearLayout>(R.layout.block_cover)
        val compact = vm.route.screen in listOf("global-search", "detail", "image", "archive-list", "related", "picker", "character-select") || (vm.route.screen == "search" && vm.submittedQueries["search"] != null)
        panel.findViewById<CollageArtView>(R.id.cover_art).apply {
            variant = if(vm.route.screen in listOf("recruit", "briefing", "assembly", "report", "saved-team", "archives")) 2 else 0
            layoutParams.height = dp(if(compact) 80 else 112)
            layoutParams.width = dp(if(compact) 88 else 112)
        }
        if(vm.route.screen == "splash") content.gravity = android.view.Gravity.CENTER_VERTICAL
        return panel.findViewById<TextView>(R.id.cover_title).apply {
            text = value
            if(compact) textSize = 36f
            if(vm.route.screen == "splash") { textSize = 48f; gravity = android.view.Gravity.CENTER }
            setTextColor(color(R.color.ink_black))
            isAccessibilityHeading = true
        }
    }
    fun heroArtwork(kind: String = "heroes", height: Int = 180) {
        content.addView(CollageArtView(activity).apply {
            variant = if(kind == "cosmic") 2 else 0
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height)).apply { bottomMargin = dp(16) }
        })
    }
    fun section(value: String) = block<TextView>(R.layout.block_section).apply {
        text = value; setTextColor(color(R.color.ink_black)); isAccessibilityHeading = true
    }
    fun text(value: String) = block<TextView>(R.layout.block_text).apply { text = value; if(dark) setTextColor(color(R.color.paper_cream)) }
    fun button(label: String, enabled: Boolean = true, primary: Boolean = false, action: () -> Unit): Button = block<Button>(R.layout.block_button).apply {
        text = label
        if(!primary) { setBackgroundResource(R.drawable.button_outline); setTextColor(color(if(dark) R.color.paper_cream else R.color.ink_black)) }
        isEnabled = enabled; alpha = if(enabled) 1f else .55f; setOnClickListener { action() }
    }
    fun actionRow(label: String, copy: String = "", action: () -> Unit): LinearLayout = block<LinearLayout>(R.layout.row_action).apply {
        findViewById<TextView>(R.id.action_title).text = label
        findViewById<TextView>(R.id.action_copy).apply { text = copy; visibility = if(copy.isBlank()) View.GONE else View.VISIBLE }
        setOnClickListener { action() }
    }
    fun category(label: String, copy: String, tone: String = "paper", action: () -> Unit): LinearLayout = actionRow(label, copy, action).apply {
        val foreground = color(if(tone == "blue") R.color.archive_blue else if(tone == "red") R.color.deep_red else R.color.ink_black)
        findViewById<TextView>(R.id.action_title).apply { textSize = 28f; setTextColor(foreground) }
        findViewById<TextView>(R.id.action_copy).setTextColor(foreground)
        findViewById<ImageView>(R.id.action_arrow).imageTintList = ColorStateList.valueOf(foreground)
    }
    fun dataPair(label: String, value: String): LinearLayout = block<LinearLayout>(R.layout.block_data).apply {
        findViewById<TextView>(R.id.data_label).text = label
        findViewById<TextView>(R.id.data_value).text = value.ifBlank { "Not documented" }
    }
    fun note(value: String) = text(value).apply {
        textSize = 13f; setLineSpacing(dp(3).toFloat(), 1f)
        setTextColor(color(if(dark) R.color.stage_muted else R.color.muted_ink))
    }
    fun missionProgress(step: Int) {
        block<LinearLayout>(R.layout.block_progress).apply {
            listOf(R.id.step_brief, R.id.step_team, R.id.step_report).forEachIndexed { index, id ->
                findViewById<TextView>(id).apply {
                    background = CollageSurface(activity, if(index + 1 == step) "yellow" else "paper")
                    isSelected = index + 1 == step
                    contentDescription = "Step ${index + 1} of 3: $text" + if(isSelected) ", current" else ""
                }
            }
        }
    }
    fun scorePanel(score: Int, copy: String) {
        block<LinearLayout>(R.layout.block_score).apply {
            background = CollageSurface(activity, "paper")
            findViewById<TextView>(R.id.score_value).text = "$score / 100"
            findViewById<TextView>(R.id.score_copy).text = copy
        }
    }
    fun teamCard(team: SavedTeam, action: () -> Unit) = actionRow(team.name, "${team.members.size} members · ${date(team.createdAt)}", action)
    fun input(label: String, initial: String, multiline: Boolean = false, changed: (String) -> Unit): EditText {
        text(label).apply { labelFor = View.generateViewId() }
        return block<EditText>(R.layout.block_input).apply {
            id = (content.getChildAt(content.childCount - 2) as TextView).labelFor
            tag = "input_$label"; hint = label; setText(initial)
            if(multiline) { isSingleLine = false; minLines = 3; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }
            else imeOptions = EditorInfo.IME_ACTION_DONE
            doAfterTextChanged { changed(it.toString()) }
        }
    }
    fun state(heading: String, copy: String, loading: Boolean = false, retry: (() -> Unit)? = null) {
        block<LinearLayout>(R.layout.block_state).apply {
            background = CollageSurface(activity, "paper", 1)
            findViewById<TextView>(R.id.state_title).text = heading
            findViewById<TextView>(R.id.state_copy).text = copy
            findViewById<ProgressBar>(R.id.state_loading).visibility = if(loading) View.VISIBLE else View.GONE
            findViewById<View>(R.id.state_skeleton).visibility = if(loading) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.state_retry).apply { visibility = if(retry != null) View.VISIBLE else View.GONE; setOnClickListener { retry?.invoke() } }
        }
    }
    fun image(item: ArchiveItem, large: Boolean = true, action: (() -> Unit)? = null, onState: (ArchiveImageState) -> Unit = {}): ImageView {
        val frame = block<FrameLayout>(R.layout.block_image)
        frame.background = CollageSurface(activity, "paper", 1)
        val view = frame.findViewById<ImageView>(R.id.large_image)
        frame.layoutParams.height = dp(if(vm.route.screen == "image") 420 else 280)
        loadImage(view, item, onState)
        if(!large) frame.layoutParams.height = dp(100)
        if(action != null) { view.setOnClickListener { action() }; view.contentDescription = "Open full image of ${item.name}" }
        return view
    }
    fun loadImage(view: ImageView, item: ArchiveItem, onState: (ArchiveImageState) -> Unit) = images.load(view, item.image, "${item.name} image", imageScope, onState)
    fun character(item: ArchiveItem, selected: Boolean = false, action: (() -> Unit)? = null) {
        block<LinearLayout>(R.layout.row_character).apply {
            tag = "${item.kind}/${item.id}"
            findViewById<TextView>(R.id.record_name).text = item.name
            val metadata = listOf(item.text("real_name"), item.reference("publisher")?.name.orEmpty(), item.text("cover_date"), item.text("issue_number").takeIf { it.isNotBlank() }?.let { "Issue #$it" }.orEmpty()).filter { it.isNotBlank() }
            findViewById<TextView>(R.id.record_meta).text = metadata.joinToString(" · ").ifBlank { displayKind(item.kind) }
            findViewById<TextView>(R.id.record_deck).apply { text = plain(item.deck); visibility = if(item.deck.isBlank()) View.GONE else View.VISIBLE }
            findViewById<TextView>(R.id.record_selected).apply {
                visibility = if(selected) View.VISIBLE else View.GONE
                text = if(vm.selectionPurpose == "mission") "Selected · Tap to remove" else "Current selection · Tap to confirm"
            }
            images.load(findViewById(R.id.record_image), item.image, "${item.name} portrait", imageScope)
            isSelected = selected
            if(selected) {
                val outline = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.TRANSPARENT); setStroke(dp(2), color(R.color.archive_red)); cornerRadius = dp(4).toFloat() }
                background = android.graphics.drawable.LayerDrawable(arrayOf(CollageSurface(activity, "paper"), outline))
                setPadding(dp(8),dp(12),dp(8),dp(12))
            }
            setOnClickListener { action?.invoke() ?: open(item) }
        }
    }
    fun open(item: ArchiveItem) { vm.navigate("detail", item.kind, item.id.toString()) }
    fun missionCard(mission: Mission, featured: Boolean = false) {
        block<LinearLayout>(R.layout.row_mission).apply {
            findViewById<CollageArtView>(R.id.mission_art).variant = 2
            findViewById<TextView>(R.id.mission_title).text = mission.title
            findViewById<TextView>(R.id.mission_description).text = mission.description
            findViewById<TextView>(R.id.mission_action).text = if(featured) "Start a Mission" else "Read Briefing · Up to ${mission.size} members"
            setOnClickListener { chooseMission(mission) }
        }
    }
    fun chooseMission(mission: Mission) {
        if(vm.hasRecruitmentDraft) {
            AlertDialog.Builder(activity).setTitle("Replace your active team?").setMessage("Starting ${mission.title} clears your current recruitment draft. You can keep it and continue recruiting.")
                .setNegativeButton("Keep current team", null).setPositiveButton("Start new mission") { _, _ -> vm.startMission(mission) }.show()
        } else vm.startMission(mission)
    }
    fun profileRow(action: (() -> Unit)? = null) {
        val user = vm.user ?: return
        val row = block<LinearLayout>(R.layout.row_profile)
        row.setPadding(0, dp(8), 0, dp(20))
        val photo = row.findViewById<ImageView>(R.id.profile_photo)
        if(user.photoUrl != null) images.load(photo, user.photoUrl.toString(), "${user.displayName ?: "Agent"} profile picture", imageScope)
        else photo.contentDescription = "Profile picture unavailable"
        row.findViewById<TextView>(R.id.profile_name).apply { text = user.displayName ?: "Agent"; setTextColor(color(R.color.ink_black)) }
        row.findViewById<TextView>(R.id.profile_email).apply { text = user.email?.takeIf { it.isNotBlank() } ?: if(action != null) "View agent profile" else "Email unavailable"; setTextColor(color(R.color.muted_ink)) }
        if(action != null) { row.isFocusable = true; row.setOnClickListener { action() } }
    }
    fun tabs(options: List<String>, selected: String, unavailable: Set<String> = emptySet(), action: (String) -> Unit) {
        val scroll = block<HorizontalScrollView>(R.layout.block_tabs)
        val row = scroll.findViewById<LinearLayout>(R.id.chip_row)
        options.forEach { option ->
            val chip = activity.layoutInflater.inflate(R.layout.block_chip, row, false) as RadioButton
            chip.text = option; chip.isChecked = option == selected; chip.isEnabled = option !in unavailable
            chip.alpha = if(chip.isEnabled) 1f else .55f
            chip.contentDescription = option + if(!chip.isEnabled) ", unavailable: alignment not documented" else if(chip.isChecked) ", selected" else ""
            chip.setOnClickListener { action(option) }; row.addView(chip)
        }
        scroll.doOnLayout {
            val selectedChip = (0 until row.childCount).map { row.getChildAt(it) }
                .filterIsInstance<RadioButton>().firstOrNull { it.isChecked }
            if(selectedChip != null) {
                val offset = selectedChip.left + selectedChip.width / 2 - scroll.width / 2
                scroll.scrollTo(offset.coerceIn(0, (row.width - scroll.width).coerceAtLeast(0)), 0)
            }
        }
    }
    fun syncStatus() {
        if(vm.collectionLoading) state("Loading your collection", "Retrieving your saved dossiers…", true)
        if(vm.persistenceError != null) state("Collection sync interrupted", vm.persistenceError!!, retry = { vm.retryCollections() })
        else if(vm.collectionPending) text("Changes are stored locally and waiting to sync with Firebase.")
        else if(vm.collectionCached) text("Showing cached collection data. It may not reflect your latest changes on other devices.")
    }
    fun searchField(context: String, hint: String, submit: (String) -> Unit) {
        val row = block<LinearLayout>(R.layout.block_search)
        val field = row.findViewById<EditText>(R.id.query)
        field.tag = "query_$context"; field.hint = hint; field.setText(vm.queries[context].orEmpty())
        field.doAfterTextChanged { vm.queries[context] = it.toString() }
        row.findViewById<ImageButton>(R.id.clear_query).setOnClickListener { field.setText(""); field.requestFocus() }
        val run = { activity.hideKeyboard(); submit(field.text.toString().trim()) }
        field.setOnEditorActionListener { _, action, _ -> if(action == EditorInfo.IME_ACTION_SEARCH) { run(); true } else false }
        button("Search", primary = true) { run() }
    }
    fun results(kind: String, query: String, selection: Boolean = false) {
        val key = vm.pageKey(kind, query); val remote = vm.pages[key] ?: return
        if(remote.loading) state("Retrieving dossiers", "Connecting to Comic Vine…", loading = true)
        if(remote.error != null) state("Signal lost", remote.error, retry = { vm.loadPage(kind, query, more = remote.append, refresh = true) })
        remote.value?.let { page ->
            val filterMarvel = kind == "character" && vm.marvelOnly && !selection
            val items = if(filterMarvel) page.items.filter { it.reference("publisher")?.id == 31 || it.reference("publisher")?.name.equals("Marvel", true) } else page.items
            text(if(filterMarvel) "${items.size} Marvel records in ${page.items.size} loaded results. Publisher filtering applies to loaded records." else "${page.total} results · ${page.items.size} loaded")
            if(items.isEmpty() && !remote.loading) state(if(kind == "character") "No agents found" else "No records found", if(filterMarvel) "No Marvel matches on the loaded pages. Load more or choose All." else "Try another name or browse the archive.")
            for(item in items) character(item, selected = selection && isCandidateSelected(item), action = if(selection) ({ vm.select(item) }) else null)
            if(page.hasMore) button(if(remote.loading) "Loading more…" else "Load more", !remote.loading) { vm.loadPage(kind, query, more = true) }
        }
    }
    fun isCandidateSelected(item: ArchiveItem): Boolean = when(vm.selectionPurpose) {
        "compareA" -> vm.compareA?.id == item.id
        "compareB" -> vm.compareB?.id == item.id
        "timeline" -> vm.timelineCharacter?.id == item.id
        else -> vm.roster.any { it.id == item.id }
    }
    fun links(heading: String, records: List<ArchiveItem>) {
        section(heading)
        if(records.isEmpty()) text("No data available")
        records.take(8).forEach { record -> actionRow(record.name, displayKind(record.kind)) { open(record) } }
        if(records.size > 8) {
            text("${records.size} linked records")
            button("View all $heading") { vm.relatedItems = records; vm.relatedHeading = heading; vm.relatedLimit = 20; vm.navigate("related") }
        }
    }
    fun comparisonImages(a: ArchiveItem, b: ArchiveItem) {
        val row = block<LinearLayout>(R.layout.block_comparison)
        images.load(row.findViewById(R.id.compare_image_a), a.image, a.name, imageScope)
        images.load(row.findViewById(R.id.compare_image_b), b.image, b.name, imageScope)
        row.findViewById<TextView>(R.id.compare_text_a).text = "A · ${a.name}"
        row.findViewById<TextView>(R.id.compare_text_b).text = "B · ${b.name}"
    }
    fun comparisonRow(a: String, b: String) {
        val row = block<LinearLayout>(R.layout.block_comparison)
        row.findViewById<ImageView>(R.id.compare_image_a).visibility = View.GONE
        row.findViewById<ImageView>(R.id.compare_image_b).visibility = View.GONE
        row.findViewById<TextView>(R.id.compare_text_a).text = a
        row.findViewById<TextView>(R.id.compare_text_b).text = b
    }
    fun dp(value: Int) = (activity.resources.displayMetrics.density * value).toInt()
    fun plain(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().replace("\uFFFC", "").trim().replace(Regex("\\n{3,}"), "\n\n")
    fun date(millis: Long) = if(millis > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis)) else "Date unavailable"
    fun displayKind(kind: String) = kind.replace('_', ' ').replaceFirstChar { it.uppercase() }
    fun confirmDelete(team: SavedTeam) {
        AlertDialog.Builder(activity).setTitle("Delete ${team.name}?").setMessage("This removes the saved team. Your completed mission history will remain.").setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ -> vm.deleteTeam(team) }.show()
    }
}
