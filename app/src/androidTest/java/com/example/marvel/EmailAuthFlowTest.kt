package com.example.marvel

import android.graphics.Bitmap
import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marvel.data.awaitResult
import com.example.marvel.ui.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmailAuthFlowTest {
    private fun waitFor(check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 45_000
        while (System.currentTimeMillis() < deadline) {
            var settled = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { settled = check() }
            if (settled) return
            Thread.sleep(100)
        }
        fail("Authentication state did not settle")
    }

    private fun capture(name: String) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(400)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "review").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val size = InstrumentationRegistry.getArguments().getString("captureClass", "phone")
        File(folder, "auth-$name-$size.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun signupSigninRecoveryAndValidation() = runBlocking {
        val auth =
            FirebaseAuth.getInstance().apply {
                useEmulator("10.0.2.2", 9099)
                signOut()
            }
        val db = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }
        val email = "agent-${System.currentTimeMillis()}@example.com"
        val password = "Archive-test-782!"
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        waitFor { activity.model.route.screen == "welcome" }
        capture("welcome")
        scenario.onActivity { it.findViewById<Button>(R.id.create_account).performClick() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        capture("signup")
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_name).setText("Email Agent")
            it.findViewById<EditText>(R.id.auth_email).setText("invalid")
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { activity.model.authError == "Enter a valid email address." }
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_email).setText(email)
            it.findViewById<EditText>(R.id.auth_password).setText(password)
            it.findViewById<EditText>(R.id.auth_confirm).setText("mismatch")
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { activity.model.authError == "Passwords do not match." }
        capture("validation")
        scenario.recreate()
        scenario.onActivity {
            activity = it
            assertEquals(password, it.findViewById<EditText>(R.id.auth_password).text.toString())
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_confirm).setText(password)
            assertEquals("sign-up", it.model.route.screen)
            assertEquals(password, it.model.authConfirmPassword)
            assertTrue(it.findViewById<Button>(R.id.auth_submit).isEnabled)
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { !activity.model.authLoading }
        assertEquals(
            "Signup error: ${activity.model.authError}",
            "first-run-profile",
            activity.model.route.screen,
        )
        assertEquals(
            "Profile update status: ${activity.model.message}",
            "Email Agent",
            auth.currentUser!!.displayName,
        )
        val uid = auth.currentUser!!.uid
        waitFor { !activity.model.collectionLoading }
        var profile = db.collection("users").document(uid).get().awaitResult()
        if (profile.getString("displayName") != "Email Agent") {
            Thread.sleep(1000)
            profile = db.collection("users").document(uid).get().awaitResult()
        }
        assertEquals("Email Agent", profile.getString("displayName"))
        assertEquals(email, profile.getString("email"))
        assertEquals("", activity.model.authPassword)
        capture("profile")
        scenario.onActivity { it.signOut() }
        waitFor { activity.model.user == null }
        scenario.onActivity { it.model.openAuth("email-sign-in") }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_email).setText(email)
            it.findViewById<EditText>(R.id.auth_password).setText("incorrect-password")
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { !activity.model.authLoading && activity.model.authError != null }
        assertTrue(
            activity.model.authError,
            activity.model.authError!!.contains("Email or password is incorrect"),
        )
        capture("signin-error")
        scenario.onActivity { it.findViewById<Button>(R.id.auth_reset).performClick() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { it.findViewById<Button>(R.id.auth_submit).performClick() }
        waitFor { !activity.model.authLoading && activity.model.authNotice != null }
        capture("reset")
        scenario.onActivity { it.findViewById<Button>(R.id.auth_alternative).performClick() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_password).setText(password)
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { !activity.model.authLoading && activity.model.user != null }
        assertEquals(uid, activity.model.user!!.uid)
        assertEquals("home", activity.model.route.screen)
        assertEquals("", activity.model.authPassword)
        scenario.onActivity { it.signOut() }
        waitFor { activity.model.user == null }
        scenario.onActivity { it.model.openAuth("email-sign-in") }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_email).setText(email)
            it.findViewById<EditText>(R.id.auth_password).setText(password)
            it.findViewById<Button>(R.id.auth_submit).performClick()
            assertTrue(it.model.authLoading)
            it.findViewById<Button>(R.id.auth_back).performClick()
            assertEquals("welcome", it.model.route.screen)
            assertEquals("", it.model.authPassword)
        }
        waitFor { !activity.model.authBusy }
        assertNull(auth.currentUser)
        assertEquals("welcome", activity.model.route.screen)
        scenario.onActivity { it.model.openAuth("email-sign-in") }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
            it.findViewById<EditText>(R.id.auth_password).setText(password)
            it.findViewById<Button>(R.id.auth_submit).performClick()
        }
        waitFor { !activity.model.authBusy && activity.model.user != null }
        assertEquals(uid, activity.model.user!!.uid)
        scenario.onActivity { it.signOut() }
        waitFor { activity.model.user == null }
        scenario.close()
    }
}
