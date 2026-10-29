package app.adless.streaming.domain

import kotlin.random.Random

enum class RepeatMode {
  OFF,
  ALL,
  ONE,
}

data class QueueState(
  val tracks: List<MusicTrack> = emptyList(),
  val currentIndex: Int = -1,
  val shuffle: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.OFF,
) {
  val current: MusicTrack?
    get() = tracks.getOrNull(currentIndex)

  fun add(track: MusicTrack): QueueState = copy(tracks = tracks + track)

  fun addNext(track: MusicTrack): QueueState {
    if (tracks.isEmpty() || currentIndex !in tracks.indices)
      return copy(tracks = listOf(track) + tracks, currentIndex = 0)
    val updated = tracks.toMutableList().apply { add(currentIndex + 1, track) }
    return copy(tracks = updated)
  }

  fun remove(index: Int): QueueState {
    if (index !in tracks.indices) return this
    val updated = tracks.toMutableList().apply { removeAt(index) }
    val nextIndex =
      when {
        updated.isEmpty() -> -1
        index < currentIndex -> currentIndex - 1
        index == currentIndex -> currentIndex.coerceAtMost(updated.lastIndex)
        else -> currentIndex
      }
    return copy(tracks = updated, currentIndex = nextIndex)
  }

  fun move(from: Int, to: Int): QueueState {
    if (from !in tracks.indices || to !in tracks.indices || from == to) return this
    val updated = tracks.toMutableList().apply { add(to, removeAt(from)) }
    val nextIndex =
      when {
        currentIndex == from -> to
        from < currentIndex && to >= currentIndex -> currentIndex - 1
        from > currentIndex && to <= currentIndex -> currentIndex + 1
        else -> currentIndex
      }
    return copy(tracks = updated, currentIndex = nextIndex)
  }

  fun next(random: Random = Random.Default): QueueState {
    if (tracks.isEmpty()) return copy(currentIndex = -1)
    if (repeatMode == RepeatMode.ONE && currentIndex in tracks.indices) return this
    val index =
      when {
        shuffle && tracks.size > 1 -> (tracks.indices - currentIndex).random(random)
        currentIndex < tracks.lastIndex -> currentIndex + 1
        repeatMode == RepeatMode.ALL -> 0
        else -> currentIndex
      }
    return copy(currentIndex = index)
  }

  fun previous(): QueueState =
    when {
      tracks.isEmpty() -> copy(currentIndex = -1)
      currentIndex > 0 -> copy(currentIndex = currentIndex - 1)
      repeatMode == RepeatMode.ALL -> copy(currentIndex = tracks.lastIndex)
      else -> this
    }
}
