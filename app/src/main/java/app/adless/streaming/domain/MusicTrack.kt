package app.adless.streaming.domain

data class MusicTrack(
  val id: String,
  val title: String,
  val artist: String,
  val album: String? = null,
  val artworkUrl: String? = null,
  val sourceId: String,
  val sourcePageUrl: String,
  val licenseName: String? = null,
  val licenseUrl: String? = null,
  val streamUri: String? = null,
) {
  val stableKey: String
    get() = "$sourceId:$id"

  companion object {}
}

data class SourceSearch(
  val sourceId: String,
  val tracks: List<MusicTrack> = emptyList(),
  val error: String? = null,
)

enum class ThemeMode {
  LIGHT,
  DARK,
  AMOLED,
}

interface MusicSource {
  val id: String
  val displayName: String
  val description: String
  val requiresDeveloperConfig: Boolean
    get() = false

  val supportsOffline: Boolean
    get() = false

  suspend fun search(query: String): List<MusicTrack>

  suspend fun resolveStream(track: MusicTrack): String
}
