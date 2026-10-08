package com.example.marvel.ui

import com.example.marvel.data.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ProfileEditorController(
    private val users: UserRepository,
    private val scope: CoroutineScope,
    private val onProfileUpdated: () -> Unit,
    private val onStateChanged: () -> Unit,
) {
    var displayName = ""
        private set

    var isSaving = false
        private set

    var error: String? = null
        private set

    var saved = false
        private set

    fun open(user: FirebaseUser) {
        displayName = user.displayName.orEmpty()
        error = null
        saved = false
    }

    fun changeDisplayName(value: String) {
        displayName = value
        error = null
        saved = false
    }

    fun save(user: FirebaseUser) {
        if (isSaving) return

        val updatedName = displayName.trim()
        when {
            updatedName.isEmpty() -> {
                error = "Enter a display name."
                onStateChanged()
                return
            }
            updatedName.length > MAX_DISPLAY_NAME_LENGTH -> {
                error = "Use no more than $MAX_DISPLAY_NAME_LENGTH characters."
                onStateChanged()
                return
            }
        }

        if (updatedName == user.displayName) {
            displayName = updatedName
            saved = true
            error = null
            onStateChanged()
            return
        }

        isSaving = true
        error = null
        saved = false
        onStateChanged()

        scope.launch {
            try {
                withTimeout(PROFILE_SAVE_TIMEOUT_MS) {
                    users.updateDisplayName(user, updatedName)
                }
                displayName = updatedName
                saved = true
                onProfileUpdated()
            } catch (failure: Exception) {
                if (failure is CancellationException && failure !is TimeoutCancellationException) {
                    throw failure
                }
                error = "Your profile could not be updated. Check your connection and retry."
            } finally {
                isSaving = false
                onStateChanged()
            }
        }
    }

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 40
        const val PROFILE_SAVE_TIMEOUT_MS = 20_000L
    }
}
