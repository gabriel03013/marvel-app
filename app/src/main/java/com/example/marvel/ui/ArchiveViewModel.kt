package com.example.marvel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

data class Route(val screen: String, val kind: String = "", val id: String = "")

data class Remote<T>(
    val value: T? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val append: Boolean = false,
)

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    val changes = MutableStateFlow(0)
    val api = ComicVineRepository()
    internal val auth = FirebaseAuth.getInstance()
    internal val users = UserRepository()
    val profileEditor =
        ProfileEditorController(
            users = users,
            scope = viewModelScope,
            onProfileUpdated = { refreshSignedInProfile() },
            onStateChanged = ::notifyChanged,
        )
    var user: FirebaseUser? = null
        internal set

    val stack = mutableListOf(Route("splash"))
    val route
        get() = stack.last()

    var authEmail = ""
    var authPassword = ""
    var authConfirmPassword = ""
    var authName = ""
    var authShowPasswords = false
    var authNotice: String? = null
    var authErrorSource = "google"
    internal var deferAuthState = false
    internal var authAttempt = 0
    internal var pendingEmailAuth:
        com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>? =
        null
    internal var authTask:
        Pair<Int, com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>>? =
        null
    internal val pendingAuthMessage =
        "This request is still finishing. Please wait, or use Back to leave this screen."
    val authBusy
        get() = authLoading || pendingEmailAuth != null

    var authLoading = false
    var authError: String? = null
    var persistenceError: String? = null
    internal val cachedCollections = mutableSetOf<String>()
    internal val pendingCollections = mutableSetOf<String>()
    val collectionCached
        get() = cachedCollections.isNotEmpty()

    val collectionPending
        get() = pendingCollections.isNotEmpty()

    var collectionLoading = true
    var favorites = emptyList<ArchiveItem>()
    var teams = emptyList<SavedTeam>()
    var history = emptyList<SavedTeam>()
    var recent = emptyList<ArchiveItem>()
    var boosters = emptyMap<String, BoosterInventory>()
    var collectedCharacters = emptyList<CollectedCharacter>()
    var currentPull: Pair<BoosterType, List<ArchiveItem>>? = null
    var currentPullLevels = emptyMap<Int, Long>()
    var revealIndex = 0
    var boosterLoading = false
    var boosterError: String? = null
    internal var boosterErrorTypeId: String? = null
    internal var boosterErrorWasOpen = false
    var searches = emptyList<String>()
    var collectionTab = "Favorites"
    var mission = missions.first()
    var customBriefing = ""
        set(value) {
            if (field != value) cancelReportGeneration()
            field = value
        }

    val roster = mutableListOf<ArchiveItem>()
    var teamName = ""
        set(value) {
            if (field != value) cancelReportGeneration()
            field = value
        }

    val hasRecruitmentDraft
        get() =
            roster.isNotEmpty() ||
                teamName.isNotBlank() ||
                (mission.id == "custom" && customBriefing.isNotBlank())

    var report: SavedTeam? = null
    var teamSuggestion: Remote<TeamSuggestion> = Remote()
    var connectionsExpanded = false
    var connectionRecordKey = ""
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

    private data class RelatedSnapshot(
        val heading: String,
        val items: List<ArchiveItem>,
        val limit: Int,
    )

    private val relatedSnapshots = mutableMapOf<String, RelatedSnapshot>()
    var timelineFilter = "All"
    var timeline = Remote<List<ArchiveItem>>()
    var message: String? = null
    var operationLoading = false
    internal var reportJob: Job? = null
    internal var reportAttempt = 0
    internal var pendingSuggestionIds = mutableSetOf<Int>()
    val details = mutableMapOf<String, Remote<ArchiveItem>>()
    val pages = mutableMapOf<String, Remote<Page>>()
    val queries = mutableMapOf<String, String>()
    val submittedQueries = mutableMapOf<String, String>()
    var marvelOnly = false
    var sort = "name:asc"
    internal val listeners = mutableListOf<ListenerRegistration>()
    internal val loadingCollections = mutableSetOf<String>()
    internal val pendingFavoriteIds = mutableSetOf<Int>()
    internal var sessionUid: String? = null
    internal var pendingRestore: Pair<String, List<Route>>? = null
    internal val prefs = application.getSharedPreferences("archive_history", 0)
    internal val authListener = FirebaseAuth.AuthStateListener { firebase ->
        val next = firebase.currentUser
        if (
            !deferAuthState &&
                pendingEmailAuth == null &&
                (next?.uid != sessionUid || route.screen == "splash")
        ) {
            cancelReportGeneration()
            user = next
            sessionUid = next?.uid
            listeners.forEach { it.remove() }
            listeners.clear()
            favorites = emptyList()
            teams = emptyList()
            history = emptyList()
            recent = emptyList()
            searches = emptyList()
            boosters = emptyMap()
            collectedCharacters = emptyList()
            clearBoosterPull()
            boosterLoading = false
            boosterError = null
            boosterErrorTypeId = null
            roster.clear()
            report = null
            compareA = null
            compareB = null
            investigateA = null
            investigateB = null
            timelineCharacter = null
            queries.clear()
            submittedQueries.clear()
            marvelOnly = false
            selectionPurpose = "mission"
            relatedItems = emptyList()
            relatedSnapshots.clear()
            timeline = Remote()
            persistenceError = null
            cachedCollections.clear()
            pendingCollections.clear()
            stack.clear()
            stack += Route(if (next == null) "welcome" else "home")
            if (next != null) {
                loadLocal(next.uid)
                observeCollections(next.uid)
                saveProfile(next)
                pendingRestore
                    ?.takeIf { it.first == next.uid }
                    ?.let { (_, routes) ->
                        stack.clear()
                        stack.addAll(routes)
                        restoreRelatedContext()
                    }
                pendingRestore = null
            }
            notifyChanged()
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun notifyChanged() {
        changes.value++
    }

    fun navigate(screen: String, kind: String = "", id: String = "") {
        cancelReportGeneration()
        saveRelatedContext()
        val next =
            Route(
                screen,
                kind,
                if (screen == "related" && id.isBlank()) UUID.randomUUID().toString() else id,
            )
        if (screen == "related")
            relatedSnapshots[next.id] =
                RelatedSnapshot(relatedHeading, relatedItems.toList(), relatedLimit)
        stack += next
        notifyChanged()
    }

    fun destination(screen: String) {
        cancelReportGeneration()
        relatedSnapshots.clear()
        stack.clear()
        stack += Route(screen)
        notifyChanged()
    }

    private fun saveRelatedContext() {
        if (route.screen == "related")
            relatedSnapshots[route.id] =
                RelatedSnapshot(relatedHeading, relatedItems.toList(), relatedLimit)
    }

    private fun restoreRelatedContext() {
        if (route.screen != "related") return
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
            if (departed.screen == "related") relatedSnapshots.remove(departed.id)
            restoreRelatedContext()
            notifyChanged()
            return true
        }
        if (user != null && route.screen != "home") {
            destination("home")
            return true
        }
        return false
    }

    fun restoreRoutes(routes: List<Route>, uid: String) {
        if (
            routes.isEmpty() ||
                routes.any {
                    it.screen in
                        listOf(
                            "welcome",
                            "splash",
                            "sign-in",
                            "email-sign-in",
                            "sign-up",
                            "password-reset",
                        )
                }
        )
            return
        if (user == null) {
            pendingRestore = uid to routes
            return
        }
        if (
            user?.uid == uid &&
                routes.isNotEmpty() &&
                routes.none {
                    it.screen in
                        listOf(
                            "welcome",
                            "splash",
                            "sign-in",
                            "email-sign-in",
                            "sign-up",
                            "password-reset",
                        )
                }
        ) {
            stack.clear()
            stack.addAll(routes)
            restoreRelatedContext()
            notifyChanged()
        }
    }

    override fun onCleared() {
        abandonAuthAttempt(authAttempt)
        auth.removeAuthStateListener(authListener)
        listeners.forEach { it.remove() }
    }
}
