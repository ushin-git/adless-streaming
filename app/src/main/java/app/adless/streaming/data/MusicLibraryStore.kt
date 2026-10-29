package app.adless.streaming.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.adless.streaming.domain.MusicTrack
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UserPlaylist(val id: String, val name: String)

class MusicLibraryStore(context: Context) :
  SQLiteOpenHelper(context, "adless_library.db", null, 1) {
  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE liked_tracks (track_key TEXT PRIMARY KEY NOT NULL, track_data TEXT NOT NULL, liked_at INTEGER NOT NULL)"
    )
    db.execSQL(
      "CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, track_data TEXT NOT NULL, played_at INTEGER NOT NULL)"
    )
    db.execSQL(
      "CREATE TABLE playlists (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, created_at INTEGER NOT NULL)"
    )
    db.execSQL(
      "CREATE TABLE playlist_tracks (playlist_id TEXT NOT NULL, position INTEGER NOT NULL, track_key TEXT NOT NULL, track_data TEXT NOT NULL, PRIMARY KEY (playlist_id, track_key), FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE)"
    )
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

  suspend fun setLiked(track: MusicTrack, liked: Boolean): Unit =
    withContext(Dispatchers.IO) {
      if (liked) {
        writableDatabase.insertWithOnConflict(
          "liked_tracks",
          null,
          ContentValues().apply {
            put("track_key", track.stableKey)
            put("track_data", track.toJson())
            put("liked_at", System.currentTimeMillis())
          },
          SQLiteDatabase.CONFLICT_REPLACE,
        )
      } else {
        writableDatabase.delete("liked_tracks", "track_key = ?", arrayOf(track.stableKey))
      }
    }

  suspend fun likedTracks(): List<MusicTrack> =
    withContext(Dispatchers.IO) {
      readableDatabase
        .query("liked_tracks", arrayOf("track_data"), null, null, null, null, "liked_at DESC")
        .use { cursor ->
          buildList { while (cursor.moveToNext()) add(MusicTrack.fromJson(cursor.getString(0))) }
        }
    }

  suspend fun recordPlay(track: MusicTrack) =
    withContext(Dispatchers.IO) {
      writableDatabase.insert(
        "history",
        null,
        ContentValues().apply {
          put("track_data", track.toJson())
          put("played_at", System.currentTimeMillis())
        },
      )
      writableDatabase.execSQL(
        "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY played_at DESC LIMIT 500)"
      )
    }

  suspend fun history(): List<MusicTrack> =
    withContext(Dispatchers.IO) {
      readableDatabase
        .query("history", arrayOf("track_data"), null, null, null, null, "played_at DESC", "100")
        .use { cursor ->
          buildList { while (cursor.moveToNext()) add(MusicTrack.fromJson(cursor.getString(0))) }
        }
    }

  suspend fun createPlaylist(name: String): UserPlaylist? =
    withContext(Dispatchers.IO) {
      val cleaned = name.trim().take(60)
      if (cleaned.isEmpty()) return@withContext null
      val playlist = UserPlaylist(UUID.randomUUID().toString(), cleaned)
      writableDatabase.insert(
        "playlists",
        null,
        ContentValues().apply {
          put("id", playlist.id)
          put("name", playlist.name)
          put("created_at", System.currentTimeMillis())
        },
      )
      playlist
    }

  suspend fun playlists(): List<UserPlaylist> =
    withContext(Dispatchers.IO) {
      readableDatabase
        .query("playlists", arrayOf("id", "name"), null, null, null, null, "created_at DESC")
        .use { cursor ->
          buildList {
            while (cursor.moveToNext()) add(UserPlaylist(cursor.getString(0), cursor.getString(1)))
          }
        }
    }

  suspend fun addToPlaylist(playlistId: String, track: MusicTrack) =
    withContext(Dispatchers.IO) {
      val db = writableDatabase
      val position =
        db
          .rawQuery(
            "SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlist_id = ?",
            arrayOf(playlistId),
          )
          .use {
            if (it.moveToFirst()) it.getInt(0) else 0
          }
      db.insertWithOnConflict(
        "playlist_tracks",
        null,
        ContentValues().apply {
          put("playlist_id", playlistId)
          put("position", position)
          put("track_key", track.stableKey)
          put("track_data", track.toJson())
        },
        SQLiteDatabase.CONFLICT_IGNORE,
      )
    }

  suspend fun playlistTracks(playlistId: String): List<MusicTrack> =
    withContext(Dispatchers.IO) {
      readableDatabase
        .query(
          "playlist_tracks",
          arrayOf("track_data"),
          "playlist_id = ?",
          arrayOf(playlistId),
          null,
          null,
          "position ASC",
        )
        .use { cursor ->
          buildList { while (cursor.moveToNext()) add(MusicTrack.fromJson(cursor.getString(0))) }
        }
    }

  private fun MusicTrack.toJson(): String =
    JSONObject()
      .put("id", id)
      .put("title", title)
      .put("artist", artist)
      .put("album", album ?: JSONObject.NULL)
      .put("artworkUrl", artworkUrl ?: JSONObject.NULL)
      .put("sourceId", sourceId)
      .put("sourcePageUrl", sourcePageUrl)
      .put("licenseName", licenseName ?: JSONObject.NULL)
      .put("licenseUrl", licenseUrl ?: JSONObject.NULL)
      .put("streamUri", streamUri ?: JSONObject.NULL)
      .toString()

  private fun MusicTrack.Companion.fromJson(json: String): MusicTrack {
    val item = JSONObject(json)
    return MusicTrack(
      id = item.getString("id"),
      title = item.getString("title"),
      artist = item.getString("artist"),
      album = item.optString("album").takeIf(String::isNotBlank)?.takeUnless { it == "null" },
      artworkUrl =
        item.optString("artworkUrl").takeIf(String::isNotBlank)?.takeUnless { it == "null" },
      sourceId = item.getString("sourceId"),
      sourcePageUrl = item.getString("sourcePageUrl"),
      licenseName =
        item.optString("licenseName").takeIf(String::isNotBlank)?.takeUnless { it == "null" },
      licenseUrl =
        item.optString("licenseUrl").takeIf(String::isNotBlank)?.takeUnless { it == "null" },
      streamUri =
        item.optString("streamUri").takeIf(String::isNotBlank)?.takeUnless { it == "null" },
    )
  }
}
