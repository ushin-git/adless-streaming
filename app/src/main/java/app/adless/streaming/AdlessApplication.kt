package app.adless.streaming

import android.app.Application
import app.adless.streaming.data.LocalSettingsStore
import app.adless.streaming.data.MusicLibraryStore
import app.adless.streaming.data.SourceRegistry
import app.adless.streaming.data.source.AudiusSource
import app.adless.streaming.data.source.InternetArchiveSource
import app.adless.streaming.data.source.JamendoSource
import app.adless.streaming.data.source.LocalAudioSource
import app.adless.streaming.data.source.WikimediaCommonsSource
import app.adless.streaming.playback.PlaybackController

class AdlessApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}

class AppContainer(application: Application) {
  val sourceRegistry =
    SourceRegistry(
      listOf(
        LocalAudioSource(application),
        JamendoSource(),
        AudiusSource(),
        InternetArchiveSource(),
        WikimediaCommonsSource(),
      )
    )
  val settingsStore = LocalSettingsStore(application)
  val musicLibraryStore = MusicLibraryStore(application)
  val playbackController = PlaybackController(application)
}
