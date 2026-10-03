package com.example.marvel

import android.util.Base64
import android.widget.CheckBox
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Emulator-only JWTs; never invokes Credential Manager, Google accounts or a production endpoint. */
@RunWith(AndroidJUnit4::class)
class GoogleAuthProtocolTest {
    private fun waitFor(check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 45_000
        while (System.currentTimeMillis() < deadline) {
            var settled = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { settled = check() }
            if (settled) return
            Thread.sleep(100)
        }
        fail("Google auth protocol did not settle")
    }

    // Mock OIDC credentials are supported only by the configured Auth emulator:
    // https://firebase.google.com/docs/emulator-suite/connect_auth#non-interactive_testing
    private fun testToken(subject: String): String {
        fun encoded(value: JSONObject) = Base64.encodeToString(value.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val now = System.currentTimeMillis() / 1000
        val header = JSONObject().put("alg", "none").put("typ", "JWT")
        val claims = JSONObject().put("iss", "https://accounts.google.com")
            .put("aud", "emulator-only-google-client").put("sub", subject)
            .put("email", "$subject@example.test").put("email_verified", true)
            .put("name", "Protocol Test Agent").put("iat", now).put("exp", now + 3600)
        return "${encoded(header)}.${encoded(claims)}."
    }

    @Test fun abandonedAndSupersededGoogleAttemptsDoNotPublishSessions() {
        val auth = FirebaseAuth.getInstance().apply { useEmulator("10.0.2.2", 9099); signOut() }
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        try {
            waitFor { activity.model.route.screen == "welcome" }
            val abandonedToken = testToken("cancelled-${UUID.randomUUID()}")
            scenario.onActivity {
                it.model.navigate("sign-in")
                val attempt = it.model.beginGoogleAuth()!!
                it.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { it.model.authenticate(abandonedToken, attempt) }
                assertTrue(it.model.authBusy)
                assertTrue(it.model.back())
                assertEquals("welcome", it.model.route.screen)
            }
            waitFor { !activity.model.authBusy }
            assertNull(auth.currentUser)
            assertNull(activity.model.user)
            assertEquals("welcome", activity.model.route.screen)

            scenario.onActivity {
                it.model.navigate("sign-in")
                val attempt = it.model.beginGoogleAuth()!!
                it.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { it.model.authenticate(testToken("timeout-${UUID.randomUUID()}"), attempt) }
                it.model.finishGoogleAuth(attempt, "Sign-in timed out. Check your connection and retry.")
                assertTrue(it.model.authBusy)
                assertTrue(it.model.authError!!.contains("Back"))
                assertNull(it.model.beginGoogleAuth())
            }
            waitFor { !activity.model.authBusy }
            assertNull(auth.currentUser)
            assertNull(activity.model.user)

            val acceptedToken = testToken("accepted-${UUID.randomUUID()}")
            var oldAttempt = 0
            scenario.onActivity {
                oldAttempt = it.model.beginGoogleAuth()!!
                it.model.back()
                it.model.navigate("sign-in")
                val current = it.model.beginGoogleAuth()!!
                it.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { it.model.authenticate(abandonedToken, oldAttempt) }
                it.model.finishGoogleAuth(oldAttempt, "Stale error")
                assertTrue(it.model.isAuthAttemptCurrent(current))
                assertNull(it.model.authError)
                it.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { it.model.authenticate(acceptedToken, current) }
            }
            waitFor { !activity.model.authBusy && activity.model.user != null }
            assertEquals("first-run-profile", activity.model.route.screen)
            val acceptedUid = auth.currentUser!!.uid
            scenario.onActivity { it.model.finishGoogleAuth(oldAttempt, "Stale finally") }
            assertEquals(acceptedUid, auth.currentUser!!.uid)
            assertEquals(acceptedUid, activity.model.user!!.uid)
            assertNull(activity.model.authError)

            scenario.onActivity { it.model.signOut() }
            waitFor { activity.model.user == null }
            scenario.onActivity {
                it.model.navigate("sign-in")
                val attempt = it.model.beginGoogleAuth()!!
                it.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { it.model.authenticate(acceptedToken, attempt) }
            }
            waitFor { !activity.model.authBusy && activity.model.user != null }
            assertEquals("home", activity.model.route.screen)
            assertEquals(acceptedUid, activity.model.user!!.uid)
            scenario.onActivity { it.model.signOut() }
            waitFor { activity.model.user == null }
        } finally { scenario.close() }
    }

    @Test fun passwordVisibilitySurvivesValidationAndRotationAndClearsOnBack() {
        FirebaseAuth.getInstance().apply { useEmulator("10.0.2.2", 9099); signOut() }
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        try {
            waitFor { activity.model.route.screen == "welcome" }
            scenario.onActivity { it.model.openAuth("email-sign-in") }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                it.findViewById<EditText>(R.id.auth_email).setText("invalid")
                it.findViewById<EditText>(R.id.auth_password).setText("memory-only-test-password")
                it.findViewById<CheckBox>(R.id.auth_show_password).isChecked = true
                it.model.submitEmailAuth()
            }
            waitFor { activity.model.authError != null }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                assertTrue(it.findViewById<CheckBox>(R.id.auth_show_password).isChecked)
                assertNull(it.findViewById<EditText>(R.id.auth_password).transformationMethod)
            }
            scenario.recreate()
            scenario.onActivity { activity = it }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                assertTrue(it.model.authShowPasswords)
                assertTrue(it.findViewById<CheckBox>(R.id.auth_show_password).isChecked)
                assertEquals("memory-only-test-password", it.findViewById<EditText>(R.id.auth_password).text.toString())
                assertNull(it.findViewById<EditText>(R.id.auth_password).transformationMethod)
                it.model.back()
                assertFalse(it.model.authShowPasswords)
                assertEquals("", it.model.authPassword)
                assertEquals("", it.model.authConfirmPassword)
            }
        } finally { scenario.close() }
    }
}
