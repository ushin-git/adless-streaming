package app.adless.streaming.data

import androidx.core.net.toUri
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object HttpJson {
  suspend fun get(url: String, userAgent: String = "AdlessStreaming/0.1 (Android)"): JSONObject =
    withContext(Dispatchers.IO) {
      val connection = URL(url).openConnection() as HttpURLConnection
      try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)
        val status = connection.responseCode
        if (status !in 200..299) error("Service returned HTTP $status")
        connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
      } finally {
        connection.disconnect()
      }
    }

  fun query(base: String, parameters: Map<String, String>): String =
    base
      .toUri()
      .buildUpon()
      .apply {
        parameters.forEach { (key, value) -> appendQueryParameter(key, value) }
      }
      .build()
      .toString()

  fun licenseAllowed(url: String?): Boolean {
    val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return false
    if (
      uri.scheme?.lowercase() !in setOf("https", "http") ||
        uri.host?.lowercase() != "creativecommons.org"
    ) {
      return false
    }
    val path = uri.path.orEmpty().trim('/').split('/').map(String::lowercase)
    if (path.size != 3) return false
    val version = path[2]
    if (!version.matches(Regex("[1-4](?:\\.[0-9])?"))) return false
    return (path[0] == "licenses" && path[1] in setOf("by", "by-sa")) ||
      (path[0] == "publicdomain" && path[1] in setOf("zero", "mark"))
  }
}
