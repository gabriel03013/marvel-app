package com.example.marvel

import android.graphics.Bitmap
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.marvel.data.*
import com.example.marvel.ui.Remote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Uses real Comic Vine records and isolated Firebase emulators, never a production test account. */
@RunWith(AndroidJUnit4::class)
class ArchiveFlowTest {
    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 45_000
        while(System.currentTimeMillis() < deadline) { if(condition()) return; Thread.sleep(100) }
        fail("Timed out waiting for archive state")
    }
    private fun capture(name: String) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(700)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "review").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(folder, "${name.removeSuffix("-phone")}-${InstrumentationRegistry.getArguments().getString("captureClass", "phone")}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun realDiscoveryRecruitmentPersistenceAndNavigation() = runBlocking {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        auth.useEmulator("10.0.2.2", 9099)
        db.useEmulator("10.0.2.2", 8080)
        auth.signOut()
        val credential = auth.signInAnonymously().awaitResult()
        credential.user!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName("Archive Review").build()).awaitResult()
        val api = ComicVineRepository()
        val page = api.list("character", "Spider-Man")
        assertTrue("Search must use the singular Comic Vine resource identifier", page.items.any { it.id == 1443 })
        assertTrue(page.hasMore)
        val spider = api.detail("character", 1443)
        val gambit = api.detail("character", 1499)
        assertEquals("Spider-Man", spider.name)
        assertTrue(spider.related("powers").isNotEmpty())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        waitFor { activity.model.user != null && !activity.model.collectionLoading }
        capture("home-phone")
        scenario.onActivity {
            val vm = it.model
            vm.queries["search"] = "Spider-Man"; vm.submittedQueries["search"] = "Spider-Man"
            vm.pages[vm.pageKey("character", "Spider-Man")] = Remote(page)
            vm.destination("search")
        }
        capture("search-phone")
        scenario.onActivity { it.model.details["character/1443"] = Remote(spider); it.model.navigate("detail", "character", "1443") }
        capture("detail-phone")
        scenario.onActivity { it.model.favorite(spider) }
        waitFor { activity.model.favorites.any { it.id == spider.id } && !activity.model.operationLoading }
        assertTrue(db.collection("users").document(credential.user!!.uid).collection("favorites").document("1443").get().awaitResult().exists())
        scenario.onActivity { assertTrue(it.model.back()); assertEquals("search", it.model.route.screen); it.model.startMission(missions.first()) }
        capture("briefing-phone")
        scenario.onActivity { it.model.selectionPurpose = "mission"; it.model.select(spider); it.model.select(gambit); it.model.navigate("picker") }
        capture("picker-phone")
        scenario.onActivity { it.model.teamName = "Archive Field Team"; it.model.navigate("assembly") }
        capture("assembly-phone")
        scenario.onActivity { it.model.generateReport() }
        waitFor { activity.model.route.screen == "report" && !activity.model.operationLoading }
        val report = activity.model.report!!
        assertEquals(2, report.members.size)
        assertEquals(2, evaluate(report.members, missions.first()).knownMembers)
        capture("report-phone")
        scenario.onActivity { it.model.saveReport() }
        waitFor { activity.model.teams.any { it.id == report.id } && activity.model.history.any { it.id == report.id } && !activity.model.operationLoading }
        val root = db.collection("users").document(credential.user!!.uid)
        assertTrue(root.get().awaitResult().exists())
        assertTrue(root.collection("teams").document(report.id).get().awaitResult().exists())
        assertTrue(root.collection("missions").document(report.id).get().awaitResult().exists())
        scenario.onActivity { it.model.collectionTab = "Saved Teams"; it.model.destination("collection") }
        capture("collection-phone")
        scenario.onActivity { it.model.compareA = spider; it.model.compareB = gambit; it.model.details["character/${gambit.id}"] = Remote(gambit); it.model.navigate("compare-result") }
        capture("compare-phone")
        scenario.recreate()
        scenario.onActivity { activity = it; assertEquals("compare-result", it.model.route.screen); assertEquals(spider.id, it.model.compareA?.id) }
        scenario.onActivity { it.model.timelineCharacter = spider; it.model.timeline = Remote(); it.model.navigate("timeline"); it.model.loadTimeline() }
        waitFor { !activity.model.timeline.loading }
        assertTrue(activity.model.timeline.value.orEmpty().isNotEmpty())
        capture("timeline-phone")
        scenario.onActivity { it.model.destination("archives") }
        capture("archives-phone")
        scenario.onActivity { it.model.navigate("archive-list", "power") }
        waitFor { activity.model.pages[activity.model.pageKey("power", "")]?.value != null || activity.model.pages[activity.model.pageKey("power", "")]?.error != null }
        assertNotNull(activity.model.pages[activity.model.pageKey("power", "")]?.value)
        capture("powers-phone")
        scenario.onActivity { it.model.navigate("detail", "power", "1") }
        waitFor { activity.model.details["power/1"]?.value != null || activity.model.details["power/1"]?.error != null }
        assertNotNull(activity.model.details["power/1"]?.value)
        capture("power-detail-phone")
        scenario.onActivity { it.model.destination("profile") }
        capture("profile-phone")
        scenario.onActivity { it.model.navigate("settings") }
        capture("settings-phone")
        scenario.onActivity { it.model.deleteTeam(report) }
        waitFor { activity.model.teams.none { it.id == report.id } }
        assertTrue(root.collection("missions").document(report.id).get().awaitResult().exists())
        scenario.onActivity {
            val vm = it.model
            vm.pages[vm.pageKey("character", "no-match")] = Remote(Page(emptyList(), 0, 0, 0))
            vm.submittedQueries["search"] = "no-match"; vm.queries["search"] = "no-match"; vm.destination("search")
        }
        capture("empty-search-phone")
        scenario.onActivity {
            val vm = it.model
            vm.pages[vm.pageKey("character", "no-match")] = Remote(error = "Cannot reach Comic Vine. Check your connection and retry.")
            vm.notifyChanged()
        }
        capture("error-search-phone")
        scenario.onActivity { it.signOut() }
        waitFor { activity.model.user == null && activity.model.route.screen == "welcome" }
        capture("welcome-phone")
        scenario.close()
        auth.signInAnonymously().awaitResult()
        try { root.collection("favorites").document("1443").get(com.google.firebase.firestore.Source.SERVER).awaitResult(); fail("Other users must not read a private collection") }
        catch(e: com.google.firebase.firestore.FirebaseFirestoreException) { assertEquals(com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
        auth.signOut()
    }
    @Test fun adaptiveLayoutsAndFontScale() = runBlocking {
        val auth = FirebaseAuth.getInstance(); val db = FirebaseFirestore.getInstance()
        auth.useEmulator("10.0.2.2", 9099); db.useEmulator("10.0.2.2", 8080)
        auth.signOut(); val account = auth.signInAnonymously().awaitResult()
        account.user!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName("Alexandra Archive Reviewer").build()).awaitResult()
        val api = ComicVineRepository(); val page = api.list("character", "Spider-Man"); val spider = api.detail("character", 1443)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity; scenario.onActivity { activity = it }
        waitFor { activity.model.user != null && !activity.model.collectionLoading }
        capture("home-phone")
        scenario.onActivity {
            it.model.pages[it.model.pageKey("character", "Spider-Man")] = Remote(page)
            it.model.submittedQueries["search"] = "Spider-Man"; it.model.queries["search"] = "Spider-Man"
            it.model.destination("search")
        }
        capture("search-phone")
        scenario.onActivity { it.model.details["character/1443"] = Remote(spider); it.model.navigate("detail", "character", "1443") }
        capture("detail-phone")
        scenario.onActivity { it.model.startMission(missions.first()) }
        capture("briefing-phone")
        scenario.onActivity { it.model.selectionPurpose = "mission"; it.model.select(spider); it.model.navigate("picker") }
        capture("picker-phone")
        scenario.onActivity { it.model.teamName = "A team name with room to grow"; it.model.navigate("assembly") }
        capture("assembly-phone")
        scenario.onActivity { it.model.report = SavedTeam("review", "A team name with room to grow", "cosmic", missions.first().description, listOf(spider), System.currentTimeMillis()); it.model.navigate("report") }
        capture("report-phone")
        scenario.onActivity { it.model.destination("collection") }
        capture("collection-phone")
        scenario.onActivity { it.signOut() }; waitFor { activity.model.user == null }
        capture("welcome-phone")
        scenario.close()
    }

    @Test fun reviewFixes() = runBlocking {
        val auth = FirebaseAuth.getInstance(); val db = FirebaseFirestore.getInstance()
        auth.useEmulator("10.0.2.2", 9099); db.useEmulator("10.0.2.2", 8080)
        auth.signOut(); auth.signInAnonymously().awaitResult()
        val api = ComicVineRepository(); val page = api.list("character", "Spider-Man"); val spider = api.detail("character", 1443)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity; scenario.onActivity { activity = it }
        waitFor { activity.model.user != null }
        scenario.onActivity {
            val vm = it.model; vm.pages[vm.pageKey("character", "Spider-Man")] = Remote(page)
            vm.queries["search"] = "Spider-Man"; vm.submittedQueries["search"] = "Spider-Man"; vm.destination("search")
        }
        capture("search-phone")
        scenario.onActivity { it.model.pages[it.model.pageKey("character", "Spider-Man")] = Remote(error = "Cannot reach Comic Vine. Check your connection and retry."); it.model.notifyChanged() }
        capture("error-search-phone")
        scenario.onActivity { it.binding.screenHost.findViewById<android.widget.Button>(R.id.state_retry).performClick() }
        waitFor { activity.model.pages[activity.model.pageKey("character", "Spider-Man")]?.value != null }
        scenario.onActivity {
            assertFalse(com.example.marvel.ui.ScreenRenderer(it, it.model).plain("<p>Overview<img src='image.png'></p>").contains('\uFFFC'))
            it.model.details["character/1443"] = Remote(spider); it.model.navigate("detail", "character", "1443")
        }
        capture("detail-phone")
        scenario.onActivity { it.signOut() }; scenario.close()
    }

}
