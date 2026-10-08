package com.example.marvel.ui

import com.example.marvel.data.ComicVineEndpoints

fun ScreenRenderer.settings() {
    val user = vm.user

    title("Settings")
    section("Account")
    dataPair(
        "Signed in as",
        if (user?.isAnonymous == true) "Guest session"
        else user?.email?.takeIf { it.isNotBlank() } ?: "Email unavailable",
    )
    button("Edit profile") { vm.editProfile() }
    button(if (user?.isAnonymous == true) "End guest session" else "Sign out") { confirmSignOut() }
    dataPair("App language", "English")

    section("Your data")
    text(
        "Favorites, booster inventory, collected character cards, saved teams and mission history are stored privately under your Firebase account. Recently viewed dossiers and searches stay on this device."
    )
    note(
        if (user?.isAnonymous == true)
            "Your guest collection is stored under this anonymous Firebase account. Signing out ends access to its saved collection."
        else
            "Your account name, email and optional Google photo identify your archive. Signing out keeps your saved collection."
    )
    button("Clear recent dossiers and searches") { vm.clearLocalHistory() }

    section("Comic Vine API endpoints")
    text(
        "Archive records are loaded from the Comic Vine API. Requests include the required API key and JSON format parameters."
    )
    dataPair("Base URL", ComicVineEndpoints.BASE_URL)
    ComicVineEndpoints.all.forEach { endpoint ->
        dataPair(endpoint.name, endpoint.url)
    }

    section("About the Archives")
    text(
        "A comic archive assembled from paper, ink and collected dossiers. Search, discover and recruit."
    )
    section("Data credits")
    text(
        "Comic data and character imagery supplied by Comic Vine. This educational app is not affiliated with Marvel. Mission scenarios and evaluations are created by the app."
    )
}
