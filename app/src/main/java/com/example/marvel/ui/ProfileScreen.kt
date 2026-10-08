package com.example.marvel.ui

import android.text.InputFilter
import android.text.InputType

fun ScreenRenderer.profile() {
    val user = vm.user ?: return
    val isFirstRun = vm.route.screen == "first-run-profile"

    title("Your profile")
    profileRow()

    if (user.isAnonymous) {
        note(
            "Guest session · Your saved archive is linked to this anonymous account. Signing out ends access to these saved items."
        )
    }

    if (isFirstRun) {
        button("Enter the Archives", primary = true) { vm.destination("home") }
    }

    section("Account")
    button("Edit profile") { vm.editProfile() }
    button(if (user.isAnonymous) "End guest session" else "Sign out") { confirmSignOut() }
    actionRow("Settings", "Account, API endpoints, privacy and data credits.") {
        vm.navigate("settings")
    }

    section("Your collection")
    syncStatus()
    dataPair("Favorite dossiers", collectionCount(vm.favorites.size.toLong()))
    dataPair("Booster character cards", collectionCount(vm.collectedCharacters.sumOf { it.copies }))
    dataPair("Boosters owned", collectionCount(vm.boosters.values.sumOf { it.count }))
    dataPair("Saved teams", collectionCount(vm.teams.size.toLong()))
    dataPair("Completed missions", collectionCount(vm.history.size.toLong()))
}

fun ScreenRenderer.editProfile() {
    val user = vm.user ?: return

    title("Edit profile")
    text("Update the name shown in your archive. Your email and profile photo remain unchanged.")

    val editor = vm.profileEditor
    input("Agent name", editor.displayName, changed = editor::changeDisplayName).apply {
        filters = arrayOf(InputFilter.LengthFilter(PROFILE_NAME_MAX_LENGTH))
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    editor.error?.let { error -> state("Profile not updated", error) }
    if (editor.saved) note("Profile updated.")

    button(
        if (editor.isSaving) "Saving profile…" else "Save changes",
        enabled = !editor.isSaving,
        primary = true,
    ) {
        activity.hideKeyboard()
        editor.save(user)
    }
    button("Cancel") { vm.back() }
}

private const val PROFILE_NAME_MAX_LENGTH = 40
