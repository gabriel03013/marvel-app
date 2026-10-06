package com.example.marvel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class Route(val screen: String, val kind: String = "", val id: String = "")
data class Remote<T>(val value: T? = null, val loading: Boolean = false, val error: String? = null, val append: Boolean = false)
class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    val changes = MutableStateFlow(0)
    val api = ComicVineRepository()
    private val auth = FirebaseAuth.getInstance()
    private val users = UserRepository()
    var user: FirebaseUser? = null; private set
    val stack = mutableListOf(Route("splash"))
    val route get() = stack.last()
    // Kept only in memory so retries and rotation retain the form; never saved to disk.
    var authEmail = ""
    var authPassword = ""
    var authConfirmPassword = ""
    var authName = ""
    var authShowPasswords = false
    var authNotice: String? = null
    var authErrorSource = "google"
    private var deferAuthState = false
    private var authAttempt = 0
    private var pendingEmailAuth: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>? = null
    private var authTask: Pair<Int, com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>>? = null
    private val pendingAuthMessage = "This request is still finishing. Please wait, or use Back to leave this screen."
    val authBusy get() = authLoading || pendingEmailAuth != null
    var authLoading = false
    var authError: String? = null
    var persistenceError: String? = null
    private val cachedCollections = mutableSetOf<String>()
    private val pendingCollections = mutableSetOf<String>()
    val collectionCached get() = cachedCollections.isNotEmpty()
    val collectionPending get() = pendingCollections.isNotEmpty()
    var collectionLoading = true
    var favorites = emptyList<ArchiveItem>()
    var teams = emptyList<SavedTeam>()
    var history = emptyList<SavedTeam>()
    var recent = emptyList<ArchiveItem>()
    var searches = emptyList<String>()
    var collectionTab = "Favorites"
    var mission = missions.first()
    var customBriefing = ""
        set(value) { if(field != value) cancelReportGeneration(); field = value }
    val roster = mutableListOf<ArchiveItem>()
    var teamName = ""
        set(value) { if(field != value) cancelReportGeneration(); field = value }
    val hasRecruitmentDraft get() = roster.isNotEmpty() || teamName.isNotBlank() || (mission.id == "custom" && customBriefing.isNotBlank())
    var report: SavedTeam? = null
    var teamSuggestion: Remote<TeamSuggestion> = Remote()
    var graphExpanded = false
    var graphRecordKey = ""
    var simulationSource: SavedTeam? = null
    var simulationError: String? = null
    var revealedReportId: String? = null
    var compareA: ArchiveItem? = null
    var compareB: ArchiveItem? = null
    var investigateA: ArchiveItem? = null
    var investigateB: ArchiveItem? = null
    var selectionPurpose = "mission"
    var timelineCharacter: ArchiveItem? = null
    var relatedItems = emptyList<ArchiveItem>()
    var relatedHeading = "Related records"
    var relatedLimit = 20
    private data class RelatedSnapshot(val heading: String, val items: List<ArchiveItem>, val limit: Int)
    private val relatedSnapshots = mutableMapOf<String, RelatedSnapshot>()
    var timelineFilter = "All"
    var timeline = Remote<List<ArchiveItem>>()
    var message: String? = null
    var operationLoading = false
    private var reportJob: Job? = null
    private var reportAttempt = 0
    val details = mutableMapOf<String, Remote<ArchiveItem>>()
    val pages = mutableMapOf<String, Remote<Page>>()
    val queries = mutableMapOf<String, String>()
    val submittedQueries = mutableMapOf<String, String>()
    var marvelOnly = false
    var sort = "name:asc"
    private val listeners = mutableListOf<ListenerRegistration>()
    private val loadingCollections = mutableSetOf<String>()
    private val pendingFavoriteIds = mutableSetOf<Int>()
    private var sessionUid: String? = null
    private var pendingRestore: Pair<String, List<Route>>? = null
    private val prefs = application.getSharedPreferences("archive_history", 0)
    private val authListener = FirebaseAuth.AuthStateListener { firebase ->
        val next = firebase.currentUser
        if (!deferAuthState && pendingEmailAuth == null && (next?.uid != sessionUid || route.screen == "splash")) {
            cancelReportGeneration()
            user = next; sessionUid = next?.uid
            listeners.forEach { it.remove() }; listeners.clear()
            favorites = emptyList(); teams = emptyList(); history = emptyList(); recent = emptyList(); searches = emptyList()
            roster.clear(); report = null; compareA = null; compareB = null; investigateA = null; investigateB = null; timelineCharacter = null
            queries.clear(); submittedQueries.clear(); marvelOnly = false; selectionPurpose = "mission"; relatedItems = emptyList(); relatedSnapshots.clear(); timeline = Remote()
            persistenceError = null; cachedCollections.clear(); pendingCollections.clear()
            stack.clear(); stack += Route(if (next == null) "welcome" else "home")
            if (next != null) {
                loadLocal(next.uid); observeCollections(next.uid); saveProfile(next)
                pendingRestore?.takeIf { it.first == next.uid }?.let { (_, routes) -> stack.clear(); stack.addAll(routes); restoreRelatedContext() }; pendingRestore = null
            }
            notifyChanged()
        }
    }
    init { auth.addAuthStateListener(authListener) }
    fun notifyChanged() { changes.value++ }
    fun navigate(screen: String, kind: String = "", id: String = "") {
        cancelReportGeneration()
        saveRelatedContext()
        val next = Route(screen, kind, if(screen == "related" && id.isBlank()) UUID.randomUUID().toString() else id)
        if(screen == "related") relatedSnapshots[next.id] = RelatedSnapshot(relatedHeading, relatedItems.toList(), relatedLimit)
        stack += next; notifyChanged()
    }
    fun destination(screen: String) { cancelReportGeneration(); relatedSnapshots.clear(); stack.clear(); stack += Route(screen); notifyChanged() }
    private fun saveRelatedContext() {
        if(route.screen == "related") relatedSnapshots[route.id] = RelatedSnapshot(relatedHeading, relatedItems.toList(), relatedLimit)
    }
    private fun restoreRelatedContext() {
        if(route.screen != "related") return
        val snapshot = relatedSnapshots[route.id]
        relatedHeading = snapshot?.heading ?: "Related records"
        relatedItems = snapshot?.items ?: emptyList()
        relatedLimit = snapshot?.limit ?: 20
    }
    fun loadMoreRelated() {
        relatedLimit += 20
        saveRelatedContext()
        notifyChanged()
    }
    fun back(): Boolean {
        cancelReportGeneration()
        if (route.screen in listOf("sign-in", "email-sign-in", "sign-up", "password-reset")) {
            abandonAuthAttempt(authAttempt)
            clearAuthForm()
            if (pendingEmailAuth != null) authError = pendingAuthMessage
        }
        if (stack.size > 1) {
            val departed = stack.removeAt(stack.lastIndex)
            if(departed.screen == "related") relatedSnapshots.remove(departed.id)
            restoreRelatedContext(); notifyChanged(); return true
        }
        if (user != null && route.screen != "home") { destination("home"); return true }
        return false
    }
    fun restoreRoutes(routes: List<Route>, uid: String) {
        if (routes.isEmpty() || routes.any { it.screen in listOf("welcome", "splash", "sign-in", "email-sign-in", "sign-up", "password-reset") }) return
        if (user == null) { pendingRestore = uid to routes; return }
        if (user?.uid == uid && routes.isNotEmpty() && routes.none { it.screen in listOf("welcome", "splash", "sign-in", "email-sign-in", "sign-up", "password-reset") }) {
            stack.clear(); stack.addAll(routes); restoreRelatedContext(); notifyChanged()
        }
    }
    fun beginGoogleAuth(): Int? {
        if (authBusy) return null
        clearAuthForm()
        authErrorSource = "google"
        return beginAuthAttempt(deferSession = true)
    }
    fun beginAnonymousAuth(): Int? {
        if (authBusy) return null
        clearAuthForm()
        authErrorSource = "anonymous"
        return beginAuthAttempt(deferSession = true)
    }
    fun isAuthAttemptCurrent(attempt: Int) = attempt == authAttempt && authLoading
    suspend fun authenticate(token: String, attempt: Int) {
        // Credential Manager may respond after Back or Activity cancellation.
        if (!isAuthAttemptCurrent(attempt)) return
        val result = awaitAuthTask(auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)), attempt)
        if (isAuthAttemptCurrent(attempt)) completeAuthAttempt(attempt, acceptSession = true, newAccount = result.additionalUserInfo?.isNewUser == true)
    }
    suspend fun authenticateAnonymously(attempt: Int) {
        if (!isAuthAttemptCurrent(attempt)) return
        awaitAuthTask(auth.signInAnonymously(), attempt)
        if (isAuthAttemptCurrent(attempt)) completeAuthAttempt(attempt, acceptSession = true)
    }
    fun finishGoogleAuth(attempt: Int, error: String? = null) {
        if (!isAuthAttemptCurrent(attempt)) return
        authError = error
        abandonAuthAttempt(attempt)
        notifyChanged()
    }
    fun finishAnonymousAuth(attempt: Int, error: String? = null) {
        if (!isAuthAttemptCurrent(attempt)) return
        authErrorSource = "anonymous"
        authError = error
        abandonAuthAttempt(attempt)
        notifyChanged()
    }
    fun openAuth(screen: String) {
        if (authBusy) return
        clearAuthForm()
        navigate(screen)
    }
    private fun clearAuthForm() {
        authPassword = ""; authConfirmPassword = ""; authShowPasswords = false; authError = null; authNotice = null
    }
    private fun beginAuthAttempt(deferSession: Boolean): Int {
        val attempt = ++authAttempt
        authTask = null; authLoading = true; deferAuthState = deferSession
        notifyChanged()
        return attempt
    }
    private fun rejectAuthSession(task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>) {
        val uid = task.takeIf { it.isSuccessful }?.result?.user?.uid ?: return
        // Only the session returned by this Task may be rejected. Never clear a published session.
        if (auth.currentUser?.uid == uid && sessionUid != uid) auth.signOut()
    }
    private fun abandonAuthAttempt(attempt: Int) {
        if (attempt != authAttempt) return
        authAttempt++
        authTask?.takeIf { it.first == attempt }?.second?.let(::rejectAuthSession)
        authTask = null; authLoading = false; deferAuthState = false
        if (pendingEmailAuth != null) authError = pendingAuthMessage
        authListener.onAuthStateChanged(auth)
        notifyChanged()
    }
    private fun completeAuthAttempt(attempt: Int, acceptSession: Boolean, newAccount: Boolean = false) {
        if (!isAuthAttemptCurrent(attempt)) return
        val task = authTask?.takeIf { it.first == attempt }?.second
        val uid = task?.takeIf { it.isSuccessful }?.result?.user?.uid
        if (!acceptSession) task?.let(::rejectAuthSession)
        if (task != null && task.isComplete && pendingEmailAuth === task) pendingEmailAuth = null
        authTask = null; authLoading = false; deferAuthState = false
        authListener.onAuthStateChanged(auth)
        if (acceptSession && uid != null && user?.uid == uid) {
            clearAuthForm(); authEmail = ""; authName = ""
            if (newAccount) destination("first-run-profile")
        }
        notifyChanged()
    }
    private suspend fun awaitAuthTask(
        task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>, attempt: Int
    ): com.google.firebase.auth.AuthResult {
        pendingEmailAuth = task; authTask = attempt to task
        task.addOnCompleteListener {
            // Firebase Tasks continue after coroutine timeout/Back; reject their late sessions.
            if (pendingEmailAuth === task) {
                if (attempt != authAttempt) rejectAuthSession(task)
                pendingEmailAuth = null
                if (authError == pendingAuthMessage) authError = "The previous request has finished. Please try again."
            }
            if (!deferAuthState) authListener.onAuthStateChanged(auth)
            notifyChanged()
        }
        return task.awaitResult()
    }
    fun submitEmailAuth() {
        if (authBusy) return
        val screen = route.screen
        if (screen !in listOf("email-sign-in", "sign-up", "password-reset")) return
        val email = authEmail.trim()
        val password = authPassword
        val name = authName.trim()
        authError = when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address."
            screen == "sign-up" && name.isBlank() -> "Enter your agent name."
            screen != "password-reset" && password.isEmpty() -> "Enter your password."
            screen == "sign-up" && password.length < 6 -> "Use at least 6 characters for your password."
            screen == "sign-up" && password != authConfirmPassword -> "Passwords do not match."
            else -> null
        }
        authNotice = null
        if (authError != null) { notifyChanged(); return }
        val attempt = beginAuthAttempt(deferSession = screen != "password-reset")
        viewModelScope.launch {
            var newAccount = false
            var completed = false
            try {
                withTimeout(30_000) {
                    when (screen) {
                        "password-reset" -> {
                            auth.sendPasswordResetEmail(email).awaitResult()
                            if (attempt == authAttempt) authNotice = "If an account uses this email, you’ll receive a password reset link. Check your inbox and spam folder."
                        }
                        "sign-up" -> {
                            val created = awaitAuthTask(auth.createUserWithEmailAndPassword(email, password), attempt).user
                                ?: error("Account unavailable")
                            if (attempt != authAttempt) return@withTimeout
                            newAccount = true
                            created.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(name).build()).awaitResult()
                        }
                        "email-sign-in" -> awaitAuthTask(auth.signInWithEmailAndPassword(email, password), attempt)
                    }
                }
                completed = true
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                android.util.Log.w("ArchiveAuth", "Auth operation failed: ${(error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: error.javaClass.simpleName}")
                if (attempt == authAttempt) {
                    // With enumeration protection, reset remains intentionally neutral.
                    if (screen == "password-reset" && error is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        authNotice = "If an account uses this email, you’ll receive a password reset link. Check your inbox and spam folder."
                    } else authError = authErrorMessage(error)
                    if (newAccount) message = "Account created, but your agent name could not sync. Your account is ready to use."
                    if (error is TimeoutCancellationException && !newAccount) abandonAuthAttempt(attempt)
                }
            } finally {
                completeAuthAttempt(attempt, acceptSession = screen != "password-reset" && (completed || newAccount), newAccount = newAccount)
            }
        }
    }
    fun signOut() { persistDraft(); pendingRestore = null; abandonAuthAttempt(authAttempt); clearAuthForm(); authEmail = ""; authName = ""; auth.signOut() }
    private fun saveProfile(profile: FirebaseUser) {
        viewModelScope.launch {
            try { withTimeout(20_000) { users.profile(profile) } }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; if (user?.uid == profile.uid) { persistenceError = "Your profile could not sync. Check your connection and retry."; notifyChanged() } }
        }
    }
    fun retryCollections() { user?.let { listeners.forEach { l -> l.remove() }; listeners.clear(); persistenceError = null; observeCollections(it.uid); saveProfile(it); notifyChanged() } }
    private fun observeCollections(uid: String) {
        collectionLoading = true; loadingCollections.clear(); loadingCollections.addAll(listOf("favorites", "teams", "missions"))
        for (collection in listOf("favorites", "teams", "missions")) {
            listeners += users.observe(uid, collection) { rows, cached, pending, error ->
                if (user?.uid != uid) return@observe
                loadingCollections.remove(collection); collectionLoading = loadingCollections.isNotEmpty()
                if (cached) cachedCollections.add(collection) else cachedCollections.remove(collection)
                if (pending) pendingCollections.add(collection) else pendingCollections.remove(collection)
                if (error != null) persistenceError = error else when(collection) {
                    "favorites" -> favorites = rows.map { ArchiveItem.fromMap(it.second) }.sortedBy { it.name }
                    "teams" -> teams = rows.map { SavedTeam.fromMap(it.first, it.second) }.sortedByDescending { it.createdAt }
                    "missions" -> history = rows.map { SavedTeam.fromMap(it.first, it.second) }.sortedByDescending { it.createdAt }
                }
                notifyChanged()
            }
        }
    }
    fun favorite(item: ArchiveItem) {
        val uid = user?.uid ?: return
        if (!pendingFavoriteIds.add(item.id)) return
        val remove = favorites.any { it.id == item.id }
        operationLoading = true; notifyChanged()
        viewModelScope.launch {
            try { withTimeout(20_000) { users.favorite(uid, item, remove) }; if (user?.uid == uid) message = if (remove) "Favorite removed" else "Favorite saved" }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; if (user?.uid == uid) message = "Favorite has not synced yet. Changes may be queued locally; check your connection and collection status." }
            finally { pendingFavoriteIds.remove(item.id); operationLoading = false; notifyChanged() }
        }
    }
    fun pageKey(kind: String, query: String, sort: String = this.sort) = "$kind|${query.trim()}|$sort"
    fun loadPage(kind: String, query: String, more: Boolean = false, refresh: Boolean = false) {
        val key = pageKey(kind, query); val old = pages[key]
        if (old?.loading == true || (!refresh && !more && old?.value != null)) return
        val previous = old?.value
        val append = more && previous != null
        pages[key] = Remote(previous, true, append = append); notifyChanged()
        if (kind == "character" && query.isNotBlank()) rememberSearch(query)
        val selectedSort = sort
        viewModelScope.launch {
            try {
                val page = api.list(kind, query, if(append) previous!!.offset + previous.consumed else 0, selectedSort)
                pages[key] = Remote(if (!append) page else Page((previous!!.items + page.items).distinctBy { it.id }, 0, previous.consumed + page.consumed, page.total))
            } catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; pages[key] = Remote(previous, false, e.message ?: "The archive could not be loaded.", append = append) }
            notifyChanged()
        }
    }
    fun loadDetail(kind: String, id: Int, refresh: Boolean = false) {
        val key = "$kind/$id"; val old = details[key]
        if (old?.loading == true || (!refresh && old?.value != null)) return
        details[key] = Remote(old?.value, true); notifyChanged()
        viewModelScope.launch {
            try { val item = api.detail(kind, id, refresh); details[key] = Remote(item); if (kind == "character") rememberCharacter(item) }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; details[key] = Remote(old?.value, false, e.message ?: "The dossier could not be loaded.") }
            notifyChanged()
        }
    }
    fun startMission(template: Mission) { cancelReportGeneration(); mission = template; roster.clear(); teamName = ""; customBriefing = ""; report = null; teamSuggestion = Remote(); simulationSource = null; navigate("briefing", id = template.id) }
    fun select(item: ArchiveItem) {
        cancelReportGeneration()
        when(selectionPurpose) {
            "compareA" -> { compareA = item; back() }
            "compareB" -> { compareB = item; back() }
            "investigateA" -> { investigateA = item; back() }
            "investigateB" -> { investigateB = item; back() }
            "timeline" -> { timelineCharacter = item; timeline = Remote(); back(); loadTimeline() }
            else -> {
                report = null
                teamSuggestion = Remote()
                if (roster.any { it.id == item.id }) roster.removeAll { it.id == item.id }
                else if (roster.size < mission.size) roster += item else message = "This mission allows up to ${mission.size} members."
            }
        }
        notifyChanged()
    }
    fun addFromDetail(item: ArchiveItem) {
        cancelReportGeneration()
        teamSuggestion = Remote()
        if (roster.none { it.id == item.id } && roster.size < mission.size) {
            report = null
            roster += item
            message = "${item.name} added to your active team"
        }
        else message = if (roster.any { it.id == item.id }) "Already on your active team" else "Your active team is full. Remove a member in Recruit."
        notifyChanged()
    }
    fun removeRecruit(item: ArchiveItem) {
        cancelReportGeneration()
        teamSuggestion = Remote()
        report = null
        roster.removeAll { it.id == item.id }; notifyChanged()
    }
    fun suggestMissionTeam() {
        if (teamSuggestion.loading) return
        val origin = route
        teamSuggestion = Remote(teamSuggestion.value, true)
        notifyChanged()
        viewModelScope.launch {
            val candidates = (favorites + recent + roster + pages.values.flatMap { it.value?.items.orEmpty() })
                .filter { it.kind == "character" && it.id > 0 }.distinctBy { it.id }.take(12)
            if (candidates.isEmpty()) {
                teamSuggestion = Remote(error = "Search for characters or save favorites first, then ask for a suggestion.")
                notifyChanged(); return@launch
            }
            val dossiers = mutableListOf<ArchiveItem>()
            var failures = 0
            for (candidate in candidates) {
                if (route != origin) { teamSuggestion = Remote(); notifyChanged(); return@launch }
                try { dossiers += api.detail("character", candidate.id) } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    failures++
                }
            }
            if (route != origin) { teamSuggestion = Remote(); notifyChanged(); return@launch }
            val result = suggestTeam(dossiers, mission)
            pendingSuggestionIds = result.members.map { it.id }.toMutableSet()
            teamSuggestion = if (dossiers.isEmpty()) Remote(error = "Character dossiers could not be loaded. Retry or continue recruiting manually.")
                else Remote(result, error = if (result.members.isEmpty()) "The loaded dossiers do not contain enough documented power data for this mission. You can continue manually." else if (failures > 0) "Some dossiers could not be loaded. This suggestion uses the available records." else null)
            notifyChanged()
        }
    }
    fun acceptSuggestion() {
        val suggested = teamSuggestion.value ?: return
        if (suggested.members.isEmpty()) return
        roster.clear(); roster.addAll(suggested.members.take(mission.size)); report = null
        message = "Suggested roster added. Review, remove, or add members before continuing."
        notifyChanged()
    }
    private var pendingSuggestionIds = mutableSetOf<Int>()
    fun reviseSuggestedMember(item: ArchiveItem) {
        val suggestion = teamSuggestion.value ?: return
        if (!pendingSuggestionIds.add(item.id)) pendingSuggestionIds.remove(item.id)
        if (pendingSuggestionIds.size > mission.size) {
            pendingSuggestionIds.remove(item.id)
            message = "This mission allows up to ${mission.size} members in a suggested roster."
        }
        teamSuggestion = teamSuggestion.copy(value = reviseSuggestion(suggestion, pendingSuggestionIds, mission))
        notifyChanged()
    }
    fun runSimulation() {
        if (roster.isEmpty() || operationLoading) return
        simulationError = null
        val missionCopy = mission
        val origin = route
        val ids = roster.map { it.id }
        operationLoading = true; notifyChanged()
        viewModelScope.launch {
            try {
                val members = ids.map { api.detail("character", it) }
                if (route != origin || mission != missionCopy || roster.map { it.id } != ids) return@launch
                roster.clear(); roster.addAll(members)
                simulationSource = SavedTeam("simulation", teamName.ifBlank { "Untitled team" }, missionCopy.id,
                    if (missionCopy.id == "custom") customBriefing else missionCopy.description, members, System.currentTimeMillis())
                navigate("simulation")
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                simulationError = "Simulation could not retrieve all dossiers. Check your connection and retry."
                message = simulationError
            } finally { operationLoading = false; notifyChanged() }
        }
    }
    fun createReportFromSimulation() {
        val source = simulationSource ?: return
        if (teamName.isBlank()) { message = "Name your team before generating the mission report."; notifyChanged(); return }
        report = source.copy(id = UUID.randomUUID().toString(), name = teamName.trim(), createdAt = System.currentTimeMillis())
        navigate("report", id = report!!.id)
    }
    private fun cancelReportGeneration() {
        reportAttempt++
        reportJob?.let { job ->
            reportJob = null; job.cancel(); operationLoading = false; notifyChanged()
        }
    }
    fun generateReport() {
        if(operationLoading || route.screen != "assembly") return
        if (teamName.isBlank() || roster.isEmpty()) { message = "Name your team and recruit at least one member."; notifyChanged(); return }
        val uid = user?.uid ?: return
        val selected = roster.toList(); val name = teamName.trim(); val selectedMission = mission; val briefing = customBriefing
        val attempt = ++reportAttempt
        val origin = route
        operationLoading = true; notifyChanged()
        reportJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val complete = selected.map { api.detail("character", it.id) }
                if (user?.uid != uid || attempt != reportAttempt || route != origin || teamName.trim() != name || mission != selectedMission || customBriefing != briefing || roster.map { it.id } != selected.map { it.id }) return@launch
                roster.clear(); roster.addAll(complete)
                report = SavedTeam(UUID.randomUUID().toString(), name, selectedMission.id, if (selectedMission.id == "custom") briefing else selectedMission.description, complete, System.currentTimeMillis())
                reportJob = null; operationLoading = false
                navigate("report", id = report!!.id)
            } catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; if(attempt == reportAttempt && user?.uid == uid) message = "Report could not be generated. Check your connection and retry." }
            finally { if(attempt == reportAttempt) { reportJob = null; operationLoading = false; notifyChanged() } }
        }
        reportJob?.start()
    }
    fun saveReport() {
        val uid = user?.uid ?: return; val team = report ?: return
        if (operationLoading) return
        operationLoading = true; notifyChanged()
        viewModelScope.launch {
            try { withTimeout(20_000) { users.saveTeam(uid, team) }; if(user?.uid == uid) message = "Team and mission saved" }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; if(user?.uid == uid) message = "Team has not synced yet. Your report is still here and changes may be queued locally; check your connection and retry." }
            finally { operationLoading = false; notifyChanged() }
        }
    }
    fun deleteTeam(team: SavedTeam) {
        val uid = user?.uid ?: return
        viewModelScope.launch {
            try {
                withTimeout(20_000) { users.deleteTeam(uid, team.id) }
                if(user?.uid == uid) {
                    message = "Saved team deleted. Mission history retained."
                    if (route.screen == "saved-team" && route.id == team.id) back()
                }
            }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; if(user?.uid == uid) message = "Deletion has not synced yet. It may be queued locally; check your connection and retry." }
            notifyChanged()
        }
    }
    fun inspire(team: ArchiveItem) {
        val uid = user?.uid ?: return
        operationLoading = true; notifyChanged()
        viewModelScope.launch {
            try {
                val ids = team.related("characters").ifEmpty { team.related("members") }.take(mission.size)
                check(ids.isNotEmpty()) { "No team members are documented in this record." }
                val members = ids.map { api.detail("character", it.id) }
                if (user?.uid != uid) return@launch
                roster.clear(); roster.addAll(members); report = null; selectionPurpose = "mission"; navigate("picker")
            } catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; message = e.message }
            finally { operationLoading = false; notifyChanged() }
        }
    }
    fun loadTimeline(refresh: Boolean = false) {
        val selected = timelineCharacter ?: return
        if (timeline.loading || (!refresh && timeline.value != null)) return
        val previous = timeline.value
        timeline = Remote(previous, loading = true); notifyChanged()
        viewModelScope.launch {
            try {
                val hero = api.detail("character", selected.id, refresh)
                if(timelineCharacter?.id != selected.id) return@launch
                timelineCharacter = hero
                val issues = (listOfNotNull(hero.reference("first_appeared_in_issue")) + hero.related("issue_credits")).distinctBy { it.id }.take(20)
                val loaded = mutableListOf<ArchiveItem>(); var failed = 0
                for (issue in issues) { try { loaded += api.detail("issue", issue.id, refresh) } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    failed++; previous?.firstOrNull { it.id == issue.id }?.let { loaded += it }
                } }
                if(timelineCharacter?.id != selected.id) return@launch
                timeline = Remote(loaded.sortedBy { it.text("cover_date").ifBlank { "9999" } }, error = if(failed > 0) "Some dates could not be refreshed. Previous records are kept where available. Retry to complete this partial timeline." else null)
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                if(timelineCharacter?.id != selected.id) return@launch
                timeline = Remote(previous, error = e.message)
            }
            notifyChanged()
        }
    }
    fun clearLocalHistory() {
        val uid = user?.uid ?: return
        recent = emptyList(); searches = emptyList()
        prefs.edit().remove("recent_$uid").remove("search_$uid").apply()
        message = "Local reading and search history cleared"; notifyChanged()
    }
    private fun rememberCharacter(item: ArchiveItem) {
        val uid = user?.uid ?: return
        recent = (listOf(item) + recent.filter { it.id != item.id }).take(15)
        prefs.edit().putString("recent_$uid", JSONArray(recent.map { JSONObject(it.map()) }).toString()).apply()
    }
    private fun rememberSearch(query: String) {
        val uid = user?.uid ?: return
        searches = (listOf(query.trim()) + searches.filterNot { it.equals(query.trim(), true) }).take(8)
        prefs.edit().putString("search_$uid", JSONArray(searches).toString()).apply()
    }
    fun persistDraft() {
        val uid = user?.uid ?: return
        val draft = JSONObject().put("mission", mission.id).put("name", teamName).put("briefing", customBriefing)
            .put("roster", JSONArray(roster.map { JSONObject(it.map()) }))
        compareA?.let { draft.put("compareA", JSONObject(it.map())) }
        compareB?.let { draft.put("compareB", JSONObject(it.map())) }
        investigateA?.let { draft.put("investigateA", JSONObject(it.map())) }
        investigateB?.let { draft.put("investigateB", JSONObject(it.map())) }
        report?.let { draft.put("report", JSONObject(it.map()).put("id", it.id)) }
        prefs.edit().putString("draft_$uid", draft.toString()).apply()
    }
    private fun loadLocal(uid: String) {
        runCatching {
            val draft = JSONObject(prefs.getString("draft_$uid", "{}") ?: "{}")
            mission = missions.firstOrNull { it.id == draft.optString("mission") } ?: missions.first()
            teamName = draft.optString("name"); customBriefing = draft.optString("briefing")
            fun item(j: JSONObject) = ArchiveItem.fromMap(j.keys().asSequence().associateWith { j.opt(it) })
            roster.clear(); roster.addAll(draft.optJSONArray("roster").items().map(::item))
            compareA = draft.optJSONObject("compareA")?.let(::item); compareB = draft.optJSONObject("compareB")?.let(::item)
            investigateA = draft.optJSONObject("investigateA")?.let(::item); investigateB = draft.optJSONObject("investigateB")?.let(::item)
            draft.optJSONObject("report")?.let { j ->
                report = SavedTeam(j.optString("id"), j.optString("name"), j.optString("missionId"), j.optString("briefing"), j.optJSONArray("members").items().map(::item), j.optLong("createdAt"))
            }
        }

        recent = runCatching { JSONArray(prefs.getString("recent_$uid", "[]")).items().map { ArchiveItem.fromMap(it.keys().asSequence().associateWith { k -> it.opt(k) }) } }.getOrDefault(emptyList())
        searches = runCatching { val a = JSONArray(prefs.getString("search_$uid", "[]")); (0 until a.length()).map { a.getString(it) } }.getOrDefault(emptyList())
    }
    override fun onCleared() { abandonAuthAttempt(authAttempt); auth.removeAuthStateListener(authListener); listeners.forEach { it.remove() } }
}
