package com.example.marvel

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marvel.data.ArchiveItem
import com.example.marvel.data.awaitResult
import com.example.marvel.data.missions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real recorded API dossiers; authentication/storage isolated to local emulators.
 * The ViewModel's fresh repository starts real asynchronous requests, then the user leaves/edits.
 */
@RunWith(AndroidJUnit4::class)
class ReportGenerationTest {
    @Test fun abandonedReportsNeverRestoreAnOldDraftOrNavigate() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.filesDir, "design-review")
        fun record(id: Int) = ArchiveItem.fromJson(JSONObject(File(folder,"record-character-$id.json").readText()),"character")
        val spider = record(1443); val gambit = record(1499)
        val auth = FirebaseAuth.getInstance().apply { useEmulator("10.0.2.2",9099); signOut() }
        FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2",8080); disableNetwork().awaitResult() }
        auth.signInAnonymously().awaitResult()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        val deadline = System.currentTimeMillis()+10_000
        while(activity.model.user == null && System.currentTimeMillis()<deadline) Thread.sleep(50)
        assertNotNull(activity.model.user)
        scenario.onActivity {
            val vm = it.model
            fun draft() {
                vm.startMission(missions.first()); vm.selectionPurpose="mission"
                vm.select(spider); vm.select(gambit); vm.teamName="Current team"
                vm.navigate("picker"); vm.navigate("assembly")
            }
            draft(); vm.generateReport(); assertTrue(vm.operationLoading)
            vm.back(); assertEquals("picker",vm.route.screen)
            assertFalse(vm.operationLoading); assertNull(vm.report)
            assertEquals(listOf(1443,1499),vm.roster.map { member -> member.id })
            vm.navigate("assembly"); vm.generateReport(); assertTrue(vm.operationLoading)
            vm.removeRecruit(spider)
            assertFalse(vm.operationLoading); assertEquals(listOf(1499),vm.roster.map { member -> member.id })
            vm.generateReport(); assertTrue(vm.operationLoading)
            vm.teamName="Edited team"
            assertFalse(vm.operationLoading); assertNull(vm.report)
            vm.generateReport(); assertTrue(vm.operationLoading)
            vm.startMission(missions.last())
            assertEquals("briefing",vm.route.screen); assertFalse(vm.operationLoading)
            assertTrue(vm.roster.isEmpty()); assertNull(vm.report)
        }
        Thread.sleep(2500)
        scenario.onActivity {
            assertEquals("briefing",it.model.route.screen)
            assertTrue(it.model.roster.isEmpty()); assertNull(it.model.report)
            assertFalse(it.model.operationLoading); it.signOut()
        }
        scenario.close()
    }
}
