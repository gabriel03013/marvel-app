package com.example.marvel.ui

import android.app.AlertDialog

fun ScreenRenderer.confirmSignOut() {
    if (vm.user?.isAnonymous != true) {
        activity.signOut()
        return
    }

    AlertDialog.Builder(activity)
        .setTitle("End guest session?")
        .setMessage(
            "Your saved collection is tied to this guest account. Signing out ends access to those items."
        )
        .setNegativeButton("Keep browsing", null)
        .setPositiveButton("End session") { _, _ -> activity.signOut() }
        .show()
}
