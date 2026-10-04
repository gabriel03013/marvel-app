package com.example.marvel.data

import com.example.marvel.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.SocketTimeoutException
import java.io.IOException

class ComicVineRepository {
    private val cache = object : LinkedHashMap<String, Pair<Long, ArchiveItem>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, ArchiveItem>>?) = size > 60
    }
    suspend fun list(kind: String, query: String = "", offset: Int = 0, sort: String = "name:asc"): Page {
        val params = mutableMapOf("limit" to "20", "offset" to offset.toString())
        val endpoint = if (query.isNotBlank()) {
            params["query"] = query.trim(); params["resources"] = kind; "search"
        } else { params["sort"] = sort; plural(kind) }
        params["field_list"] = "id,name,real_name,image,deck,publisher,issue_number,volume,cover_date,start_year,count_of_issues,count_of_team_members,count_of_issue_appearances,count_of_isssue_appearances,first_appeared_in_issue,api_detail_url"
        val json = request(endpoint, params)
        val records = json.optJSONArray("results").items().map { ArchiveItem.fromJson(it, kind) }
        return Page(records, offset, json.optInt("number_of_page_results", records.size), json.optInt("number_of_total_results", records.size))
    }
    suspend fun detail(kind: String, id: Int, refresh: Boolean = false): ArchiveItem {
        require(id > 0) { "This record has no valid archive identifier." }
        val key = "$kind/$id"
        synchronized(cache) { cache[key] }?.let { if (!refresh && System.currentTimeMillis() - it.first < 300_000) return it.second }
        val code = codes[kind] ?: error("This archive type is unavailable.")
        val record = ArchiveItem.fromJson(request("$kind/$code-$id", emptyMap()).getJSONObject("results"), kind)
        synchronized(cache) { cache[key] = System.currentTimeMillis() to record }
        return record
    }
    private suspend fun request(endpoint: String, options: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        check(BuildConfig.COMIC_VINE_API_KEY.isNotBlank()) { "Comic Vine is not configured. Add COMIC_VINE_API_KEY to local.properties, then rebuild the app." }
        val params = options + mapOf("api_key" to BuildConfig.COMIC_VINE_API_KEY, "format" to "json")
        val query = params.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        val conn = URI("https://comicvine.gamespot.com/api/$endpoint/?$query").toURL().openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000; conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "SHIELDArchivesAndroid/1.0 (educational comic explorer)")
            conn.setRequestProperty("Accept", "application/json")
            when (conn.responseCode) {
                401, 403 -> error("Comic Vine denied access. Check the API key and try again.")
                429 -> error("The archive request limit was reached. Wait a moment before retrying.")
                in 200..299 -> Unit
                else -> error("The archive server is unavailable. Try again shortly.")
            }
            val j = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            check(j.optInt("status_code") == 1) { "Comic Vine could not complete this request. Check your configuration and retry." }
            j
        } catch (e: SocketTimeoutException) { throw IOException("The archive took too long to respond. Check your connection and retry.", e) }
        catch (e: IOException) { throw IOException("Cannot reach Comic Vine. Check your connection and retry.", e) }
        finally { conn.disconnect() }
    }
    companion object {
        val codes = mapOf("character" to "4005", "power" to "4035", "team" to "4060", "story_arc" to "4045", "issue" to "4000", "volume" to "4050", "publisher" to "4010", "location" to "4020", "person" to "4040")
        fun plural(kind: String) = when(kind) { "story_arc" -> "story_arcs"; else -> "${kind}s" }
        private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    }
}
