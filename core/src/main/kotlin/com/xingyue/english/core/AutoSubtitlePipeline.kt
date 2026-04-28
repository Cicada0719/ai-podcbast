package com.xingyue.english.core

import java.io.File
import kotlin.math.roundToLong

interface MediaEngine {
    val available: Boolean
    val version: String
    fun extractSubtitleTrack(input: String, output: String): String?
    fun extractAudioForAsr(input: String, output: String): String?
}

object UnavailableMediaEngine : MediaEngine {
    override val available: Boolean = false
    override val version: String = ""
    override fun extractSubtitleTrack(input: String, output: String): String? = null
    override fun extractAudioForAsr(input: String, output: String): String? = null
}

interface CloudLanguageService {
    val hasCloudKey: Boolean
    fun transcribeAudio(audioPath: String): List<CaptionCue>
    fun translateToChinese(english: String): String
    fun lookup(word: String): WordDefinition
}

object OfflineLanguageService : CloudLanguageService {
    override val hasCloudKey: Boolean = false
    override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
    override fun translateToChinese(english: String): String = ""
    override fun lookup(word: String): WordDefinition =
        WordDefinition(word = word, phonetic = "", chinese = "离线词典暂无释义")
}

class AutoSubtitlePipeline(
    private val contentStore: ImportedContentStore,
    private val captionRepository: BilingualCaptionRepository,
    @Suppress("UNUSED_PARAMETER")
    learningWordRepository: LearningWordRepository,
    private val mediaEngine: MediaEngine = UnavailableMediaEngine,
    private val languageService: CloudLanguageService = OfflineLanguageService
) {
    fun processImportedItem(itemId: String): SubtitleProcessingResult {
        val item = contentStore.get(itemId) ?: error("imported item not found: $itemId")
        return when (item.kind) {
            SourceType.SUBTITLE -> processImportedSubtitle(item)
            SourceType.DOCUMENT -> processDocument(item)
            SourceType.AUDIO -> processAudio(item)
            SourceType.VIDEO -> processVideo(item)
        }
    }

    fun retryProcessing(itemId: String): SubtitleProcessingResult =
        processImportedItem(itemId)

    fun cancelProcessing(itemId: String) {
        val item = contentStore.get(itemId) ?: return
        contentStore.update(item.copy(status = ImportProcessingStatus.IMPORTED, statusMessage = "已取消处理", progress = 0))
    }

    private fun processImportedSubtitle(item: ImportedContent): SubtitleProcessingResult {
        update(item, ImportProcessingStatus.EXTRACTING_SUBTITLE, "解析字幕", 20)
        val parsed = runCatching { SubtitleParser.parse(item.title, item.originalText) }.getOrElse {
            fail(item, "字幕解析失败：${it.message}")
            throw it
        }
        if (parsed.isEmpty()) {
            fail(item, "字幕解析失败：没有可识别的时间轴")
            error("subtitle parsing produced no cues")
        }

        val translated = translateMissingChinese(item, alignCueTimingToContent(item, parsed))
        return saveCaptionAndWords(item, translated, CaptionSource.IMPORTED_SUBTITLE)
    }

    private fun processDocument(item: ImportedContent): SubtitleProcessingResult {
        update(item, ImportProcessingStatus.EXTRACTING_WORDS, "提取正文，准备生成双语文档", 35)
        val cues = if (item.extension.equals("json", ignoreCase = true) || item.title.endsWith(".json", ignoreCase = true)) {
            SubtitleParser.parse(item.title, item.originalText).ifEmpty { SubtitleParser.fromPlainText(item.originalText) }
        } else {
            SubtitleParser.fromPlainText(item.originalText)
        }
        if (cues.isEmpty()) {
            fail(item, "文档没有可学习正文")
            error("document text is empty")
        }
        val translated = translateMissingChinese(item, alignCueTimingToContent(item, cues))
        return saveCaptionAndWords(item, translated, CaptionSource.DOCUMENT)
    }

    private fun processAudio(item: ImportedContent): SubtitleProcessingResult {
        val extension = item.extension.lowercase()
        val asrInput = if (item.importSource == ImportSource.DIRECT_URL && item.sourcePath.isHttpUrl()) {
            item.sourcePath
        } else if (extension == "wav" || extension == "pcm") {
            item.sourcePath
        } else {
            if (!mediaEngine.available) {
                needsMediaEngine(item)
                error("需要字幕或转写能力")
            }
            update(item, ImportProcessingStatus.EXTRACTING_SUBTITLE, "准备音频", 20)
            mediaEngine.extractAudioForAsr(item.sourcePath, derivedPath(item, "wav"))
                ?: run {
                    fail(item, "音频准备失败")
                    error("音频准备失败")
                }
        }

        if (!languageService.hasCloudKey) {
            needsCloudKey(item)
            error("需要先配置云端转写")
        }

        update(item, ImportProcessingStatus.TRANSCRIBING, "生成英文字幕", 45)
        val cues = runCatching { languageService.transcribeAudio(asrInput) }.getOrElse {
            fail(item, "语音识别失败：${it.message ?: "请稍后重试"}")
            throw it
        }
        if (cues.isEmpty()) {
            fail(item, "语音识别没有返回字幕")
            error("语音识别没有返回字幕")
        }
        val translated = translateMissingChinese(item, alignCueTimingToContent(item, cues))
        return saveCaptionAndWords(item, translated, CaptionSource.CLOUD_ASR)
    }

    private fun processVideo(item: ImportedContent): SubtitleProcessingResult {
        if (!mediaEngine.available) {
            needsMediaEngine(item)
            error("需要字幕或转写能力")
        }

        update(item, ImportProcessingStatus.EXTRACTING_SUBTITLE, "提取字幕轨", 20)
        val subtitleText = mediaEngine.extractSubtitleTrack(item.sourcePath, derivedPath(item, "srt"))
        if (!subtitleText.isNullOrBlank()) {
            val cues = SubtitleParser.parse("${item.title}.srt", subtitleText)
            val translated = translateMissingChinese(item, alignCueTimingToContent(item, cues))
            return saveCaptionAndWords(item, translated, CaptionSource.EMBEDDED_TRACK)
        }

        if (!languageService.hasCloudKey) {
            needsCloudKey(item)
            error("需要先配置云端转写")
        }

        update(item, ImportProcessingStatus.TRANSCRIBING, "生成英文字幕", 45)
        val audioPath = mediaEngine.extractAudioForAsr(item.sourcePath, derivedPath(item, "wav")) ?: item.sourcePath
        val cues = runCatching { languageService.transcribeAudio(audioPath) }.getOrElse {
            fail(item, "语音识别失败：${it.message ?: "请稍后重试"}")
            throw it
        }
        if (cues.isEmpty()) {
            fail(item, "语音识别没有返回字幕")
            error("语音识别没有返回字幕")
        }
        val translated = translateMissingChinese(item, alignCueTimingToContent(item, cues))
        return saveCaptionAndWords(item, translated, CaptionSource.CLOUD_ASR)
    }

    private fun translateMissingChinese(item: ImportedContent, cues: List<CaptionCue>): List<CaptionCue> {
        if (cues.all { it.chinese.isNotBlank() }) return cues
        if (!languageService.hasCloudKey) return cues

        update(item, ImportProcessingStatus.TRANSLATING, "生成中文字幕", 65)
        return cues.map { cue ->
            if (cue.chinese.isNotBlank() || cue.english.isBlank()) cue
            else cue.copy(chinese = languageService.translateToChinese(cue.english))
        }
    }

    private fun alignCueTimingToContent(item: ImportedContent, cues: List<CaptionCue>): List<CaptionCue> {
        val duration = item.durationMs
        if (duration <= 1L || cues.isEmpty()) return cues
        if (item.kind != SourceType.VIDEO && item.kind != SourceType.AUDIO) return cues
        val lastEnd = cues.maxOf { it.endMs }
        if (lastEnd <= duration) return cues
        val shouldScale = lastEnd - duration >= TIMELINE_OVERFLOW_GRACE_MS ||
            lastEnd >= (duration.toDouble() * TIMELINE_OVERFLOW_RATIO).roundToLong()
        val ratio = if (shouldScale) duration.toDouble() / lastEnd.toDouble() else 1.0
        return cues.map { cue ->
            val start = (cue.startMs * ratio).roundToLong().coerceAtLeast(0L).coerceAtMost(duration - 1L)
            val end = (cue.endMs * ratio).roundToLong().coerceAtLeast(0L).coerceIn(start + 1L, duration)
            cue.copy(startMs = start, endMs = end)
        }
    }

    private fun saveCaptionAndWords(
        item: ImportedContent,
        cues: List<CaptionCue>,
        source: CaptionSource
    ): SubtitleProcessingResult {
        if (cues.isEmpty()) {
            fail(item, "没有生成可学习字幕，可重新导入字幕文件后重试")
            error("caption saving requires at least one cue")
        }

        update(item, ImportProcessingStatus.EXTRACTING_WORDS, "整理可点选词", 82)
        val captionId = "cap-${item.id}"
        val caption = BilingualCaption(
            id = captionId,
            sourceItemId = item.id,
            cues = cues,
            source = source
        )
        captionRepository.save(caption)

        val wordCount = TextTools.candidateWords(cues).size

        contentStore.update(
            item.copy(
                status = ImportProcessingStatus.READY_TO_LEARN,
                statusMessage = if (cues.any { it.chinese.isBlank() }) {
                    "可学习。可点选字幕词加入词库。"
                } else {
                    "双语已生成。可点选字幕词加入词库。"
                },
                progress = 100,
                captionId = captionId,
                wordCount = wordCount
            )
        )

        return SubtitleProcessingResult(
            captionPath = "captions/$captionId.en.json",
            bilingualCaptionPath = "captions/$captionId.bilingual.json",
            captionCount = cues.size,
            wordCount = wordCount,
            source = source
        )
    }

    private fun SourceType.toLearningSourceType(): LearningSourceType =
        when (this) {
            SourceType.VIDEO -> LearningSourceType.VIDEO
            SourceType.AUDIO -> LearningSourceType.AUDIO
            SourceType.SUBTITLE -> LearningSourceType.SUBTITLE
            SourceType.DOCUMENT -> LearningSourceType.DOCUMENT
        }

    private fun update(item: ImportedContent, status: ImportProcessingStatus, message: String, progress: Int) {
        val latest = contentStore.get(item.id) ?: item
        contentStore.update(latest.copy(status = status, statusMessage = message, progress = progress))
    }

    private fun needsCloudKey(item: ImportedContent) {
        val latest = contentStore.get(item.id) ?: item
        val message = when (item.kind) {
            SourceType.AUDIO,
            SourceType.VIDEO -> "已找到音视频，但需要先配置云端转写生成字幕"
            SourceType.SUBTITLE,
            SourceType.DOCUMENT -> "需要先配置云端转写"
        }
        contentStore.update(
            latest.copy(
                status = ImportProcessingStatus.NEEDS_CLOUD_KEY,
                statusMessage = message,
                progress = 0
            )
        )
    }

    private fun needsMediaEngine(item: ImportedContent) {
        val latest = contentStore.get(item.id) ?: item
        contentStore.update(
            latest.copy(
                status = ImportProcessingStatus.NEEDS_MEDIA_ENGINE,
                statusMessage = "需要字幕文件或转写能力",
                progress = 0
            )
        )
    }

    private fun fail(item: ImportedContent, message: String) {
        val latest = contentStore.get(item.id) ?: item
        contentStore.update(
            latest.copy(
                status = ImportProcessingStatus.FAILED,
                statusMessage = message,
                progress = 0
            )
        )
    }

    private fun derivedPath(item: ImportedContent, extension: String): String {
        val source = File(item.sourcePath)
        val root = source.parentFile ?: File(".")
        val dir = File(root, "derived").apply { mkdirs() }
        return File(dir, "${item.id}.$extension").path
    }

    private fun String.isHttpUrl(): Boolean =
        startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

    companion object {
        private const val TIMELINE_OVERFLOW_GRACE_MS = 15_000L
        private const val TIMELINE_OVERFLOW_RATIO = 1.10
    }
}
