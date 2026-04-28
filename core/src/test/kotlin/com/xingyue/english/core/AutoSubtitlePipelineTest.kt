package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AutoSubtitlePipelineTest {
    @Test
    fun importedSrtCreatesBilingualCaptionWithoutAutoSavingWords() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "srt-1",
                    title = "scene.srt",
                    kind = SourceType.SUBTITLE,
                    extension = "srt",
                    originalText = """
                        1
                        00:00:01,000 --> 00:00:03,000
                        Repetition builds durable memory.
                    """.trimIndent()
                )
            )
        )
        val words = InMemoryLearningWordRepository()
        val captions = InMemoryBilingualCaptionRepository()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = captions,
            learningWordRepository = words,
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String = "重复建立持久记忆。"
                override fun lookup(word: String): WordDefinition = error("automatic import must not lookup or save words")
            }
        )

        val result = pipeline.processImportedItem("srt-1")

        assertEquals(CaptionSource.IMPORTED_SUBTITLE, result.source)
        assertTrue(result.wordCount > 0)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("srt-1")?.status)
        assertEquals(result.wordCount, store.get("srt-1")?.wordCount)
        assertTrue(store.get("srt-1")?.statusMessage.orEmpty().contains("点选字幕词"))
        assertEquals("重复建立持久记忆。", captions.findByContentId("srt-1")?.cues?.single()?.chinese)
        assertTrue(words.all().isEmpty())
    }

    @Test
    fun documentImportCreatesBilingualDocumentCaptionOnly() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "doc-1",
                    title = "notes.txt",
                    kind = SourceType.DOCUMENT,
                    extension = "txt",
                    originalText = "Curiosity makes listening practice sustainable. Reflection improves retention."
                )
            )
        )
        val captions = InMemoryBilingualCaptionRepository()
        val words = InMemoryLearningWordRepository()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = captions,
            learningWordRepository = words,
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String = "自动生成中文译文。"
                override fun lookup(word: String): WordDefinition = error("automatic import must not lookup or save words")
            }
        )

        val result = pipeline.processImportedItem("doc-1")

        assertEquals(CaptionSource.DOCUMENT, result.source)
        assertTrue(captions.findByContentId("doc-1")?.cues.orEmpty().isNotEmpty())
        assertTrue(captions.findByContentId("doc-1")?.cues.orEmpty().all { it.chinese.isNotBlank() })
        assertEquals(result.wordCount, store.get("doc-1")?.wordCount)
        assertTrue(words.all().isEmpty())
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("doc-1")?.status)
        assertTrue(store.get("doc-1")?.statusMessage.orEmpty().contains("双语已生成"))
    }

    @Test
    fun jsonDocumentCreatesBilingualCaptionWithoutAutoSavingWords() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "json-1",
                    title = "captions.json",
                    kind = SourceType.DOCUMENT,
                    extension = "json",
                    originalText = """
                        {
                          "cues": [
                            {
                              "id": "json-a",
                              "startMs": 1000,
                              "endMs": 2600,
                              "english": "Focused listening improves retention.",
                              "chinese": ""
                            },
                            {
                              "start": 3.0,
                              "end": 5.2,
                              "text": "Shadowing turns input into speech.\n跟读把输入变成输出。"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        )
        val captions = InMemoryBilingualCaptionRepository()
        val words = InMemoryLearningWordRepository()
        val translated = mutableListOf<String>()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = captions,
            learningWordRepository = words,
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String {
                    translated += english
                    return "专注听力提升记忆保持。"
                }
                override fun lookup(word: String): WordDefinition = error("automatic import must not lookup or save words")
            }
        )

        val result = pipeline.processImportedItem("json-1")
        val cues = captions.findByContentId("json-1")?.cues.orEmpty()

        assertEquals(CaptionSource.DOCUMENT, result.source)
        assertEquals(2, cues.size)
        assertEquals("专注听力提升记忆保持。", cues.first().chinese)
        assertEquals("跟读把输入变成输出。", cues[1].chinese)
        assertEquals(listOf("Focused listening improves retention."), translated)
        assertEquals(result.wordCount, store.get("json-1")?.wordCount)
        assertTrue(words.all().isEmpty())
    }

    @Test
    fun mp3WithoutMediaEngineReportsRepairableMediaState() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "audio-1",
                    title = "episode.mp3",
                    kind = SourceType.AUDIO,
                    extension = "mp3",
                    sourcePath = "/tmp/episode.mp3"
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(store, InMemoryBilingualCaptionRepository(), InMemoryLearningWordRepository())

        assertFailsWith<IllegalStateException> {
            pipeline.processImportedItem("audio-1")
        }
        assertEquals(ImportProcessingStatus.NEEDS_MEDIA_ENGINE, store.get("audio-1")?.status)
    }

    @Test
    fun wavWithoutCloudKeyReportsCloudKeyState() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "audio-2",
                    title = "voice.wav",
                    kind = SourceType.AUDIO,
                    extension = "wav",
                    sourcePath = "/tmp/voice.wav"
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(store, InMemoryBilingualCaptionRepository(), InMemoryLearningWordRepository())

        assertFailsWith<IllegalStateException> {
            pipeline.processImportedItem("audio-2")
        }
        assertEquals(ImportProcessingStatus.NEEDS_CLOUD_KEY, store.get("audio-2")?.status)
    }

    @Test
    fun videoWithEmbeddedTrackCompletesWithoutAsr() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-1",
                    title = "movie.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/movie.mp4"
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = InMemoryBilingualCaptionRepository(),
            learningWordRepository = InMemoryLearningWordRepository(),
            mediaEngine = object : MediaEngine {
                override val available: Boolean = true
                override val version: String = "test"
                override fun extractSubtitleTrack(input: String, output: String): String = """
                    1
                    00:00:01,000 --> 00:00:02,500
                    Embedded subtitles should bind automatically.
                """.trimIndent()
                override fun extractAudioForAsr(input: String, output: String): String = output
            }
        )

        val result = pipeline.processImportedItem("video-1")

        assertEquals(CaptionSource.EMBEDDED_TRACK, result.source)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("video-1")?.status)
    }

    @Test
    fun videoWithOverlongEmbeddedTrackScalesTimelineToVideoDuration() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-long-captions",
                    title = "four-minute.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/four-minute.mp4",
                    durationMs = 240_000L
                )
            )
        )
        val captions = InMemoryBilingualCaptionRepository()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = captions,
            learningWordRepository = InMemoryLearningWordRepository(),
            mediaEngine = object : MediaEngine {
                override val available: Boolean = true
                override val version: String = "test"
                override fun extractSubtitleTrack(input: String, output: String): String = """
                    1
                    00:00:00,000 --> 00:03:00,000
                    First half should scale.

                    2
                    00:03:00,000 --> 00:06:00,000
                    Second half should end at the video duration.
                """.trimIndent()
                override fun extractAudioForAsr(input: String, output: String): String = output
            }
        )

        val result = pipeline.processImportedItem("video-long-captions")
        val cues = captions.findByContentId("video-long-captions")?.cues.orEmpty()

        assertEquals(CaptionSource.EMBEDDED_TRACK, result.source)
        assertEquals(2, cues.size)
        assertEquals(120_000L, cues[1].startMs)
        assertEquals(240_000L, cues[1].endMs)
        assertTrue(cues.all { it.endMs <= 240_000L })
    }
}
