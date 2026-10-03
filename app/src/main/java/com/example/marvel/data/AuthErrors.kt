package com.example.marvel.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.TimeoutCancellationException

fun authErrorMessage(error: Exception): String = when {
    error is TimeoutCancellationException -> "The request timed out. Check your connection and try again."
    error is FirebaseNetworkException -> "Cannot connect. Check your internet connection and try again."
    error is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
    error is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method is unavailable. Try again later or choose another sign-in method."
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password that meets this account’s password policy."
        "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "This email already has an account. Use your existing sign-in method."
        "ERROR_USER_DISABLED" -> "This account has been disabled. Contact the app administrator."
        "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> "Email or password is incorrect. Try again or reset your password."
        else -> "Could not complete sign-in. Please try again."
    }
    else -> "Could not complete the request. Please try again."
}
