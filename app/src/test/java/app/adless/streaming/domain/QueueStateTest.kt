package app.adless.streaming.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueStateTest {
    private fun track(name: String) = MusicTrack(
        id = name,
        title = name,
        artist = "Test artist",
        sourceId = "test",
        sourcePageUrl = "https://example.test/$name",
    )

    @Test
    fun addNextInsertsAfterCurrentWithoutChangingCurrent() {
        val state = QueueState(listOf(track("a"), track("b")), currentIndex = 0).addNext(track("c"))

        assertEquals(listOf("a", "c", "b"), state.tracks.map(MusicTrack::title))
        assertEquals("a", state.current?.title)
    }

    @Test
    fun movingCurrentTrackKeepsItSelected() {
        val state = QueueState(listOf(track("a"), track("b"), track("c")), currentIndex = 1).move(1, 0)

        assertEquals(listOf("b", "a", "c"), state.tracks.map(MusicTrack::title))
        assertEquals("b", state.current?.title)
    }

    @Test
    fun removingTrackBeforeCurrentAdjustsIndex() {
        val state = QueueState(listOf(track("a"), track("b"), track("c")), currentIndex = 2).remove(0)

        assertEquals(1, state.currentIndex)
        assertEquals("c", state.current?.title)
    }

    @Test
    fun repeatOneKeepsCurrentTrackOnNext() {
        val before = QueueState(listOf(track("a"), track("b")), currentIndex = 1, repeatMode = RepeatMode.ONE)

        assertSame(before, before.next())
    }

    @Test
    fun shuffleSelectsAnotherTrack() {
        val state = QueueState(listOf(track("a"), track("b"), track("c")), currentIndex = 1, shuffle = true).next()

        assertTrue(state.currentIndex in listOf(0, 2))
    }

    @Test
    fun emptyQueueHasNoCurrentTrack() {
        val state = QueueState().next()

        assertFalse(state.tracks.isNotEmpty())
        assertEquals(null, state.current)
        assertEquals(-1, state.currentIndex)
    }
}

