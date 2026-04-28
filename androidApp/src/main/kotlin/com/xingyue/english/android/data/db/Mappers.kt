package com.xingyue.english.android.data.db

import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.DictionaryEntry
import com.xingyue.english.core.FsrsCardState
import com.xingyue.english.core.ImportSource
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.LearningReview
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.ReviewRating
import com.xingyue.english.core.SourceContext
import com.xingyue.english.core.SourceType
import com.xingyue.english.core.SpeechSynthesisResult
import com.xingyue.english.core.StudyAttempt
import com.xingyue.english.core.StudyAttemptStatus
import com.xingyue.english.core.StudyTaskType
import com.xingyue.english.core.TextTools
import java.io.File

fun ImportedContent.toEntity(): ContentEntity =
    ContentEntity(
        id = id,
        title = title,
        kind = kind.name,
        extension = extension,
        uri = sourcePath,
        localPath = sourcePath,
        sourceUrl = sourceUrl,
        importSource = importSource.name,
        originalText = originalText,
        durationMs = durationMs,
        coverPath = coverPath,
        status = status.name,
        statusMessage = statusMessage,
        progress = progress,
        captionId = captionId,
        wordCount = wordCount,
        favorite = favorite,
        createdAt = createdAt
    )

fun ContentEntity.toDomain(): ImportedContent =
    ImportedContent(
        id = id,
        title = title,
        kind = enumValueOrDefault(kind, SourceType.DOCUMENT),
        extension = extension,
        sourcePath = localPath.ifBlank { uri },
        sourceUrl = sourceUrl,
        importSource = enumValueOrDefault(importSource, ImportSource.LOCAL_FILE),
        originalText = originalText,
        durationMs = durationMs,
        coverPath = coverPath,
        status = enumValueOrDefault(status, ImportProcessingStatus.IMPORTED),
        statusMessage = statusMessage,
        progress = progress,
        captionId = captionId,
        wordCount = wordCount,
        favorite = favorite,
        createdAt = createdAt
    )

fun BilingualCaption.toEntities(): List<CaptionCueEntity> =
    cues.map { cue ->
        CaptionCueEntity(
            captionId = id,
            cueId = cue.id,
            sourceItemId = sourceItemId,
            source = source.name,
            startMs = cue.startMs,
            endMs = cue.endMs,
            english = cue.english,
            chinese = cue.chinese,
            createdAt = createdAt
        )
    }

fun List<CaptionCueEntity>.toCaption(): BilingualCaption? {
    if (isEmpty()) return null
    val first = first()
    return BilingualCaption(
        id = first.captionId,
        sourceItemId = first.sourceItemId,
        source = enumValueOrDefault(first.source, CaptionSource.IMPORTED_SUBTITLE),
        createdAt = first.createdAt,
        cues = map {
            CaptionCue(
                id = it.cueId,
                startMs = it.startMs,
                endMs = it.endMs,
                english = it.english,
                chinese = it.chinese
            )
        }
    )
}

fun LearningWord.toEntity(): LearningWordEntity =
    LearningWordEntity(
        normalized = normalized,
        id = id,
        word = word,
        phonetic = phonetic,
        chineseDefinition = chineseDefinition,
        status = status.name,
        occurrenceCount = occurrenceCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dueAt = dueAt,
        notes = notes
    )

fun LearningWordEntity.toDomain(contexts: List<WordContextEntity>): LearningWord =
    LearningWord(
        id = id,
        word = word,
        normalized = normalized,
        phonetic = phonetic,
        chineseDefinition = chineseDefinition,
        status = enumValueOrDefault(status, LearningWordStatus.NEW_WORD),
        contexts = contexts.map { it.toDomain() },
        occurrenceCount = occurrenceCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dueAt = dueAt,
        notes = notes
    )

fun SourceContext.toEntity(wordNormalized: String): WordContextEntity =
    WordContextEntity(
        wordNormalized = wordNormalized,
        sourceItemId = sourceItemId,
        sourceTitle = sourceTitle,
        captionId = captionId,
        captionStartMs = captionStartMs,
        captionEndMs = captionEndMs,
        englishSentence = englishSentence,
        chineseSentence = chineseSentence,
        sourceType = sourceType.name,
        sourceUrl = sourceUrl,
        paragraphIndex = paragraphIndex,
        createdAt = createdAt
    )

fun WordContextEntity.toDomain(): SourceContext =
    SourceContext(
        sourceItemId = sourceItemId,
        sourceTitle = sourceTitle,
        captionId = captionId,
        captionStartMs = captionStartMs,
        captionEndMs = captionEndMs,
        englishSentence = englishSentence,
        chineseSentence = chineseSentence,
        sourceType = enumValueOrDefault(sourceType, LearningSourceType.DOCUMENT),
        sourceUrl = sourceUrl,
        paragraphIndex = paragraphIndex,
        createdAt = createdAt
    )

