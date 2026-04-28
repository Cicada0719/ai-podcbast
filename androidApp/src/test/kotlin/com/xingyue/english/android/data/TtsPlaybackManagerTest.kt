package com.xingyue.english.android.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TtsPlaybackManagerTest {
    @Test
    fun playReleasesPreviousPlayerBeforeStartingNext() {
        val first = FakeAudioPlayer()
        val second = FakeAudioPlayer()
        val players = ArrayDeque(listOf(first, second))
        val manager = TtsPlaybackManager { players.removeFirst() }

        manager.play("first.mp3")
        manager.play("second.mp3")

        assertEquals("first.mp3", first.path)
        assertTrue(first.stopped)
        assertTrue(first.released)
        assertEquals("second.mp3", second.path)
        assertTrue(second.started)
        assertFalse(second.released)
    }

    @Test
    fun completionReleasesCurrentPlayer() {
        val player = FakeAudioPlayer()
        val manager = TtsPlaybackManager { player }

        manager.play("speech.mp3")
        player.complete()

        assertTrue(player.stopped)
        assertTrue(player.released)
    }

    @Test
    fun playFailureReleasesFailedPlayer() {
        val player = FakeAudioPlayer(failOnPrepare = true)
        val manager = TtsPlaybackManager { player }

        assertFailsWith<IllegalStateException> {
            manager.play("broken.mp3")
        }

        assertTrue(player.stopped)
        assertTrue(player.released)
    }

    private class FakeAudioPlayer(
        private val failOnPrepare: Boolean = false
    ) : TtsAudioPlayer {
        var path: String = ""
        var started = false
        var stopped = false
        var released = false
        private var completionListener: (() -> Unit)? = null

        override fun setDataSource(path: String) {
            this.path = path
        }

        override fun prepare() {
            if (failOnPrepare) error("prepare failed")
        }

        override fun start() {
            started = true
        }

        override fun stop() {
            stopped = true
        }

        override fun release() {
            released = true
        }

        override fun setOnCompletionListener(listener: () -> Unit) {
            completionListener = listener
        }

        override fun setOnErrorListener(listener: (Throwable) -> Unit) = Unit

        fun complete() {
            completionListener?.invoke()
        }
    }
}
