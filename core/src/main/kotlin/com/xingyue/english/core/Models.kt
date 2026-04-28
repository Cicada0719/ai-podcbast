package com.xingyue.english.core

enum class SourceType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    DOCUMENT
}

enum class ImportSource {
    LOCAL_FILE,
    DIRECT_URL,
    BOUND_SUBTITLE
}

enum class CaptionSource {
    EMBEDDED_TRACK,
    CLOUD_ASR,
    IMPORTED_SUBTITLE,
    DOCUMENT
}

enum class ImportProcessingStatus {
    IMPORTED,
    EXTRACTING_SUBTITLE,
    TRANSCRIBING,
    TRANSLATING,
    EXTRACTING_WORDS,
    READY_TO_LEARN,
    NEEDS_CLOUD_KEY,
    NEEDS_MEDIA_ENGINE,
    FAILED
}

enum class LearningSourceType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    DOCUMENT
}

enum class LearningWordStatus {
    NEW_WORD,
    LEARNING,
    DUE,
    FAMILIAR,
    MASTERED,
    IGNORED
}

enum class ReviewRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

enum class StudyTaskType {
    NEW_WORDS,
    DUE_REVIEW,
    SPELLING,
    LISTENING_REPEAT,
    HUNDRED_LS,
    MISTAKES
}

enum class StudyAttemptStatus {
    STARTED,
    COMPLETED,
    SKIPPED,
    FAILED
}

data class StudyTaskItem(
    val id: String,
    val taskType: StudyTaskType,
    val title: String,
    val subtitle: String = "",
    val contentId: String = "",
    val wordNormalized: String = "",
    val sourceType: LearningSourceType? = null,
    val priority: Int = 0,
    val dueAt: Long = 0L,
    val estimatedMinutes: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

data class CaptionToken(
    val text: String,
    val normalized: String,
    val startIndex: Int,
    val endIndex: Int,
    val saved: Boolean = false
)

data class CaptionCue(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val english: String,
    val chinese: String = "",
    val tokens: List<CaptionToken> = TextTools.tokenize(english)
)

data class BilingualCaption(
    val id: String,
    val sourceItemId: String,
    val cues: List<CaptionCue>,
    val source: CaptionSource,
    val createdAt: Long = System.currentTimeMillis()
)

data class WordDefinition(
    val word: String,
    val phonetic: String = "",
    val chinese: String = "",
    val phrases: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val roots: String = ""
)

data class WordSelectionContext(
    val word: String,
    val normalized: String = TextTools.normalizeWord(word),
    val sourceItemId: String,
    val captionStartMs: Long,
    val captionEndMs: Long,
    val englishSentence: String,
    val chineseSentence: String,
    val sourceType: LearningSourceType,
    val captionId: String = "",
    val sourceTitle: String = "",
    val phonetic: String = "",
    val chineseDefinition: String = "",
    val sourceUrl: String = "",
    val paragraphIndex: Int = -1
)

data class SourceJumpTarget(
    val sourceItemId: String,
    val sourceType: LearningSourceType,
    val title: String,
    val captionId: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val paragraphIndex: Int = -1,
    val sourceUrl: String = ""
)

data class SourceContext(
    val sourceItemId: String,
    val sourceTitle: String,
    val captionId: String,
    val captionStartMs: Long,
    val captionEndMs: Long,
    val englishSentence: String,
    val chineseSentence: String,
    val sourceType: LearningSourceType,
    val sourceUrl: String = "",
    val paragraphIndex: Int = -1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun jumpTarget(): SourceJumpTarget =
        SourceJumpTarget(
            sourceItemId = sourceItemId,
            sourceType = sourceType,
            title = sourceTitle,
            captionId = captionId,
            startMs = captionStartMs,
            endMs = captionEndMs,
            paragraphIndex = paragraphIndex,
            sourceUrl = sourceUrl
        )
}

data class LearningWord(
    val id: String,
    val word: String,
    val normalized: String,
    val phonetic: String = "",
    val chineseDefinition: String = "",
    val status: LearningWordStatus = LearningWordStatus.NEW_WORD,
    val contexts: List<SourceContext> = emptyList(),
    val occurrenceCount: Int = contexts.size,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val dueAt: Long = createdAt,
    val notes: String = ""
)

data class LearningReview(
    val id: String,
    val normalized: String,
    val rating: ReviewRating,
    val reviewedAt: Long,
    val nextDueAt: Long,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reviewCount: Int = 0,
    val lapses: Int = 0
)

data class FsrsCardState(
    val normalized: String,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reviewCount: Int = 0,
    val lapses: Int = 0,
    val lastReviewAt: Long = 0L,
    val scheduledDays: Int = 0,
    val dueAt: Long = 0L
)

data class ReviewSessionResult(
    val word: LearningWord,
    val cardState: FsrsCardState,
    val review: LearningReview
)

data class StudyAttempt(
    val id: String,
    val taskType: StudyTaskType,
    val taskItemId: String = "",
    val contentId: String = "",
    val wordNormalized: String = "",
    val result: String = "",
    val score: Int = 0,
    val status: StudyAttemptStatus = StudyAttemptStatus.COMPLETED,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = createdAt
) {
    val completedOrClosedAt: Long
        get() = if (completedAt > 0L) completedAt else createdAt

    val countsAsAttempt: Boolean
        get() = status != StudyAttemptStatus.STARTED

    val countsAsCompleted: Boolean
        get() = status == StudyAttemptStatus.COMPLETED

    val isMistake: Boolean
        get() = status == StudyAttemptStatus.FAILED ||
            score in 1..59 ||
            result.equals("AGAIN", ignoreCase = true) ||
            result.equals("incorrect", ignoreCase = true) ||
            result.equals("wrong", ignoreCase = true) ||
            result.equals("failed", ignoreCase = true)
}

typealias StudyRecord = StudyAttempt

data class ImportedContent(
    val id: String,
    val title: String,
    val kind: SourceType,
    val extension: String = "",
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val importSource: ImportSource = ImportSource.LOCAL_FILE,
    val originalText: String = "",
    val durationMs: Long = 0L,
    val coverPath: String = "",
    val status: ImportProcessingStatus = ImportProcessingStatus.IMPORTED,
    val statusMessage: String = "已导入",
    val progress: Int = 0,
    val captionId: String = "",
    val wordCount: Int = 0,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SubtitleProcessingResult(
    val captionPath: String,
    val bilingualCaptionPath: String,
    val captionCount: Int,
    val wordCount: Int,
    val source: CaptionSource
)

data class LearningWordFilters(
    val sourceType: LearningSourceType? = null,
    val status: LearningWordStatus? = null,
    val sourceItemId: String? = null
)

data class DictionaryEntry(
    val word: String,
    val normalized: String = TextTools.normalizeWord(word),
    val phonetic: String = "",
    val definition: String = "",
    val phrases: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val source: String = "内置词典"
)

data class TtsVoice(
    val id: String,
    val name: String,
    val description: String,
    val language: String = "中文/英文"
)

data class SpeechSynthesisResult(
    val text: String,
    val voiceId: String,
    val audioPath: String,
    val cached: Boolean
)

interface SpeechSynthesisService {
    val available: Boolean
    fun synthesize(text: String, voiceId: String, speed: Float = 1f, volume: Float = 1f): SpeechSynthesisResult
    fun previewVoice(voiceId: String): SpeechSynthesisResult =
        synthesize("I was wondering if you could help me with this.", voiceId)
}
