package com.example.marvel

import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marvel.data.awaitResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnonymousAuthFlowTest {
    private fun waitFor(check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 45_000
        while (System.currentTimeMillis() < deadline) {
            var settled = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { settled = check() }
            if (settled) return
            Thread.sleep(100)
        }
        fail("Anonymous authentication did not settle")
    }

    @Test fun guestCanEnterArchiveAndProfileIsPersisted() = runBlocking {
        val auth = FirebaseAuth.getInstance().apply { useEmulator("10.0.2.2", 9099); signOut() }
        val db = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        waitFor { activity.model.route.screen == "welcome" }

        scenario.onActivity { it.findViewById<Button>(R.id.anonymous_sign_in).performClick() }
        waitFor { activity.model.user?.isAnonymous == true && activity.model.route.screen == "home" && !activity.model.authBusy }
        val uid = auth.currentUser?.uid
        assertNotNull(uid)
        assertEquals(uid, activity.model.user?.uid)
        assertTrue(auth.currentUser!!.isAnonymous)
        var profile = db.collection("users").document(uid!!).get().awaitResult()
        val profileDeadline = System.currentTimeMillis() + 20_000
        while (!profile.exists() && System.currentTimeMillis() < profileDeadline) {
            Thread.sleep(300)
            profile = db.collection("users").document(uid).get().awaitResult()
        }
        assertTrue("Anonymous account profile should be saved under the Firebase UID", profile.exists())
        assertEquals("Agent", profile.getString("displayName"))

        scenario.onActivity { it.model.navigate("profile") }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        var guestLabelVisible = false
        scenario.onActivity { screen ->
            val root = screen.binding.screenHost
            fun visit(view: android.view.View) {
                if (view is android.widget.TextView && view.text.toString() == "Guest session") guestLabelVisible = true
                if (view is android.view.ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
            }
            visit(root)
        }
        assertTrue("Profile should identify anonymous accounts as guest sessions", guestLabelVisible)
        scenario.close()
    }
}
