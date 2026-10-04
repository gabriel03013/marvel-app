package com.example.marvel

import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.marvel.databinding.ActivityMainBinding
import com.example.marvel.ui.ArchiveViewModel
import com.example.marvel.ui.Route
import com.example.marvel.ui.ScreenRenderer
import com.example.marvel.data.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainActivity : ComponentActivity() {
    val model: ArchiveViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    private lateinit var renderer: ScreenRenderer
    private var lastRoute: Route? = null
    private var displayedMessage: String? = null
    private var archiveReady = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom)); insets
        }
        initializeArchive(savedInstanceState)
    }
    private fun initializeArchive(savedInstanceState: Bundle?) {
        try {
            com.google.firebase.FirebaseApp.initializeApp(this) ?: com.google.firebase.FirebaseApp.getInstance()
            com.google.firebase.auth.FirebaseAuth.getInstance()
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
            renderer = ScreenRenderer(this, model)
        } catch (error: Exception) {
            android.util.Log.e("ArchiveStartup", "Archive initialization failed: ${error.javaClass.simpleName}")
            showInitializationFailure { initializeArchive(savedInstanceState) }
            return
        }
        archiveReady = true
        savedInstanceState?.getStringArrayList("routes")?.map { encoded -> encoded.split("|", limit = 3).let { Route(it[0], it.getOrElse(1){""}, it.getOrElse(2){""}) } }?.let { model.restoreRoutes(it, savedInstanceState.getString("route_uid").orEmpty()) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (!model.back()) { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true } }
        })
        binding.backButton.setOnClickListener { model.back() }
        binding.globalSearchButton.setOnClickListener { if (model.route.screen != "global-search") model.navigate("global-search") }
        binding.message.setOnClickListener { model.message = null; binding.message.visibility = View.GONE }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { model.changes.collect { render() } } }
    }
    private fun showInitializationFailure(retry: () -> Unit) {
        binding.topBar.visibility = View.GONE
        binding.bottomNav.visibility = View.GONE
        binding.message.visibility = View.GONE
        binding.screenHost.removeAllViews()
        val page = layoutInflater.inflate(R.layout.screen_dark, binding.screenHost, false)
        binding.screenHost.addView(page)
        val content = page.findViewById<LinearLayout>(R.id.page_content)
        content.gravity = android.view.Gravity.CENTER_VERTICAL
        val cover = layoutInflater.inflate(R.layout.block_cover, content, false)
        cover.background = null
        cover.findViewById<TextView>(R.id.cover_title).apply {
            text = "S.H.I.E.L.D.\nArchives"; setTextColor(getColor(R.color.ink_black)); isAccessibilityHeading = true
        }
        content.addView(cover)
        val state = layoutInflater.inflate(R.layout.block_state, content, false)
        state.background = com.example.marvel.ui.CollageSurface(this, "paper", 1)
        state.findViewById<TextView>(R.id.state_title).text = "The archive could not open"
        state.findViewById<TextView>(R.id.state_copy).text = "We could not prepare your account service. Retry to open the archive."
        state.findViewById<Button>(R.id.state_retry).apply { visibility = View.VISIBLE; setOnClickListener { retry() } }
        content.addView(state)
    }
    private fun render() {
        val route = model.route
        val sameScreen = route == lastRoute
        val sameCover = route.screen in listOf("welcome", "sign-in") && lastRoute?.screen in listOf("welcome", "sign-in")
        val focused = if(sameScreen) currentFocus as? EditText else null
        val focusTag = focused?.tag
        val cursor = focused?.selectionStart ?: 0
        val scroll = if (sameScreen || sameCover) binding.screenHost.findViewById<ScrollView>(R.id.page_scroll)?.scrollY ?: 0 else 0
        lastRoute = route
        val public = model.user == null || route.screen in listOf("welcome", "splash", "sign-in")
        binding.topBar.visibility = if (public) View.GONE else View.VISIBLE
        binding.bottomNav.visibility = if (public) View.GONE else View.VISIBLE
        binding.backButton.visibility = if (model.stack.size > 1 || route.screen !in listOf("home", "search", "recruit", "archives", "collection")) View.VISIBLE else View.INVISIBLE
        binding.topTitle.text = when(route.screen) { "detail" -> "HERO & ARCHIVE DOSSIER"; "report", "saved-team" -> "MISSION REPORT"; else -> "S.H.I.E.L.D. ARCHIVES" }
        renderer.renderNavigation(binding.bottomNav)
        renderer.render(binding.screenHost)
        if(!sameScreen) { binding.screenHost.isFocusableInTouchMode = true; binding.screenHost.requestFocus() }
        if(!sameScreen && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            binding.screenHost.alpha = 0.88f
            binding.screenHost.animate().alpha(1f).setDuration(180).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        }
        if(focusTag != null) binding.screenHost.findViewWithTag<EditText>(focusTag)?.let { field -> field.requestFocus(); field.setSelection(cursor.coerceIn(0, field.text.length)) }
        if (sameScreen || sameCover) binding.screenHost.findViewById<ScrollView>(R.id.page_scroll)?.post { binding.screenHost.findViewById<ScrollView>(R.id.page_scroll)?.scrollTo(0, scroll) }
        if(model.message != null && model.message != displayedMessage) {
            displayedMessage = model.message
            val expected = model.message
            binding.message.postDelayed({ if(model.message == expected) { model.message = null; displayedMessage = null; binding.message.visibility = View.GONE } }, 5000)
        }
        binding.message.contentDescription = "${model.message.orEmpty()}. Tap to dismiss."
        binding.message.text = model.message
        binding.message.visibility = if (model.message.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    fun signIn() {
        val attempt = model.beginGoogleAuth() ?: return
        lifecycleScope.launch {
            try {
                val id = resources.getIdentifier("default_web_client_id", "string", packageName)
                if (id == 0 || getString(id).isBlank()) {
                    android.util.Log.w("ArchiveAuth", "Google sign-in client configuration is missing")
                    error("Google sign-in unavailable")
                }
                val option = GetSignInWithGoogleOption.Builder(getString(id)).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(this@MainActivity).getCredential(MutableContextWrapper(this@MainActivity), request)
                if (!model.isAuthAttemptCurrent(attempt)) return@launch
                val credential = result.credential
                check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Unsupported Google credential type" }
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                withTimeout(30_000) { model.authenticate(token, attempt) }
            } catch (e: Exception) {
                if (e is CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                android.util.Log.w("ArchiveAuth", "Google sign-in failed: ${(e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
                val message = when {
                    e is GetCredentialCancellationException -> "Google sign-in did not complete. Try again or sign in with email."
                    e is NoCredentialException -> "No Google account is available. Add a Google account on this device and retry."
                    e is com.google.firebase.FirebaseException -> authErrorMessage(e)
                    e is androidx.credentials.exceptions.GetCredentialProviderConfigurationException -> "Google sign-in is unavailable on this device. Update Google Play services or sign in with email."
                    e is kotlinx.coroutines.TimeoutCancellationException -> "Sign-in timed out. Check your connection and retry."
                    else -> "Google sign-in is unavailable. Try again or sign in with email."
                }
                model.finishGoogleAuth(attempt, message)
            } finally { model.finishGoogleAuth(attempt) }
        }
    }
    fun signOut() {
        model.signOut()
        lifecycleScope.launch { runCatching { CredentialManager.create(this@MainActivity).clearCredentialState(ClearCredentialStateRequest()) } }
    }
    fun hideKeyboard() { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.root.windowToken, 0); currentFocus?.clearFocus(); binding.root.isFocusableInTouchMode = true; binding.root.requestFocus() }
    fun share(team: SavedTeam) {
        val template = missions.firstOrNull { it.id == team.missionId } ?: missions.last()
        val result = evaluate(team.members, template)
        val text = "${team.name}\n${template.title}\n${team.briefing}\n\n" + team.members.joinToString("\n") { "${it.name}: ${it.related("powers").joinToString { p -> p.name }.ifBlank { "Powers not documented" }}" } + "\n\nApp-generated evaluation: ${result.score}/100. Based on roster coverage, documented power diversity and mission fit; not official Marvel statistics. Data: Comic Vine."
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share team report"))
    }
    override fun onStop() { if(archiveReady) model.persistDraft(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        if(archiveReady) {
            model.persistDraft()
            outState.putString("route_uid", model.user?.uid)
            outState.putStringArrayList("routes", ArrayList(model.stack.map { "${it.screen}|${it.kind}|${it.id}" }))
        }
        super.onSaveInstanceState(outState)
    }
}
