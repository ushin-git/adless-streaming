package app.adless.streaming.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

data class PlaybackItem(val uri: Uri, val title: String, val artist: String)

class PlaybackController(context: Context) {
  private val appContext = context.applicationContext
  private val mainExecutor: Executor = Executor { command ->
    Handler(Looper.getMainLooper()).post(command)
  }
  private val token =
    SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
  private val future: ListenableFuture<MediaController> =
    MediaController.Builder(appContext, token).buildAsync()

  fun setListener(listener: (MediaController?) -> Unit) {
    future.addListener(
      { listener(runCatching { future.get() }.getOrNull()) },
      mainExecutor,
    )
  }

  fun setQueue(
    items: List<PlaybackItem>,
    startIndex: Int,
    startPositionMs: Long,
    startPlayback: Boolean,
  ) {
    if (items.isEmpty()) return
    future.addListener(
      {
        runCatching {
          future.get().apply {
            val wasPlaying = playWhenReady
            val mediaItems = items.map { item ->
              MediaItem.Builder()
                .setUri(item.uri)
                .setMediaMetadata(
                  MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist).build()
                )
                .build()
            }
            setMediaItems(
              mediaItems,
              startIndex.coerceIn(mediaItems.indices),
              startPositionMs.coerceAtLeast(0L),
            )
            prepare()
            if (startPlayback || wasPlaying) play() else pause()
          }
        }
      },
      mainExecutor,
    )
  }

  fun clearQueue() {
    future.addListener(
      {
        runCatching {
          future.get().apply {
            pause()
            clearMediaItems()
          }
        }
      },
      mainExecutor,
    )
  }

  fun release() {
    MediaController.releaseFuture(future)
  }
}
