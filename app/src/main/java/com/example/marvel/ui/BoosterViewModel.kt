package com.example.marvel.ui

import androidx.lifecycle.viewModelScope
import com.example.marvel.data.BoosterType
import com.example.marvel.data.boosterTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun ArchiveViewModel.addBooster(type: BoosterType) {
    val uid = user?.uid ?: return
    if (boosterLoading) return

    boosterLoading = true
    boosterError = null
    boosterErrorTypeId = type.id
    boosterErrorWasOpen = false
    notifyChanged()

    viewModelScope.launch {
        try {
            withTimeout(20_000) { users.addBooster(uid, type) }
            if (user?.uid == uid) message = "${type.title} added to your collection"
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException)
                throw error
            if (user?.uid == uid) {
                boosterError =
                    error.message
                        ?: "This booster could not be added. Check your connection and retry."
            }
        } finally {
            boosterLoading = false
            notifyChanged()
        }
    }
}

fun ArchiveViewModel.selectBooster(type: BoosterType) {
    boosterError = null
    boosterErrorTypeId = type.id
    clearBoosterPull()
    navigate("booster-opening", id = type.id)
}

fun ArchiveViewModel.openBooster(type: BoosterType) {
    val uid = user?.uid ?: return
    if (boosterLoading) return

    val availableBoosters = boosters[type.id]?.count ?: 0L
    if (availableBoosters < 1L) {
        boosterError = "You do not have a ${type.title} booster yet. Add one from the booster shop."
        notifyChanged()
        return
    }

    boosterLoading = true
    boosterError = null
    boosterErrorTypeId = type.id
    boosterErrorWasOpen = true
    notifyChanged()

    val openingRoute = route
    viewModelScope.launch {
        try {
            val cards = withTimeout(60_000) { api.randomCharacters(type.cardCount) }
            check(cards.size == type.cardCount) {
                "Comic Vine returned fewer than ${type.cardCount} character dossiers. Retry the opening."
            }
            val levels = users.openBooster(uid, type, cards)
            if (user?.uid == uid) {
                currentPull = type to cards
                currentPullLevels = levels
                revealIndex = 0
                if (route == openingRoute) navigate("booster-reveal", id = type.id)
                else message = "Booster opened. Your cards are saved in My Collection."
            }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException)
                throw error
            if (user?.uid == uid) {
                boosterError =
                    when (error) {
                        is TimeoutCancellationException ->
                            "Opening timed out. Your booster was not confirmed as opened. Check your collection and retry."
                        else ->
                            error.message
                                ?: "The booster could not be opened. Check your connection and retry."
                    }
            }
        } finally {
            boosterLoading = false
            notifyChanged()
        }
    }
}

fun ArchiveViewModel.retryBoosterAction() {
    val type = boosterTypes.firstOrNull { it.id == boosterErrorTypeId } ?: return
    if (boosterErrorWasOpen) openBooster(type) else addBooster(type)
}

fun ArchiveViewModel.continueReveal() {
    val pull = currentPull ?: return
    revealIndex = (revealIndex + 1).coerceAtMost(pull.second.lastIndex)
    notifyChanged()
}

fun ArchiveViewModel.finishReveal() {
    collectionTab = "Cards"
    clearBoosterPull()
    destination("collection")
    message = "Cards saved to your collection"
    notifyChanged()
}

fun ArchiveViewModel.clearBoosterPull() {
    currentPull = null
    currentPullLevels = emptyMap()
    revealIndex = 0
}
