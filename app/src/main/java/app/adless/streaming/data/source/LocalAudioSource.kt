package app.adless.streaming.data.source

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import app.adless.streaming.domain.MusicSource
import app.adless.streaming.domain.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioSource(private val context: Context) : MusicSource {
  override val id = "local"
  override val displayName = "On this device"
  override val description = "Audio files already on your device. Nothing is uploaded."
  override val supportsOffline = true

  override suspend fun search(query: String): List<MusicTrack> =
    withContext(Dispatchers.IO) {
      val permission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
      if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        throw SecurityException("Allow audio access to search music on this device.")
      }
      val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      val projection =
        arrayOf(
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.TITLE,
          MediaStore.Audio.Media.ARTIST,
          MediaStore.Audio.Media.ALBUM,
          MediaStore.Audio.Media.IS_MUSIC,
        )
      val selection =
        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.TITLE} LIKE ?"
      val args = arrayOf("%${query.replace("%", "\\%").replace("_", "\\_")}%")
      val tracks = mutableListOf<MusicTrack>()
      context.contentResolver
        .query(
          collection,
          projection,
          selection,
          args,
          "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )
        ?.use { cursor ->
          val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
          val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
          val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
          val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
          while (cursor.moveToNext()) {
            val mediaUri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
            tracks +=
              MusicTrack(
                id = mediaUri.toString(),
                title = cursor.getString(titleColumn).orEmpty(),
                artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown artist" },
                album = cursor.getString(albumColumn),
                sourceId = id,
                sourcePageUrl = mediaUri.toString(),
                streamUri = mediaUri.toString(),
              )
            if (tracks.size == 100) break
          }
        }
      tracks
    }

  override suspend fun resolveStream(track: MusicTrack): String = requireNotNull(track.streamUri)
}
