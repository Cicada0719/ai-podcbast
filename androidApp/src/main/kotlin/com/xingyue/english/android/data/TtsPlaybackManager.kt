package com.xingyue.english.android.data

import android.media.MediaPlayer
import java.io.Closeable

interface TtsAudioPlayer {
    fun setDataSource(path: String)
    fun prepare()
    fun start()
    fun stop()
    fun release()
    fun setOnCompletionListener(listener: () -> Unit)
    fun setOnErrorListener(listener: (Throwable) -> Unit)
}

class TtsPlaybackManager(
    private val playerFactory: () -> TtsAudioPlayer = { AndroidTtsAudioPlayer() }
) : Closeable {
    private val lock = Any()
    private var currentPlayer: TtsAudioPlayer? = null

    fun play(audioPath: String) {
        require(audioPath.isNotBlank()) { "audioPath must not be blank" }

        val nextPlayer = playerFactory()
        synchronized(lock) {
            releaseCurrentLocked()
            currentPlayer = nextPlayer
        }

        try {
            nextPlayer.setDataSource(audioPath)
            nextPlayer.prepare()
            nextPlayer.setOnCompletionListener { releaseIfCurrent(nextPlayer) }
            nextPlayer.setOnErrorListener { releaseIfCurrent(nextPlayer) }
            nextPlayer.start()
        } catch (error: Throwable) {
            releaseIfCurrent(nextPlayer)
            throw error
        }
    }

    fun stop() {
        synchronized(lock) {
            releaseCurrentLocked()
        }
    }

    override fun close() {
        stop()
    }

    private fun releaseIfCurrent(player: TtsAudioPlayer) {
        synchronized(lock) {
            if (currentPlayer === player) {
                releaseCurrentLocked()
            }
        }
    }

    private fun releaseCurrentLocked() {
        val player = currentPlayer ?: return
        currentPlayer = null
        runCatching { player.stop() }
        runCatching { player.release() }
    }
}

private class AndroidTtsAudioPlayer : TtsAudioPlayer {
    private val mediaPlayer = MediaPlayer()

    override fun setDataSource(path: String) {
        mediaPlayer.setDataSource(path)
    }

    override fun prepare() {
        mediaPlayer.prepare()
    }

    override fun start() {
        mediaPlayer.start()
    }

    override fun stop() {
        mediaPlayer.stop()
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        mediaPlayer.setOnCompletionListener { listener() }
    }

    override fun setOnErrorListener(listener: (Throwable) -> Unit) {
        mediaPlayer.setOnErrorListener { _, what, extra ->
            listener(IllegalStateException("MediaPlayer error: what=$what extra=$extra"))
            true
        }
    }
}
