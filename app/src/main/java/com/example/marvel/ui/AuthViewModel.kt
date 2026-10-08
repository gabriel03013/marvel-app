package com.example.marvel.ui

import androidx.lifecycle.viewModelScope
import com.example.marvel.data.*
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun ArchiveViewModel.beginGoogleAuth(): Int? {
    if (authBusy) return null
    clearAuthForm()
    authErrorSource = "google"
    return beginAuthAttempt(deferSession = true)
}

fun ArchiveViewModel.beginAnonymousAuth(): Int? {
    if (authBusy) return null
    clearAuthForm()
    authErrorSource = "anonymous"
    return beginAuthAttempt(deferSession = true)
}

fun ArchiveViewModel.isAuthAttemptCurrent(attempt: Int) = attempt == authAttempt && authLoading

suspend fun ArchiveViewModel.authenticate(token: String, attempt: Int) {
    if (!isAuthAttemptCurrent(attempt)) return
    val result =
        awaitAuthTask(
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)),
            attempt,
        )
    if (isAuthAttemptCurrent(attempt))
        completeAuthAttempt(
            attempt,
            acceptSession = true,
            newAccount = result.additionalUserInfo?.isNewUser == true,
        )
}

suspend fun ArchiveViewModel.authenticateAnonymously(attempt: Int) {
    if (!isAuthAttemptCurrent(attempt)) return
    awaitAuthTask(auth.signInAnonymously(), attempt)
    if (isAuthAttemptCurrent(attempt)) completeAuthAttempt(attempt, acceptSession = true)
}

fun ArchiveViewModel.finishGoogleAuth(attempt: Int, error: String? = null) {
    if (!isAuthAttemptCurrent(attempt)) return
    authError = error
    abandonAuthAttempt(attempt)
    notifyChanged()
}

fun ArchiveViewModel.finishAnonymousAuth(attempt: Int, error: String? = null) {
    if (!isAuthAttemptCurrent(attempt)) return
    authErrorSource = "anonymous"
    authError = error
    abandonAuthAttempt(attempt)
    notifyChanged()
}

fun ArchiveViewModel.openAuth(screen: String) {
    if (authBusy) return
    clearAuthForm()
    navigate(screen)
}

fun ArchiveViewModel.clearAuthForm() {
    authPassword = ""
    authConfirmPassword = ""
    authShowPasswords = false
    authError = null
    authNotice = null
}

fun ArchiveViewModel.beginAuthAttempt(deferSession: Boolean): Int {
    val attempt = ++authAttempt
    authTask = null
    authLoading = true
    deferAuthState = deferSession
    notifyChanged()
    return attempt
}

fun ArchiveViewModel.rejectAuthSession(
    task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>
) {
    val uid = task.takeIf { it.isSuccessful }?.result?.user?.uid ?: return
    if (auth.currentUser?.uid == uid && sessionUid != uid) auth.signOut()
}

fun ArchiveViewModel.abandonAuthAttempt(attempt: Int) {
    if (attempt != authAttempt) return
    authAttempt++
    authTask?.takeIf { it.first == attempt }?.second?.let(::rejectAuthSession)
    authTask = null
    authLoading = false
    deferAuthState = false
    if (pendingEmailAuth != null) authError = pendingAuthMessage
    authListener.onAuthStateChanged(auth)
    notifyChanged()
}

fun ArchiveViewModel.completeAuthAttempt(
    attempt: Int,
    acceptSession: Boolean,
    newAccount: Boolean = false,
) {
    if (!isAuthAttemptCurrent(attempt)) return
    val task = authTask?.takeIf { it.first == attempt }?.second
    val uid = task?.takeIf { it.isSuccessful }?.result?.user?.uid
    if (!acceptSession) task?.let(::rejectAuthSession)
    if (task != null && task.isComplete && pendingEmailAuth === task) pendingEmailAuth = null
    authTask = null
    authLoading = false
    deferAuthState = false
    authListener.onAuthStateChanged(auth)
    if (acceptSession && uid != null && user?.uid == uid) {
        clearAuthForm()
        authEmail = ""
        authName = ""
        if (newAccount) destination("first-run-profile")
    }
    notifyChanged()
}

suspend fun ArchiveViewModel.awaitAuthTask(
    task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>,
    attempt: Int,
): com.google.firebase.auth.AuthResult {
    pendingEmailAuth = task
    authTask = attempt to task
    task.addOnCompleteListener {
        if (pendingEmailAuth === task) {
            if (attempt != authAttempt) rejectAuthSession(task)
            pendingEmailAuth = null
            if (authError == pendingAuthMessage)
                authError = "The previous request has finished. Please try again."
        }
        if (!deferAuthState) authListener.onAuthStateChanged(auth)
        notifyChanged()
    }
    return task.awaitResult()
}

fun ArchiveViewModel.submitEmailAuth() {
    if (authBusy) return
    val screen = route.screen
    if (screen !in listOf("email-sign-in", "sign-up", "password-reset")) return
    val email = authEmail.trim()
    val password = authPassword
    val name = authName.trim()
    authError =
        when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Enter a valid email address."
            screen == "sign-up" && name.isBlank() -> "Enter your agent name."
            screen != "password-reset" && password.isEmpty() -> "Enter your password."
            screen == "sign-up" && password.length < 6 ->
                "Use at least 6 characters for your password."
            screen == "sign-up" && password != authConfirmPassword -> "Passwords do not match."
            else -> null
        }
    authNotice = null
    if (authError != null) {
        notifyChanged()
        return
    }
    val attempt = beginAuthAttempt(deferSession = screen != "password-reset")
    viewModelScope.launch {
        var newAccount = false
        var completed = false
        try {
            withTimeout(30_000) {
                when (screen) {
                    "password-reset" -> {
                        auth.sendPasswordResetEmail(email).awaitResult()
                        if (attempt == authAttempt)
                            authNotice =
                                "If an account uses this email, you’ll receive a password reset link. Check your inbox and spam folder."
                    }
                    "sign-up" -> {
                        val created =
                            awaitAuthTask(
                                    auth.createUserWithEmailAndPassword(email, password),
                                    attempt,
                                )
                                .user ?: error("Account unavailable")
                        if (attempt != authAttempt) return@withTimeout
                        newAccount = true
                        created
                            .updateProfile(
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()
                            )
                            .awaitResult()
                    }
                    "email-sign-in" ->
                        awaitAuthTask(auth.signInWithEmailAndPassword(email, password), attempt)
                }
            }
            completed = true
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException)
                throw error
            android.util.Log.w(
                "ArchiveAuth",
                "Auth operation failed: ${(error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: error.javaClass.simpleName}",
            )
            if (attempt == authAttempt) {
                if (
                    screen == "password-reset" &&
                        error is com.google.firebase.auth.FirebaseAuthInvalidUserException
                ) {
                    authNotice =
                        "If an account uses this email, you’ll receive a password reset link. Check your inbox and spam folder."
                } else authError = authErrorMessage(error)
                if (newAccount)
                    message =
                        "Account created, but your agent name could not sync. Your account is ready to use."
                if (error is TimeoutCancellationException && !newAccount)
                    abandonAuthAttempt(attempt)
            }
        } finally {
            completeAuthAttempt(
                attempt,
                acceptSession = screen != "password-reset" && (completed || newAccount),
                newAccount = newAccount,
            )
        }
    }
}

fun ArchiveViewModel.signOut() {
    persistDraft()
    pendingRestore = null
    abandonAuthAttempt(authAttempt)
    clearAuthForm()
    authEmail = ""
    authName = ""
    auth.signOut()
}