fun LearningReview.toEntity(): LearningReviewEntity =
    LearningReviewEntity(
        id = id,
        normalized = normalized,
        rating = rating.name,
        reviewedAt = reviewedAt,
        nextDueAt = nextDueAt,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reviewCount = reviewCount,
        lapses = lapses
    )

fun LearningReviewEntity.toDomain(): LearningReview =
    LearningReview(
        id = id,
        normalized = normalized,
        rating = enumValueOrDefault(rating, ReviewRating.GOOD),
        reviewedAt = reviewedAt,
        nextDueAt = nextDueAt,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reviewCount = reviewCount,
        lapses = lapses
    )

fun FsrsCardState.toEntity(): FsrsCardStateEntity =
    FsrsCardStateEntity(
        normalized = normalized,
        stability = stability,
        difficulty = difficulty,
        reviewCount = reviewCount,
        lapses = lapses,
        lastReviewAt = lastReviewAt,
        scheduledDays = scheduledDays,
        dueAt = dueAt
    )

fun FsrsCardStateEntity.toDomain(): FsrsCardState =
    FsrsCardState(
        normalized = normalized,
        stability = stability,
        difficulty = difficulty,
        reviewCount = reviewCount,
        lapses = lapses,
        lastReviewAt = lastReviewAt,
        scheduledDays = scheduledDays,
        dueAt = dueAt
    )

fun StudyAttempt.toEntity(): StudyTaskAttemptEntity =
    StudyTaskAttemptEntity(
        id = id,
        taskType = taskType.name,
        taskItemId = taskItemId,
        contentId = contentId,
        wordNormalized = wordNormalized,
        result = result,
        score = score,
        status = status.name,
        durationMs = durationMs,
        createdAt = createdAt,
        completedAt = completedAt
    )

fun StudyTaskAttemptEntity.toDomain(): StudyAttempt =
    StudyAttempt(
        id = id,
        taskType = enumValueOrDefault(taskType, StudyTaskType.NEW_WORDS),
        taskItemId = taskItemId,
        contentId = contentId,
        wordNormalized = wordNormalized,
        result = result,
        score = score,
        status = enumValueOrDefault(status, StudyAttemptStatus.COMPLETED),
        durationMs = durationMs,
        createdAt = createdAt,
        completedAt = completedAt
    )

fun LearningWordEntity.matches(query: String, sourceType: LearningSourceType?, status: LearningWordStatus?, contexts: List<WordContextEntity>): Boolean {
    val normalizedQuery = TextTools.normalizeWord(query)
    val queryMatches = normalizedQuery.isBlank() ||
        normalized.contains(normalizedQuery) ||
        word.contains(query, ignoreCase = true) ||
        chineseDefinition.contains(query, ignoreCase = true)
    val statusMatches = status == null || enumValueOrDefault(this.status, LearningWordStatus.NEW_WORD) == status
    val sourceMatches = sourceType == null || contexts.any { enumValueOrDefault(it.sourceType, LearningSourceType.DOCUMENT) == sourceType }
    return queryMatches && statusMatches && sourceMatches
}

inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

fun DictionaryEntry.toEntity(): DictionaryEntryEntity =
    DictionaryEntryEntity(
        normalized = normalized,
        word = word,
        phonetic = phonetic,
        definition = definition,
        phrases = phrases.joinToString("\n"),
        examples = examples.joinToString("\n"),
        source = source
    )

fun DictionaryEntryEntity.toDomain(): DictionaryEntry =
    DictionaryEntry(
        word = word,
        normalized = normalized,
        phonetic = phonetic,
        definition = definition,
        phrases = phrases.split('\n').filter { it.isNotBlank() },
        examples = examples.split('\n').filter { it.isNotBlank() },
        source = source
    )

fun SpeechSynthesisResult.toTtsCacheEntity(
    cacheKey: String,
    speed: Float,
    volume: Float,
    now: Long = System.currentTimeMillis()
): TtsCacheEntity =
    TtsCacheEntity(
        cacheKey = cacheKey,
        text = text,
        voiceId = voiceId,
        speed = speed,
        volume = volume,
        audioPath = audioPath,
        byteSize = File(audioPath).length().coerceAtLeast(0L),
        createdAt = now,
        lastUsedAt = now,
        hitCount = if (cached) 1 else 0
    )

fun TtsCacheEntity.toSpeechSynthesisResult(): SpeechSynthesisResult =
    SpeechSynthesisResult(
        text = text,
        voiceId = voiceId,
        audioPath = audioPath,
        cached = true
    )
