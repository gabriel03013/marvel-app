package com.example.marvel.data

data class ComicVineEndpoint(
    val name: String,
    val path: String,
) {
    val url: String
        get() = "${ComicVineEndpoints.BASE_URL}$path/"
}

object ComicVineEndpoints {
    const val BASE_URL = "https://comicvine.gamespot.com/api/"

    val all =
        listOf(
            ComicVineEndpoint("Search", "search"),
            ComicVineEndpoint("Characters", "characters"),
            ComicVineEndpoint("Issues", "issues"),
            ComicVineEndpoint("Volumes", "volumes"),
            ComicVineEndpoint("Teams", "teams"),
            ComicVineEndpoint("Story arcs", "story_arcs"),
            ComicVineEndpoint("Publishers", "publishers"),
            ComicVineEndpoint("Powers", "powers"),
            ComicVineEndpoint("Locations", "locations"),
        )
}
