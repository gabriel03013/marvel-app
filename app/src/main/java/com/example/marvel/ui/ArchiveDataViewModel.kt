package com.example.marvel.ui

import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

fun ArchiveViewModel.pageKey(kind: String, query: String, sort: String = this.sort) =
    "$kind|${query.trim()}|$sort"

fun ArchiveViewModel.loadPage(
    kind: String,
    query: String,
    more: Boolean = false,
    refresh: Boolean = false,
) {
    val key = pageKey(kind, query)
    val old = pages[key]
    if (old?.loading == true || (!refresh && !more && old?.value != null)) return
    val previous = old?.value
    val append = more && previous != null
    pages[key] = Remote(previous, true, append = append)
    notifyChanged()
    if (kind == "character" && query.isNotBlank()) rememberSearch(query)
    val selectedSort = sort
    viewModelScope.launch {
        try {
            val page =
                api.list(
                    kind,
                    query,
                    if (append) previous!!.offset + previous.consumed else 0,
                    selectedSort,
                )
            pages[key] =
                Remote(
                    if (!append) page
                    else
                        Page(
                            (previous!!.items + page.items).distinctBy { it.id },
                            0,
                            previous.consumed + page.consumed,
                            page.total,
                        )
                )
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            pages[key] =
                Remote(
                    previous,
                    false,
                    e.message ?: "The archive could not be loaded.",
                    append = append,
                )
        }
        notifyChanged()
    }
}

fun ArchiveViewModel.loadDetail(kind: String, id: Int, refresh: Boolean = false) {
    val key = "$kind/$id"
    val old = details[key]
    if (old?.loading == true || (!refresh && old?.value != null)) return
    details[key] = Remote(old?.value, true)
    notifyChanged()
    viewModelScope.launch {
        try {
            val item = api.detail(kind, id, refresh)
            details[key] = Remote(item)
            if (kind == "character") rememberCharacter(item)
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            details[key] =
                Remote(old?.value, false, e.message ?: "The dossier could not be loaded.")
        }
        notifyChanged()
    }
}

fun ArchiveViewModel.loadTimeline(refresh: Boolean = false) {
    val selected = timelineCharacter ?: return
    if (timeline.loading || (!refresh && timeline.value != null)) return
    val previous = timeline.value
    timeline = Remote(previous, loading = true)
    notifyChanged()
    viewModelScope.launch {
        try {
            val hero = api.detail("character", selected.id, refresh)
            if (timelineCharacter?.id != selected.id) return@launch
            timelineCharacter = hero
            val issues =
                (listOfNotNull(hero.reference("first_appeared_in_issue")) +
                        hero.related("issue_credits"))
                    .distinctBy { it.id }
                    .take(20)
            val loaded = mutableListOf<ArchiveItem>()
            var failed = 0
            for (issue in issues) {
                try {
                    loaded += api.detail("issue", issue.id, refresh)
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    failed++
                    previous?.firstOrNull { it.id == issue.id }?.let { loaded += it }
                }
            }
            if (timelineCharacter?.id != selected.id) return@launch
            timeline =
                Remote(
                    loaded.sortedBy { it.text("cover_date").ifBlank { "9999" } },
                    error =
                        if (failed > 0)
                            "Some dates could not be refreshed. Previous records are kept where available. Retry to complete this partial timeline."
                        else null,
                )
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            if (timelineCharacter?.id != selected.id) return@launch
            timeline = Remote(previous, error = e.message)
        }
        notifyChanged()
    }
}
