package com.xingyue.english.android.data

import androidx.test.core.app.ApplicationProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidMediaEngineInstrumentedTest {
    @Test
    fun ffmpegExtractsSubtitleStreamToSrtFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = File(context.cacheDir, "media-engine-test").apply { mkdirs() }
        val srt = File(dir, "track.srt").apply {
            writeText(
                """
                1
                00:00:00,000 --> 00:00:01,000
                Practice listening every day.
                """.trimIndent()
            )
        }

        val extracted = AndroidMediaEngine.extractSubtitleTrack(srt.absolutePath, File(dir, "out.srt").absolutePath)

        assertNotNull(extracted)
        assertTrue(extracted.orEmpty().contains("Practice listening every day."))
    }

    @Test
    fun ffmpegExtractsEmbeddedSubtitleTrackFromMkv() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = File(context.cacheDir, "media-engine-embedded-test").apply { mkdirs() }
        val srt = File(dir, "embedded-track.srt").apply {
            writeText(
                """
                1
                00:00:00,000 --> 00:00:01,000
                Embedded subtitle extraction works.
                """.trimIndent()
            )
        }
        val mkv = File(dir, "with-subtitle.mkv").apply { delete() }

        val muxed = executeFfmpeg(
            "-y",
            "-f",
            "lavfi",
            "-i",
            "color=c=black:s=160x90:d=1:r=1",
            "-i",
            srt.absolutePath,
            "-c:v",
            "mpeg4",
            "-c:s",
            "srt",
            "-shortest",
            mkv.absolutePath
        )

        assertTrue("FFmpeg should create an MKV file with an embedded subtitle track", muxed && mkv.length() > 0)

        val extracted = AndroidMediaEngine.extractSubtitleTrack(mkv.absolutePath, File(dir, "embedded-out.srt").absolutePath)

        assertNotNull(extracted)
        assertTrue(extracted.orEmpty().contains("Embedded subtitle extraction works."))
    }

    private fun executeFfmpeg(vararg args: String): Boolean {
        val completed = CountDownLatch(1)
        val success = AtomicReference(false)
        val session = FFmpegKit.executeAsync(args.joinToString(" ") { it.ffmpegQuote() }) { finished ->
            success.set(ReturnCode.isSuccess(finished.returnCode))
            completed.countDown()
        }
        return if (!completed.await(30, TimeUnit.SECONDS)) {
            FFmpegKit.cancel(session.sessionId)
            false
        } else {
            success.get()
        }
    }

    private fun String.ffmpegQuote(): String =
        if (any { it.isWhitespace() || it == '"' }) "\"${replace("\"", "\\\"")}\"" else this
}
