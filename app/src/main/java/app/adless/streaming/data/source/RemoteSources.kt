package app.adless.streaming.data.source

import android.net.Uri
import app.adless.streaming.BuildConfig
import app.adless.streaming.data.HttpJson
import app.adless.streaming.domain.MusicSource
import app.adless.streaming.domain.MusicTrack
import org.json.JSONObject

private fun JSONObject.string(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }

internal fun parseJamendoTrack(item: JSONObject): MusicTrack? {
    val id = item.string("id") ?: return null
    val title = item.string("name") ?: return null
    val licenseUrl = item.string("license_ccurl") ?: item.string("license_url")
    if (!HttpJson.licenseAllowed(licenseUrl)) return null
    return MusicTrack(
        id = id,
        title = title,
        artist = item.optJSONObject("artist")?.string("name") ?: "Unknown artist",
        album = item.optJSONObject("album")?.string("name"),
        artworkUrl = item.string("album_image"),
        sourceId = "jamendo",
        sourcePageUrl = item.string("shareurl") ?: "https://www.jamendo.com/track/$id",
        licenseName = item.string("license_ccsa") ?: item.string("license_ccnd"),
        licenseUrl = licenseUrl,
    )
}

internal fun parseAudiusTrack(item: JSONObject): MusicTrack? {
    val id = item.string("id") ?: return null
    val title = item.string("title") ?: return null
    val user = item.optJSONObject("user")
    val licenseUrl = item.string("license_url")
    val licenseName = item.string("license")
    if (licenseUrl != null && !HttpJson.licenseAllowed(licenseUrl)) return null
    val normalizedLicense = licenseName?.lowercase()?.replace('-', ' ').orEmpty()
    val restrictedTerms = listOf("non commercial", "noncommercial", "by nc", "no derivatives", "by nd", "all rights reserved")
    if (restrictedTerms.any { normalizedLicense.contains(it) }) return null
    return MusicTrack(
        id = id,
        title = title,
        artist = user?.string("name") ?: "Unknown artist",
        artworkUrl = item.optJSONObject("artwork")?.string("480x480"),
        sourceId = "audius",
        sourcePageUrl = item.string("permalink")?.let { "https://audius.co$it" } ?: "https://audius.co",
        licenseName = licenseName ?: licenseUrl?.substringAfterLast('/'),
        licenseUrl = licenseUrl,
    )
}

class JamendoSource(private val clientId: String = BuildConfig.JAMENDO_CLIENT_ID) : MusicSource {
    override val id = "jamendo"
    override val displayName = "Jamendo"
    override val description = "Independent music with track-specific Creative Commons licenses."
    override val requiresDeveloperConfig = true
    override val supportsOffline = false

    override suspend fun search(query: String): List<MusicTrack> {
        require(clientId.isNotBlank()) { "Add a Jamendo developer client ID in local.properties to enable this source." }
        val url = HttpJson.query("https://api.jamendo.com/v3.0/tracks/", mapOf(
            "client_id" to clientId, "format" to "json", "limit" to "30", "search" to query,
            "include" to "licenses", "audioformat" to "mp32", "order" to "relevance",
        ))
        val results = HttpJson.get(url).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index -> results.optJSONObject(index)?.let(::parseJamendoTrack) }
    }

    override suspend fun resolveStream(track: MusicTrack): String {
        require(clientId.isNotBlank()) { "Jamendo is not configured." }
        val url = HttpJson.query("https://api.jamendo.com/v3.0/tracks/file/", mapOf(
            "client_id" to clientId, "id" to track.id, "action" to "stream", "audioformat" to "mp32",
        ))
        return url
    }
}

class AudiusSource : MusicSource {
    override val id = "audius"
    override val displayName = "Audius"
    override val description = "Open catalog; content may carry creator-selected alternative licenses."
    override val supportsOffline = false

    override suspend fun search(query: String): List<MusicTrack> {
        val response = HttpJson.get(HttpJson.query("https://api.audius.co/v1/tracks/search", mapOf("query" to query, "limit" to "30")))
        val results = response.optJSONArray("data") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index -> results.optJSONObject(index)?.let(::parseAudiusTrack) }
    }

    override suspend fun resolveStream(track: MusicTrack): String {
        require(track.licenseUrl == null || HttpJson.licenseAllowed(track.licenseUrl)) { "This track's license is not supported." }
        return "https://api.audius.co/v1/tracks/${Uri.encode(track.id)}/stream"
    }
}

