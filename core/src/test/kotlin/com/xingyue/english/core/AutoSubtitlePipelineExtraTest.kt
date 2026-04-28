package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoSubtitlePipelineExtraTest {
    @Test
    fun subtitleWithoutCloudKeyStillBecomesLearnable() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "srt-offline",
                    title = "offline.srt",
                    kind = SourceType.SUBTITLE,
                    extension = "srt",
                    originalText = """
                        1
                        00:00:01,000 --> 00:00:02,000
                        Context anchors durable memory.
                    """.trimIndent()
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(store, InMemoryBilingualCaptionRepository(), InMemoryLearningWordRepository())

        val result = pipeline.processImportedItem("srt-offline")

        assertEquals(CaptionSource.IMPORTED_SUBTITLE, result.source)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("srt-offline")?.status)
        assertTrue(store.get("srt-offline")?.statusMessage.orEmpty().contains("点选字幕词"))
    }

    @Test
    fun wavWithCloudKeyCreatesCloudAsrCaptionWithoutAutoWords() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "wav-1",
                    title = "voice.wav",
                    kind = SourceType.AUDIO,
                    extension = "wav",
                    sourcePath = "/tmp/voice.wav"
                )
            )
        )
        val words = InMemoryLearningWordRepository()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = InMemoryBilingualCaptionRepository(),
            learningWordRepository = words,
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> =
                    listOf(CaptionCue("asr-1", 0L, 1800L, "Listening builds sustainable practice."))
                override fun translateToChinese(english: String): String = "听力建立可持续的练习。"
                override fun lookup(word: String): WordDefinition = error("automatic import must not lookup or save words")
            }
        )

        val result = pipeline.processImportedItem("wav-1")

        assertEquals(CaptionSource.CLOUD_ASR, result.source)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("wav-1")?.status)
        assertTrue(words.all().isEmpty())
    }

    @Test
    fun emptyAsrResultMovesContentToFailed() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "wav-empty",
                    title = "empty.wav",
                    kind = SourceType.AUDIO,
                    extension = "wav",
                    sourcePath = "/tmp/empty.wav"
                )
            )
        )
        val captions = InMemoryBilingualCaptionRepository()
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = captions,
            learningWordRepository = InMemoryLearningWordRepository(),
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String = ""
                override fun lookup(word: String): WordDefinition = WordDefinition(word)
            }
        )

        runCatching { pipeline.processImportedItem("wav-empty") }

        assertEquals(ImportProcessingStatus.FAILED, store.get("wav-empty")?.status)
        assertNull(captions.findByContentId("wav-empty"))
    }

    @Test
    fun readyToLearnRequiresAtLeastOneCue() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-empty-track",
                    title = "empty-track.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/empty-track.mp4"
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
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String = """
                    1
                    00:00:01,000 --> 00:00:02,000
                """.trimIndent()
                override fun extractAudioForAsr(input: String, output: String): String = output
            },
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String = ""
                override fun lookup(word: String): WordDefinition = WordDefinition(word)
            }
        )

        assertFailsWith<IllegalStateException> {
            pipeline.processImportedItem("video-empty-track")
        }

        assertEquals(ImportProcessingStatus.FAILED, store.get("video-empty-track")?.status)
        assertEquals(0, store.get("video-empty-track")?.progress)
        assertTrue(store.get("video-empty-track")?.captionId.orEmpty().isBlank())
        assertNull(captions.findByContentId("video-empty-track"))
    }

    @Test
    fun videoWithoutSubtitleAndWithoutCloudKeyNeedsCloudKey() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-no-key",
                    title = "public-video.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/public-video.mp4"
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
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String? = null
                override fun extractAudioForAsr(input: String, output: String): String = output
            }
        )

        assertFailsWith<IllegalStateException> {
            pipeline.processImportedItem("video-no-key")
        }

        assertEquals(ImportProcessingStatus.NEEDS_CLOUD_KEY, store.get("video-no-key")?.status)
        assertTrue(store.get("video-no-key")?.statusMessage.orEmpty().contains("生成字幕"))
        assertNull(captions.findByContentId("video-no-key"))
    }

    @Test
    fun unknownDurationKeepsOriginalCueTimeline() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-unknown-duration",
                    title = "unknown-duration.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/unknown-duration.mp4",
                    durationMs = 0L
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
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String = """
                    1
                    00:00:00,000 --> 00:03:00,000
                    First unknown-duration cue.

                    2
                    00:03:00,000 --> 00:06:00,000
                    Second unknown-duration cue.
                """.trimIndent()
                override fun extractAudioForAsr(input: String, output: String): String = output
            }
        )

        pipeline.processImportedItem("video-unknown-duration")

        val cues = captions.findByContentId("video-unknown-duration")?.cues.orEmpty()
        assertEquals(360_000L, cues.last().endMs)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("video-unknown-duration")?.status)
    }

    @Test
    fun videoWithoutEmbeddedSubtitleFallsBackToExtractedAudioAsr() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-asr",
                    title = "clip.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/clip.mp4"
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = InMemoryBilingualCaptionRepository(),
            learningWordRepository = InMemoryLearningWordRepository(),
            mediaEngine = object : MediaEngine {
                override val available: Boolean = true
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String? = null
                override fun extractAudioForAsr(input: String, output: String): String = output
            },
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> =
                    listOf(CaptionCue("asr-video", 0L, 1400L, "Fallback audio creates learnable subtitles."))
                override fun translateToChinese(english: String): String = "回退音频生成可学习字幕。"
                override fun lookup(word: String): WordDefinition = WordDefinition(word, chinese = "释义")
            }
        )

        val result = pipeline.processImportedItem("video-asr")

        assertEquals(CaptionSource.CLOUD_ASR, result.source)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, store.get("video-asr")?.status)
    }

    @Test
    fun mediaCaptionLongerThanContentDurationIsScaledBack() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "video-drift",
                    title = "clip.mp4",
                    kind = SourceType.VIDEO,
                    extension = "mp4",
                    sourcePath = "/tmp/clip.mp4",
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
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String = """
                    1
                    00:00:00,000 --> 00:03:00,000
                    The first half should stay aligned.

                    2
                    00:03:00,000 --> 00:06:00,000
                    The final cue should not exceed the video.
                """.trimIndent()
                override fun extractAudioForAsr(input: String, output: String): String = output
            }
        )

        pipeline.processImportedItem("video-drift")

        val lastEnd = captions.findByContentId("video-drift")?.cues.orEmpty().maxOf { it.endMs }
        assertEquals(240_000L, lastEnd)
    }

    @Test
    fun audioExtractionFailureMovesContentToFailed() {
        val store = InMemoryImportedContentStore(
            listOf(
                ImportedContent(
                    id = "mp3-bad",
                    title = "broken.mp3",
                    kind = SourceType.AUDIO,
                    extension = "mp3",
                    sourcePath = "/tmp/broken.mp3"
                )
            )
        )
        val pipeline = AutoSubtitlePipeline(
            contentStore = store,
            captionRepository = InMemoryBilingualCaptionRepository(),
            learningWordRepository = InMemoryLearningWordRepository(),
            mediaEngine = object : MediaEngine {
                override val available: Boolean = true
                override val version: String = "fake"
                override fun extractSubtitleTrack(input: String, output: String): String? = null
                override fun extractAudioForAsr(input: String, output: String): String? = null
            },
            languageService = object : CloudLanguageService {
                override val hasCloudKey: Boolean = true
                override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
                override fun translateToChinese(english: String): String = ""
                override fun lookup(word: String): WordDefinition = WordDefinition(word)
            }
        )

        assertFailsWith<IllegalStateException> { pipeline.processImportedItem("mp3-bad") }

        assertEquals(ImportProcessingStatus.FAILED, store.get("mp3-bad")?.status)
    }

    @Test
    fun fsrsReviewSchedulesNextDueDate() {
        val repository = InMemoryLearningWordRepository()
        val word = repository.addFromSelection(
            WordSelectionContext(
                word = "retention",
                sourceItemId = "doc-1",
                captionStartMs = 0L,
                captionEndMs = 1000L,
                englishSentence = "Retention improves with review.",
                chineseSentence = "",
                sourceType = LearningSourceType.DOCUMENT
            )
        )

        val (updated, review) = FsrsScheduler.review(word, ReviewRating.GOOD, now = 1_000L)

        assertEquals(ReviewRating.GOOD, review.rating)
        assertTrue(updated.dueAt > 1_000L)
        assertEquals(LearningWordStatus.FAMILIAR, updated.status)
    }

    @Test
    fun captionRepositoryHighlightsSavedTokens() {
        val captions = InMemoryBilingualCaptionRepository()
        captions.save(
            BilingualCaption(
                id = "caption-1",
                sourceItemId = "content-1",
                source = CaptionSource.IMPORTED_SUBTITLE,
                cues = listOf(CaptionCue("cue-1", 0L, 1000L, "Durable context matters."))
            )
        )

        val updated = captions.markSavedWords("caption-1", setOf("durable", "context"))

        assertTrue(updated?.cues?.single()?.tokens.orEmpty().filter { it.saved }.map { it.normalized }.containsAll(listOf("durable", "context")))
    }
}
