package com.example.marvel

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marvel.data.*
import com.example.marvel.ui.Remote
import com.example.marvel.ui.Route
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Native capture matrix. Comic records are fetched live and reused across device configurations.
 * Authentication uses the local emulator; Firestore is offline on its emulator endpoint.
 * Saved collection/report snapshots are test-only compositions, never production seed content.
 */
@RunWith(AndroidJUnit4::class)
class DesignReviewTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val folder get() = File(instrumentation.targetContext.filesDir, "design-review").apply { mkdirs() }
    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 30_000
        while(!condition() && System.currentTimeMillis() < until) Thread.sleep(100)
        assertTrue("Review session should initialize", condition())
    }
    private fun descendants(view: View): List<View> = listOf(view) + if(view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun clickSystemAction(label: String) {
        waitFor { instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label)?.any { it.isClickable } == true }
        val node = instrumentation.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(label).first { it.isClickable }
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
    }
    private suspend fun record(api: ComicVineRepository, kind: String, id: Int): ArchiveItem {
        val file = File(folder, "record-$kind-$id.json")
        val raw = if(file.exists()) file.readText() else api.detail(kind,id).raw.also(file::writeText)
        return ArchiveItem.fromJson(JSONObject(raw),kind)
    }
    private suspend fun page(api: ComicVineRepository, kind: String, query: String = ""): Page {
        val file = File(folder, "page-$kind-${query.hashCode()}.json")
        if(file.exists()) {
            val j = JSONObject(file.readText())
            return Page(j.getJSONArray("items").items().map { ArchiveItem.fromJson(it,kind) },0,j.getInt("consumed"),j.getInt("total"))
        }
        return api.list(kind,query).also { p -> file.writeText(JSONObject().put("consumed",p.consumed).put("total",p.total).put("items",JSONArray(p.items.map { it.json })).toString()) }
    }
    private fun capture(name: String, scenario: ActivityScenario<MainActivity>, bottom: Boolean = false) {
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        scenario.onActivity { activity ->
            fun inspect(view: View) {
                if(view.visibility != View.VISIBLE) return
                if(view is Button) assertTrue("48dp target: ${view.text}",view.height >= 48*activity.resources.displayMetrics.density)
                if(view is TextView && view.layout != null && view.ellipsize == null && view.text.isNotEmpty()) {
                    assertTrue("Text must fit vertically: ${view.text.take(50)}", view.layout.getLineBottom(view.layout.lineCount-1) <= view.height-view.paddingTop-view.paddingBottom+2)
                }
                if(view is ViewGroup) (0 until view.childCount).forEach { inspect(view.getChildAt(it)) }
            }
            inspect(activity.binding.root)
            activity.binding.screenHost.findViewById<android.widget.ScrollView>(R.id.page_scroll)?.let { scroll ->
                val maxScroll = ((scroll.getChildAt(0)?.height ?: 0) - scroll.height + scroll.paddingTop + scroll.paddingBottom).coerceAtLeast(0)
                scroll.scrollTo(0, if(bottom) maxScroll else 0)
            }
        }
        instrumentation.waitForIdleSync()
        // Idle does not guarantee that SurfaceFlinger has presented the scrolled frame.
        Thread.sleep(350); instrumentation.waitForIdleSync()
        val device = InstrumentationRegistry.getArguments().getString("captureClass","phone")
        scenario.onActivity { activity ->
            val scroll = activity.binding.screenHost.findViewById<android.widget.ScrollView>(R.id.page_scroll)
            if(!bottom && scroll != null) assertEquals("First viewport starts at top: $name",0,scroll.scrollY)
            if(bottom && scroll != null) {
                val maxScroll = ((scroll.getChildAt(0)?.height ?: 0) - scroll.height + scroll.paddingTop + scroll.paddingBottom).coerceAtLeast(0)
                assertEquals("Bottom capture reaches content end: $name",maxScroll,scroll.scrollY)
            }
            File(folder,"$name-$device-state.json").writeText(JSONObject()
                .put("route",activity.model.route.screen).put("kind",activity.model.route.kind)
                .put("scrollY",scroll?.scrollY ?: 0).put("fontScale",activity.resources.configuration.fontScale)
                .put("screenWidthDp",activity.resources.configuration.screenWidthDp).put("bottom",bottom)
                .put("contentHeight",scroll?.getChildAt(0)?.height ?: 0).put("viewportHeight",scroll?.height ?: 0).toString())
        }
        val shot = instrumentation.uiAutomation.takeScreenshot()
        File(folder,"$name-$device.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun captureCollectionTabs() = runBlocking {
        val auth = FirebaseAuth.getInstance()
        auth.useEmulator("10.0.2.2",9099)
        val db = FirebaseFirestore.getInstance()
        db.useEmulator("10.0.2.2",8080)
        db.disableNetwork().awaitResult()
        auth.signOut()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        waitFor { activity.model.route.screen == "welcome" }
        auth.signInAnonymously().awaitResult()
        waitFor { activity.model.user != null }
        val api = ComicVineRepository()
        val spider = record(api,"character",1443)
        val gambit = record(api,"character",1499)
        val team = SavedTeam("design-review","Archive Field Team","cosmic",missions.first().description,listOf(spider,gambit),1_791_033_600_000)
        scenario.onActivity {
            it.model.collectionLoading=false; it.model.persistenceError=null
            it.model.favorites=listOf(spider,gambit); it.model.recent=listOf(gambit,spider)
            it.model.teams=listOf(team); it.model.history=listOf(team)
            it.model.collectionTab="Favorites"; it.model.destination("collection")
        }
        fun assertSelectedVisible(label: String) {
            scenario.onActivity {
                val row = it.binding.root.findViewById<android.widget.LinearLayout>(R.id.chip_row)
                val selected = descendants(row).filterIsInstance<android.widget.RadioButton>().single { chip -> chip.isChecked }
                assertEquals(label,selected.text.toString())
                val viewport = row.parent as android.widget.HorizontalScrollView
                val chipPosition=IntArray(2); val viewportPosition=IntArray(2)
                selected.getLocationOnScreen(chipPosition); viewport.getLocationOnScreen(viewportPosition)
                assertTrue("Selected tab starts inside viewport: $label",chipPosition[0] >= viewportPosition[0])
                assertTrue("Selected tab ends inside viewport: $label",chipPosition[0]+selected.width <= viewportPosition[0]+viewport.width)
            }
        }
        capture("collection",scenario); assertSelectedVisible("Favorites")
        for((route,label) in listOf("favorites" to "Favorites","saved-teams" to "Saved Teams","mission-history" to "Mission History","recent" to "Recently Viewed")) {
            scenario.onActivity { it.model.destination(route) }
            capture(route,scenario); assertSelectedVisible(label)
        }
        scenario.onActivity {
            it.model.collectionTab="Favorites"; it.model.destination("collection")
        }
        instrumentation.waitForIdleSync(); Thread.sleep(350)
        scenario.onActivity {
            descendants(it.binding.root.findViewById<android.widget.LinearLayout>(R.id.chip_row))
                .filterIsInstance<android.widget.RadioButton>().single { chip -> chip.text == "Recently Viewed" }.performClick()
        }
        capture("collection-after-tab-click",scenario); assertSelectedVisible("Recently Viewed")
        scenario.recreate()
        scenario.onActivity { activity=it }
        waitFor { activity.model.route.screen == "collection" }
        capture("collection-after-tab-rotation",scenario); assertSelectedVisible("Recently Viewed")
        scenario.close(); auth.signOut()
    }
    @Test fun captureContextNavigation() = runBlocking {
        val auth=FirebaseAuth.getInstance(); auth.useEmulator("10.0.2.2",9099)
        val db=FirebaseFirestore.getInstance(); db.useEmulator("10.0.2.2",8080); db.disableNetwork().awaitResult()
        auth.signOut()
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity=it }
        waitFor { activity.model.route.screen == "welcome" }
        auth.signInAnonymously().awaitResult()
        waitFor { activity.model.user != null }
        val api=ComicVineRepository(); val spider=record(api,"character",1443); val gambit=record(api,"character",1499)
        val characters=page(api,"character","Spider-Man")
        val firstIssue=spider.reference("first_appeared_in_issue")?.let { record(api,"issue",it.id) }
        val firstPower=spider.related("powers").first()
        val power=record(api,"power",firstPower.id)
        scenario.onActivity {
            it.model.details["character/1443"]=Remote(spider); it.model.details["character/1499"]=Remote(gambit)
            it.model.details["power/${power.id}"]=Remote(power)
            it.model.pages[it.model.pageKey("character","")]=Remote(characters)
            it.model.pages[it.model.pageKey("character","Spider-Man")]=Remote(characters)
            it.model.compareA=spider; it.model.compareB=gambit
            it.model.timelineCharacter=spider; it.model.timeline=Remote(listOfNotNull(firstIssue))
            it.model.collectionLoading=false; it.model.persistenceError=null
            it.model.destination("archives")
        }
        fun assertDestination(label: String) {
            scenario.onActivity {
                val nav=it.binding.bottomNav
                val selected=(0 until nav.childCount).map { n -> nav.getChildAt(n) }.filter { child -> child.isSelected }
                assertEquals("Exactly one primary destination remains selected",1,selected.size)
                assertEquals(label,selected.single().findViewById<TextView>(R.id.nav_label).text.toString())
                assertEquals("$label, selected",selected.single().contentDescription.toString())
            }
        }
        fun snapshot(name: String, selected: String) { capture(name,scenario); assertDestination(selected) }
        fun clickRow(label: String) {
            instrumentation.waitForIdleSync(); Thread.sleep(350)
            scenario.onActivity {
                val title=descendants(it.binding.screenHost).filterIsInstance<TextView>()
                    .single { view -> view.id == R.id.action_title && view.text.toString() == label }
                assertTrue((title.parent.parent as View).performClick())
            }
        }
        instrumentation.waitForIdleSync(); Thread.sleep(350)
        clickRow("Compare characters"); snapshot("compare","Archives")
        clickRow("Change Character A"); snapshot("character-select","Archives")
        scenario.onActivity { it.model.back() }; snapshot("navigation-compare-after-back","Archives")
        scenario.onActivity {
            descendants(it.binding.screenHost).filterIsInstance<Button>().single { b -> b.text == "Compare Profiles" }.performClick()
        }
        snapshot("compare-result","Archives")
        scenario.onActivity { it.model.back(); it.model.back() }
        clickRow("Character timeline"); snapshot("timeline","Archives")
        clickRow("Change character"); snapshot("navigation-timeline-selector","Archives")
        scenario.onActivity { it.model.back(); it.model.destination("search"); it.model.navigate("detail","character","1443") }
        clickRow("Compare"); snapshot("navigation-compare-from-dossier","Search")
        scenario.onActivity { it.model.back(); it.model.navigate("image","character","1443") }
        snapshot("image","Search")
        scenario.onActivity {
            it.model.back(); it.model.relatedHeading="Powers"; it.model.relatedItems=spider.related("powers")
            it.model.navigate("related")
        }
        snapshot("related","Search")
        scenario.onActivity { it.model.destination("home"); it.model.navigate("global-search") }
        snapshot("global-search","Home")
        scenario.onActivity { it.model.back(); it.model.navigate("profile") }; snapshot("profile","Home")
        clickRow("Settings"); snapshot("settings","Home")
        scenario.recreate(); scenario.onActivity { activity=it }
        waitFor { activity.model.route.screen == "settings" }
        snapshot("navigation-settings-after-recreation","Home")
        fun clickButton(label: String) {
            instrumentation.waitForIdleSync(); Thread.sleep(350)
            scenario.onActivity {
                assertTrue(descendants(it.binding.screenHost).filterIsInstance<Button>().single { b -> b.text.toString() == label }.performClick())
            }
        }
        scenario.onActivity { it.model.destination("search"); it.model.navigate("detail","character","1443") }
        clickButton("View all Powers"); snapshot("related","Search")
        lateinit var originalRelatedRoute: Route
        scenario.onActivity {
            originalRelatedRoute=it.model.route
            assertEquals("Powers",it.model.relatedHeading)
            assertEquals(spider.related("powers").map { item -> item.id },it.model.relatedItems.map { item -> item.id })
            assertTrue(it.binding.screenHost.findViewWithTag<View>("power/${power.id}").performClick())
        }
        clickButton("View all Characters with this power"); snapshot("related-nested","Archives")
        scenario.onActivity { assertNotEquals(originalRelatedRoute.id,it.model.route.id) }
        clickButton("Load more linked records")
        scenario.onActivity { assertEquals(40,it.model.relatedLimit); it.model.back(); it.model.back() }
        snapshot("related-after-nested-back","Search")
        scenario.onActivity {
            assertEquals(originalRelatedRoute,it.model.route)
            assertEquals("Powers",it.model.relatedHeading); assertEquals(20,it.model.relatedLimit)
            assertEquals(spider.related("powers").map { item -> item.id },it.model.relatedItems.map { item -> item.id })
        }
        scenario.recreate(); scenario.onActivity { activity=it }
        waitFor { activity.model.route == originalRelatedRoute }
        snapshot("related-after-recreation","Search")
        scenario.onActivity { assertEquals("Powers",it.model.relatedHeading); assertEquals(20,it.model.relatedLimit) }
        // An invalid provider identifier deliberately fails the repository refresh without network timing.
        assertNotNull("Live first-appearance issue is available for retention checks",firstIssue)
        val invalidHero=ArchiveItem.fromJson(JSONObject(spider.raw).put("id",-1),"character")
        scenario.onActivity {
            it.model.destination("archives"); it.model.timelineCharacter=invalidHero
            it.model.timeline=Remote(listOf(firstIssue!!),error="Some dates could not be loaded. Retry to complete this partial timeline.")
            it.model.navigate("timeline")
        }
        instrumentation.waitForIdleSync(); Thread.sleep(350)
        scenario.onActivity {
            val retry=descendants(it.binding.screenHost).filterIsInstance<Button>().single { b -> b.id == R.id.state_retry && b.visibility == View.VISIBLE }
            assertTrue(retry.performClick())
            assertEquals(listOf(firstIssue!!.id),it.model.timeline.value!!.map { record -> record.id })
        }
        waitFor { activity.model.timeline.error != null && !activity.model.timeline.loading }
        snapshot("timeline-refresh-error","Archives")
        scenario.onActivity { assertEquals(listOf(firstIssue!!.id),it.model.timeline.value!!.map { record -> record.id }) }
        scenario.onActivity {
            val retry=descendants(it.binding.screenHost).filterIsInstance<Button>().single { b -> b.id == R.id.state_retry && b.visibility == View.VISIBLE }
            assertTrue(retry.performClick())
        }
        waitFor { activity.model.timeline.error != null && !activity.model.timeline.loading }
        snapshot("timeline-retry-error","Archives")
        scenario.onActivity { assertEquals(listOf(firstIssue!!.id),it.model.timeline.value!!.map { record -> record.id }) }
        scenario.close(); auth.signOut()
    }
    @Test fun captureScreenMap() = runBlocking {
        val auth = FirebaseAuth.getInstance()
        auth.useEmulator("10.0.2.2",9099)
        val db = FirebaseFirestore.getInstance()
        db.useEmulator("10.0.2.2",8080)
        db.disableNetwork().awaitResult()
        auth.signOut()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        waitFor { activity.model.route.screen == "welcome" }
        capture("welcome",scenario)
        scenario.onActivity { it.model.openAuth("email-sign-in") }
        capture("email-sign-in",scenario)
        scenario.onActivity { it.model.openAuth("sign-up") }
        capture("sign-up",scenario)
        scenario.onActivity { it.model.openAuth("password-reset"); it.model.authNotice = "If this email is registered, a reset link will arrive shortly."; it.model.notifyChanged() }
        capture("password-reset",scenario)
        scenario.onActivity { it.model.destination("sign-in"); it.model.authError = "Google sign-in did not complete. Try again or sign in with email."; it.model.notifyChanged() }
        capture("sign-in-error",scenario)
        val account = auth.signInAnonymously().awaitResult()
        account.user!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName("Archive Review").build()).awaitResult()
        waitFor { activity.model.user != null }
        val api = ComicVineRepository()
        val characters = page(api,"character","Spider-Man")
        val spider = record(api,"character",1443)
        val gambit = record(api,"character",1499)
        val snapshots = SavedTeam("design-review","Archive Field Team","cosmic",missions.first().description,listOf(spider,gambit),1_791_033_600_000)
        scenario.onActivity {
            val vm = it.model
            vm.collectionLoading = false; vm.persistenceError = null
            vm.favorites = listOf(spider,gambit); vm.recent = listOf(gambit,spider); vm.teams = listOf(snapshots); vm.history = listOf(snapshots)
            vm.details["character/1443"] = Remote(spider); vm.details["character/1499"] = Remote(gambit)
            vm.pages[vm.pageKey("character","Spider-Man")] = Remote(characters)
            vm.pages[vm.pageKey("character","")] = Remote(characters)
            vm.destination("home")
        }
        capture("home",scenario)
        fun route(name: String, kind: String = "", id: String = "") {
            scenario.onActivity { it.model.stack.clear(); it.model.stack += Route("home"); it.model.navigate(name,kind,id) }
        }
        route("splash"); capture("splash",scenario)
        route("first-run-profile"); capture("first-run-profile",scenario)
        route("profile"); capture("profile",scenario)
        route("settings"); capture("settings",scenario)
        route("search"); capture("search-start",scenario)
        route("global-search"); capture("global-search-start",scenario)
        scenario.onActivity { it.model.queries["search"]="Spider-Man"; it.model.submittedQueries["search"]="Spider-Man" }
        route("search"); capture("search",scenario)
        route("global-search"); capture("global-search",scenario)
        route("detail","character","1443"); capture("character-detail",scenario)
        capture("character-detail-bottom",scenario,bottom=true)
        route("image","character","1443"); capture("image",scenario)
        route("recruit"); capture("recruit",scenario)
        scenario.onActivity { it.model.startMission(missions.first()) }; capture("briefing",scenario)
        scenario.onActivity { it.model.roster.clear(); it.model.roster += listOf(spider,gambit); it.model.selectionPurpose="mission" }
        route("picker"); capture("picker",scenario)
        scenario.onActivity { it.model.selectionPurpose="compareA" }
        route("character-select"); capture("character-select",scenario)
        scenario.onActivity { it.model.selectionPurpose="mission" }
        route("assembly"); capture("assembly",scenario)
        capture("assembly-bottom",scenario,bottom=true)
        scenario.onActivity { it.model.report=snapshots }
        route("report"); capture("report",scenario)
        capture("report-bottom",scenario,bottom=true)
        route("saved-team",id=snapshots.id); capture("saved-team",scenario)
        route("collection"); capture("collection",scenario)
        for(name in listOf("favorites","saved-teams","mission-history","recent")) { route(name); capture(name,scenario) }
        scenario.onActivity { it.model.compareA=spider; it.model.compareB=gambit }
        route("compare"); capture("compare",scenario)
        route("compare-result"); capture("compare-result",scenario)
        val firstIssue = spider.reference("first_appeared_in_issue")?.let { record(api,"issue",it.id) }
        scenario.onActivity { it.model.timelineCharacter=spider; it.model.timeline=Remote(listOfNotNull(firstIssue)) }
        route("timeline"); capture("timeline",scenario)
        scenario.onActivity { it.model.relatedHeading="Powers"; it.model.relatedItems=spider.related("powers") }
        route("related"); capture("related",scenario)
        route("archives"); capture("archives",scenario)
        for(kind in listOf("power","team","story_arc","issue","volume","publisher","location")) {
            val records = page(api,kind)
            scenario.onActivity { it.model.pages[it.model.pageKey(kind,"")]=Remote(records) }
            route("archive-list",kind); capture("$kind-list",scenario)
            records.items.firstOrNull()?.let {
                val full = record(api,kind,it.id)
                scenario.onActivity { a -> a.model.details["$kind/${full.id}"]=Remote(full) }
                route("detail",kind,full.id.toString()); capture("$kind-detail",scenario)
            }
        }
        scenario.onActivity { it.model.submittedQueries["search"]="review-empty"; it.model.pages[it.model.pageKey("character","review-empty")]=Remote(Page(emptyList(),0,0,0)) }
        route("search"); capture("search-empty",scenario)
        scenario.onActivity { it.model.pages[it.model.pageKey("character","review-empty")]=Remote(error="Cannot reach Comic Vine. Check your connection and retry."); it.model.notifyChanged() }
        capture("search-error",scenario)
        scenario.onActivity { it.model.pages[it.model.pageKey("character","review-empty")]=Remote(loading=true); it.model.notifyChanged() }
        capture("search-loading",scenario)
        scenario.onActivity { it.model.favorites=emptyList(); it.model.teams=emptyList(); it.model.history=emptyList(); it.model.recent=emptyList(); it.model.collectionLoading=false; it.model.destination("collection") }
        capture("collection-empty",scenario)
        route("home"); capture("home-empty",scenario)

        // Negative image fixtures change only the URL of a real provider dossier.
        fun imageFixture(url: String) {
            scenario.onActivity { it.model.details["character/1443"] = Remote(spider.copy(image=url)) }
            route("image","character","1443")
        }
        imageFixture("https://192.0.2.1/image.png")
        capture("image-loading",scenario)
        scenario.onActivity { assertTrue(descendants(it.binding.root).filterIsInstance<TextView>().any { v -> v.text.toString() == "Loading image…" && v.visibility == View.VISIBLE }) }
        imageFixture("https://127.0.0.1:9/image.png")
        fun waitForImageFailure() = waitFor {
            var visible = false
            scenario.onActivity { a -> visible = descendants(a.binding.root).filterIsInstance<Button>().any { it.text.toString() == "Retry image" && it.visibility == View.VISIBLE } }
            visible
        }
        waitForImageFailure(); capture("image-error",scenario)
        scenario.onActivity { a ->
            descendants(a.binding.root).filterIsInstance<Button>().first { it.text.toString() == "Retry image" }.performClick()
            assertTrue(descendants(a.binding.root).filterIsInstance<TextView>().any { it.text.toString() == "Loading image…" && it.visibility == View.VISIBLE })
        }
        waitForImageFailure(); capture("image-retry-error",scenario)
        imageFixture(""); capture("image-missing",scenario)
        scenario.onActivity { it.model.details["character/1443"] = Remote(spider) }

        // Choose the real Search filter, then submit an intentionally empty selector result.
        route("search")
        scenario.onActivity { a -> descendants(a.binding.root).filterIsInstance<Button>().first { it.text.toString() == "Marvel" }.performClick(); assertTrue(a.model.marvelOnly) }
        scenario.onActivity { a ->
            a.model.selectionPurpose="compareA"
            a.model.submittedQueries["picker_compareA"]="review-selector-empty"
            a.model.pages[a.model.pageKey("character","review-selector-empty")]=Remote(Page(emptyList(),0,0,0))
        }
        route("character-select"); capture("character-select-empty",scenario)
        capture("character-select-empty-bottom",scenario,bottom=true)
        scenario.onActivity { a ->
            val copy = descendants(a.binding.root).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(copy.contains("Try another name or browse the archive."))
            assertFalse(copy.any { it.contains("choose All") })
        }

        // Enter an objective without recruits, resume it, keep it, then explicitly replace it.
        scenario.onActivity { it.model.startMission(missions.first { m -> m.id == "custom" }) }
        instrumentation.waitForIdleSync(); Thread.sleep(350)
        val objective = "Protect the observatory and evacuate its researchers."
        scenario.onActivity { a -> descendants(a.binding.root).filterIsInstance<EditText>().first { it.hint.toString() == "Your mission briefing" }.setText(objective) }
        capture("briefing-custom-objective",scenario,bottom=true)
        scenario.onActivity { a -> descendants(a.binding.root).filterIsInstance<Button>().first { it.text.toString() == "Begin Recruitment" }.performClick() }
        scenario.onActivity { assertEquals("picker",it.model.route.screen); assertTrue(it.model.roster.isEmpty()); assertEquals(objective,it.model.customBriefing); it.model.destination("recruit") }
        capture("recruit-custom-draft",scenario)
        scenario.onActivity { a -> descendants(a.binding.root).filterIsInstance<Button>().first { it.text.toString() == "Continue Recruitment" }.performClick(); assertEquals("picker",a.model.route.screen); a.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { a -> assertEquals("recruit",a.model.route.screen); assertEquals(objective,a.model.customBriefing); (a.binding.root.findViewById<TextView>(R.id.mission_action).parent as View).performClick() }
        capture("custom-replacement-dialog",scenario)
        clickSystemAction("Keep current team")
        scenario.onActivity { a -> assertEquals("recruit",a.model.route.screen); assertEquals(objective,a.model.customBriefing); (a.binding.root.findViewById<TextView>(R.id.mission_action).parent as View).performClick() }
        clickSystemAction("Start new mission")
        scenario.onActivity { a ->
            assertEquals("briefing",a.model.route.screen); assertEquals("",a.model.customBriefing)
            assertTrue(a.model.roster.isEmpty()); assertEquals("",a.model.teamName)
            a.model.startMission(missions.first { it.id == "custom" })
        }
        capture("briefing-custom-reset",scenario,bottom=true)
        scenario.onActivity { it.signOut() }
        scenario.close()
    }
}