class InternetArchiveSource : MusicSource {
    override val id = "internet-archive"
    override val displayName = "Internet Archive"
    override val description = "Public audio archive; only items with an explicit compatible license are shown."
    override val supportsOffline = false

    override suspend fun search(query: String): List<MusicTrack> {
        val searchUrl = HttpJson.query("https://archive.org/advancedsearch.php", mapOf(
            "q" to "mediatype:audio AND title:\"$query\"", "fl[]" to "identifier,title,creator", "rows" to "15", "output" to "json",
        ))
        val docs = HttpJson.get(searchUrl).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
        val found = mutableListOf<MusicTrack>()
        for (index in 0 until docs.length()) {
            val doc = docs.optJSONObject(index) ?: continue
            val identifier = doc.string("identifier") ?: continue
            val metadata = runCatching { HttpJson.get("https://archive.org/metadata/${Uri.encode(identifier)}") }.getOrNull() ?: continue
            val meta = metadata.optJSONObject("metadata") ?: continue
            val licenseUrl = meta.string("licenseurl")
            if (!HttpJson.licenseAllowed(licenseUrl)) continue
            val files = metadata.optJSONArray("files") ?: continue
            val audioFile = (0 until files.length()).asSequence().mapNotNull(files::optJSONObject).firstOrNull { file ->
                val name = file.string("name")?.lowercase().orEmpty()
                name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".m4a")
            } ?: continue
            val fileName = audioFile.string("name") ?: continue
            val title = meta.string("title") ?: doc.string("title") ?: fileName.substringBeforeLast('.')
            val creatorValue = meta.opt("creator")
            val artist = when (creatorValue) {
                is org.json.JSONArray -> creatorValue.optString(0, "Unknown creator")
                is String -> creatorValue
                else -> "Unknown creator"
            }
            found += MusicTrack(
                id = "$identifier/$fileName", title = title, artist = artist, sourceId = id,
                sourcePageUrl = "https://archive.org/details/$identifier", licenseName = licenseUrl ?: "Public domain",
                licenseUrl = licenseUrl, streamUri = "https://archive.org/download/${Uri.encode(identifier)}/${Uri.encode(fileName)}",
            )
        }
        return found
    }

    override suspend fun resolveStream(track: MusicTrack): String = requireNotNull(track.streamUri)
}

class WikimediaCommonsSource : MusicSource {
    override val id = "wikimedia-commons"
    override val displayName = "Wikimedia Commons"
    override val description = "Free-licensed audio files; attribution and license are shown per file."
    override val supportsOffline = false

    override suspend fun search(query: String): List<MusicTrack> {
        val url = HttpJson.query("https://commons.wikimedia.org/w/api.php", mapOf(
            "action" to "query", "generator" to "search", "gsrsearch" to "filemime:audio music $query",
            "gsrnamespace" to "6", "gsrlimit" to "20", "prop" to "imageinfo", "iiprop" to "url|extmetadata", "format" to "json",
        ))
        val pages = HttpJson.get(url, "AdlessStreaming/0.1 (Android; contact: project README)").optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
        val result = mutableListOf<MusicTrack>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val metadata = info.optJSONObject("extmetadata") ?: continue
            val streamUrl = info.string("url") ?: continue
            val audioPath = Uri.parse(streamUrl).lastPathSegment?.lowercase().orEmpty()
            if (audioPath.substringAfterLast('.', "") !in setOf("mp3", "ogg", "oga", "opus", "flac", "wav", "m4a", "aac")) continue
            val license = metadata.optJSONObject("LicenseUrl")?.string("value")
            if (!HttpJson.licenseAllowed(license)) continue
            val artist = metadata.optJSONObject("Artist")?.string("value")?.replace(Regex("<[^>]*>"), "") ?: "Unknown creator"
            val title = page.string("title")?.removePrefix("File:") ?: continue
            result += MusicTrack(
                id = page.string("pageid") ?: title, title = title.substringBeforeLast('.'), artist = artist,
                sourceId = id, sourcePageUrl = info.string("descriptionurl") ?: "https://commons.wikimedia.org",
                licenseName = metadata.optJSONObject("LicenseShortName")?.string("value"), licenseUrl = license,
                streamUri = streamUrl,
            )
        }
        return result
    }

    override suspend fun resolveStream(track: MusicTrack): String = requireNotNull(track.streamUri)
}

