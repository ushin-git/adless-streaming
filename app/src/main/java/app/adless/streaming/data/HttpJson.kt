package app.adless.streaming.data

import androidx.core.net.toUri
import java.net.HttpURLConnection
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
    val value = url?.lowercase().orEmpty()
    return value.contains("creativecommons.org/licenses/by/") ||
      value.contains("creativecommons.org/licenses/by-sa/") ||
      value.contains("creativecommons.org/publicdomain/zero/") ||
      value.contains("creativecommons.org/publicdomain/mark/")
  }
}
