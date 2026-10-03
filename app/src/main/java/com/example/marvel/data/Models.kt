package com.example.marvel.data

import org.json.JSONArray
import org.json.JSONObject

/** API records retain only requested fields. Unknown values remain absent. */
data class ArchiveItem(val id: Int, val kind: String, val name: String, val image: String = "", val deck: String = "", val raw: String = "{}") {
    val json get() = JSONObject(raw)
    fun text(key: String): String = json.optString(key).takeUnless { it == "null" }.orEmpty()
    fun reference(key: String): ArchiveItem? = json.optJSONObject(key)?.let { fromJson(it, relationKind(key)) }
    fun related(key: String): List<ArchiveItem> = json.optJSONArray(key).items().map { fromJson(it, relationKind(key)) }.filter { it.id > 0 }
    fun map(): Map<String, Any> {
        // Save report evidence rather than enormous issue-credit/description payloads.
        val snapshot = JSONObject()
        for (key in listOf("id", "name", "real_name", "publisher", "origin", "powers", "teams", "first_appeared_in_issue", "count_of_issue_appearances")) {
            if (json.has(key)) snapshot.put(key, json.opt(key))
        }
        return mapOf("id" to id, "kind" to kind, "name" to name, "image" to image, "deck" to deck.take(1200), "raw" to snapshot.toString())
    }
    companion object {
        fun fromJson(j: JSONObject, kind: String): ArchiveItem = ArchiveItem(j.optInt("id"), kind, j.optString("name").takeUnless { it.isBlank() || it == "null" } ?: j.optJSONObject("volume")?.optString("name")?.let { "$it #${j.optString("issue_number")}" } ?: "Untitled record", j.optJSONObject("image")?.optString("medium_url").orEmpty(), j.optString("deck").takeUnless { it == "null" }.orEmpty(), j.toString())
        fun fromMap(m: Map<String, Any?>): ArchiveItem = ArchiveItem((m["id"] as? Number)?.toInt() ?: 0, m["kind"] as? String ?: "character", m["name"] as? String ?: "Untitled record", m["image"] as? String ?: "", m["deck"] as? String ?: "", m["raw"] as? String ?: "{}")
    }
}
fun JSONArray?.items(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
fun relationKind(key: String) = when {
    key.contains("character") || key in listOf("members", "friends", "enemies") -> "character"
    key.contains("power") -> "power"
    key.contains("team") -> "team"
    key.contains("story_arc") -> "story_arc"
    key.contains("volume") -> "volume"
    key.contains("publisher") -> "publisher"
    key.contains("location") -> "location"
    key.contains("issue") -> "issue"
    else -> "person"
}
data class Page(val items: List<ArchiveItem>, val offset: Int, val consumed: Int, val total: Int) { val hasMore get() = offset + consumed < total }
data class Mission(val id: String, val title: String, val description: String, val focus: List<String>, val size: Int = 4)
val missions = listOf(
    Mission("cosmic", "Cosmic Threat", "An unknown force is approaching Earth. Assemble a team with varied powers to investigate and protect the planet.", listOf("Energy Manipulation", "Flight", "Super Strength")),
    Mission("technology", "Technology Crisis", "A compromised network threatens the city. Recruit specialists who can investigate, adapt and protect civilians.", listOf("Intellect", "Gadgets", "Electricity Control")),
    Mission("mutant", "Mutant Incident", "A volatile incident calls for a coordinated response. Bring complementary abilities and a plan for safe evacuation.", listOf("Telepathy", "Telekinesis", "Healing")),
    Mission("city", "City Under Attack", "Protect neighborhoods and contain an escalating threat. Balance mobility, defense and field awareness.", listOf("Agility", "Super Strength", "Flight")),
    Mission("custom", "Build Your Own Mission", "Define your own briefing and assemble the team you want to send.", emptyList())
)
data class SavedTeam(val id: String, val name: String, val missionId: String, val briefing: String, val members: List<ArchiveItem>, val createdAt: Long) {
    fun map(): Map<String, Any> = mapOf("name" to name, "missionId" to missionId, "briefing" to briefing, "members" to members.map { it.map() }, "createdAt" to createdAt)
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, m: Map<String, Any?>) = SavedTeam(id, m["name"] as? String ?: "Untitled team", m["missionId"] as? String ?: "custom", m["briefing"] as? String ?: "", (m["members"] as? List<Map<String, Any?>>).orEmpty().map(ArchiveItem::fromMap), (m["createdAt"] as? Number)?.toLong() ?: 0L)
    }
}
data class Evaluation(val score: Int, val knownMembers: Int, val uniquePowers: List<String>, val matchedFocus: List<String>)
fun evaluate(members: List<ArchiveItem>, mission: Mission): Evaluation {
    val powers = members.flatMap { it.related("powers") }.map { it.name }.distinctBy { it.lowercase() }.sorted()
    val known = members.count { it.related("powers").isNotEmpty() }
    val matches = mission.focus.filter { focus -> powers.any { it.equals(focus, true) } }
    // Coverage, documented power diversity and briefing fit. No invented strength ranking.
    val coverage = 40 * members.size.coerceAtMost(mission.size) / mission.size
    val diversity = 40 * powers.size.coerceAtMost(8) / 8
    val fit = if (mission.focus.isEmpty()) 20 * known / members.size.coerceAtLeast(1) else 20 * matches.size / mission.focus.size
    return Evaluation(coverage + diversity + fit, known, powers, matches)
}

fun suggestedRole(item: ArchiveItem): String {
    val powers = item.related("powers").map { it.name.lowercase() }
    return when {
        powers.any { it.contains("intellect") || it.contains("telepathy") } -> "Reconnaissance / coordination"
        powers.any { it.contains("healing") || it.contains("force field") } -> "Support / protection"
        powers.any { it.contains("flight") || it.contains("speed") || it.contains("teleport") } -> "Mobility / evacuation"
        powers.any { it.contains("strength") || it.contains("energy") } -> "Field response"
        else -> "Role not inferred from available powers"
    }
}
