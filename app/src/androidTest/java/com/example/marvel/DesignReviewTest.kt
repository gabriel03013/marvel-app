package com.example.marvel

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private val folder get() = File(instrumentation.targetContext.getExternalFilesDir(null), "design-review").apply { mkdirs() }
    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 30_000
        while(!condition() && System.currentTimeMillis() < until) Thread.sleep(100)
        assertTrue("Review session should initialize", condition())
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
                if(bottom) scroll.fullScroll(View.FOCUS_DOWN) else scroll.scrollTo(0,0)
            }
        }
        instrumentation.waitForIdleSync()
        val device = InstrumentationRegistry.getArguments().getString("captureClass","phone")
        scenario.onActivity { activity ->
            val scroll = activity.binding.screenHost.findViewById<android.widget.ScrollView>(R.id.page_scroll)
            if(!bottom && scroll != null) assertEquals("First viewport starts at top: $name",0,scroll.scrollY)
            File(folder,"$name-$device-state.json").writeText(JSONObject()
                .put("route",activity.model.route.screen).put("kind",activity.model.route.kind)
                .put("scrollY",scroll?.scrollY ?: 0).put("fontScale",activity.resources.configuration.fontScale)
                .put("screenWidthDp",activity.resources.configuration.screenWidthDp).put("bottom",bottom).toString())
        }
        val shot = instrumentation.uiAutomation.takeScreenshot()
        File(folder,"$name-$device.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }
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
        scenario.onActivity { it.signOut() }
        scenario.close()
    }
}
