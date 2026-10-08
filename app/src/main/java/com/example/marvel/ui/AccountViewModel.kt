package com.example.marvel.ui

import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

fun ArchiveViewModel.editProfile() {
    val signedInUser = user ?: return
    profileEditor.open(signedInUser)
    navigate("edit-profile")
}

fun ArchiveViewModel.saveProfileEdit() {
    user?.let(profileEditor::save)
}

fun ArchiveViewModel.refreshSignedInProfile() {
    user = auth.currentUser ?: user
    notifyChanged()
}

fun ArchiveViewModel.saveProfile(profile: FirebaseUser) {
    viewModelScope.launch {
        try {
            withTimeout(20_000) { users.profile(profile) }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            if (user?.uid == profile.uid) {
                persistenceError = "Your profile could not sync. Check your connection and retry."
                notifyChanged()
            }
        }
    }
}

fun ArchiveViewModel.retryCollections() {
    user?.let {
        listeners.forEach { l -> l.remove() }
        listeners.clear()
        persistenceError = null
        observeCollections(it.uid)
        saveProfile(it)
        notifyChanged()
    }
}

fun ArchiveViewModel.observeCollections(uid: String) {
    collectionLoading = true
    loadingCollections.clear()
    loadingCollections.addAll(
        listOf("favorites", "teams", "missions", "boosters", "collected_characters")
    )
    for (collection in
        listOf("favorites", "teams", "missions", "boosters", "collected_characters")) {
        listeners +=
            users.observe(uid, collection) { rows, cached, pending, error ->
                if (user?.uid != uid) return@observe
                loadingCollections.remove(collection)
                collectionLoading = loadingCollections.isNotEmpty()
                if (cached) cachedCollections.add(collection)
                else cachedCollections.remove(collection)
                if (pending) pendingCollections.add(collection)
                else pendingCollections.remove(collection)
                if (error != null) persistenceError = error
                else
                    when (collection) {
                        "favorites" ->
                            favorites =
                                rows.map { ArchiveItem.fromMap(it.second) }.sortedBy { it.name }
                        "teams" ->
                            teams =
                                rows
                                    .map { SavedTeam.fromMap(it.first, it.second) }
                                    .sortedByDescending { it.createdAt }
                        "missions" ->
                            history =
                                rows
                                    .map { SavedTeam.fromMap(it.first, it.second) }
                                    .sortedByDescending { it.createdAt }
                        "boosters" ->
                            boosters =
                                rows
                                    .mapNotNull { (id, data) ->
                                        boosterTypes
                                            .firstOrNull { it.id == id }
                                            ?.let {
                                                it.id to
                                                    BoosterInventory(
                                                        it,
                                                        (data["count"] as? Number)
                                                            ?.toLong()
                                                            ?.coerceAtLeast(0L) ?: 0L,
                                                    )
                                            }
                                    }
                                    .toMap()
                        "collected_characters" ->
                            collectedCharacters =
                                rows
                                    .map { CollectedCharacter.fromMap(it.second) }
                                    .sortedBy { it.item.name.lowercase() }
                    }
                notifyChanged()
            }
    }
}

fun ArchiveViewModel.favorite(item: ArchiveItem) {
    val uid = user?.uid ?: return
    if (!pendingFavoriteIds.add(item.id)) return
    val remove = favorites.any { it.id == item.id }
    operationLoading = true
    notifyChanged()
    viewModelScope.launch {
        try {
            withTimeout(20_000) { users.favorite(uid, item, remove) }
            if (user?.uid == uid) message = if (remove) "Favorite removed" else "Favorite saved"
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            if (user?.uid == uid)
                message =
                    "Favorite has not synced yet. Changes may be queued locally; check your connection and collection status."
        } finally {
            pendingFavoriteIds.remove(item.id)
            operationLoading = false
            notifyChanged()
        }
    }
}

fun ArchiveViewModel.clearLocalHistory() {
    val uid = user?.uid ?: return
    recent = emptyList()
    searches = emptyList()
    prefs.edit().remove("recent_$uid").remove("search_$uid").apply()
    message = "Local reading and search history cleared"
    notifyChanged()
}

fun ArchiveViewModel.rememberCharacter(item: ArchiveItem) {
    val uid = user?.uid ?: return
    recent = (listOf(item) + recent.filter { it.id != item.id }).take(15)
    prefs
        .edit()
        .putString("recent_$uid", JSONArray(recent.map { JSONObject(it.map()) }).toString())
        .apply()
}

fun ArchiveViewModel.rememberSearch(query: String) {
    val uid = user?.uid ?: return
    searches = (listOf(query.trim()) + searches.filterNot { it.equals(query.trim(), true) }).take(8)
    prefs.edit().putString("search_$uid", JSONArray(searches).toString()).apply()
}

fun ArchiveViewModel.persistDraft() {
    val uid = user?.uid ?: return
    val draft =
        JSONObject()
            .put("mission", mission.id)
            .put("name", teamName)
            .put("briefing", customBriefing)
            .put("roster", JSONArray(roster.map { JSONObject(it.map()) }))
    compareA?.let { draft.put("compareA", JSONObject(it.map())) }
    compareB?.let { draft.put("compareB", JSONObject(it.map())) }
    investigateA?.let { draft.put("investigateA", JSONObject(it.map())) }
    investigateB?.let { draft.put("investigateB", JSONObject(it.map())) }
    report?.let { draft.put("report", JSONObject(it.map()).put("id", it.id)) }
    prefs.edit().putString("draft_$uid", draft.toString()).apply()
}

fun ArchiveViewModel.loadLocal(uid: String) {
    runCatching {
        val draft = JSONObject(prefs.getString("draft_$uid", "{}") ?: "{}")
        mission = missions.firstOrNull { it.id == draft.optString("mission") } ?: missions.first()
        teamName = draft.optString("name")
        customBriefing = draft.optString("briefing")
        fun item(j: JSONObject) =
            ArchiveItem.fromMap(j.keys().asSequence().associateWith { j.opt(it) })
        roster.clear()
        roster.addAll(draft.optJSONArray("roster").items().map(::item))
        compareA = draft.optJSONObject("compareA")?.let(::item)
        compareB = draft.optJSONObject("compareB")?.let(::item)
        investigateA = draft.optJSONObject("investigateA")?.let(::item)
        investigateB = draft.optJSONObject("investigateB")?.let(::item)
        draft.optJSONObject("report")?.let { j ->
            report =
                SavedTeam(
                    j.optString("id"),
                    j.optString("name"),
                    j.optString("missionId"),
                    j.optString("briefing"),
                    j.optJSONArray("members").items().map(::item),
                    j.optLong("createdAt"),
                )
        }
    }

    recent =
        runCatching {
                JSONArray(prefs.getString("recent_$uid", "[]")).items().map {
                    ArchiveItem.fromMap(it.keys().asSequence().associateWith { k -> it.opt(k) })
                }
            }
            .getOrDefault(emptyList())
    searches =
        runCatching {
                val a = JSONArray(prefs.getString("search_$uid", "[]"))
                (0 until a.length()).map { a.getString(it) }
            }
            .getOrDefault(emptyList())
}
