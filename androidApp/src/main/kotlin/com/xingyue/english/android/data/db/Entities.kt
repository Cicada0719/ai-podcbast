package com.xingyue.english.android.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contents")
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val extension: String,
    val uri: String,
    val localPath: String,
    val sourceUrl: String,
    val importSource: String,
    val originalText: String,
    val durationMs: Long,
    val coverPath: String,
    val status: String,
    val statusMessage: String,
    val progress: Int,
    val captionId: String,
    val wordCount: Int,
    val favorite: Boolean,
    val createdAt: Long
)

@Entity(tableName = "caption_cues", primaryKeys = ["captionId", "cueId"])
data class CaptionCueEntity(
    val captionId: String,
    val cueId: String,
    val sourceItemId: String,
    val source: String,
    val startMs: Long,
    val endMs: Long,
    val english: String,
    val chinese: String,
    val createdAt: Long
)

@Entity(tableName = "learning_words")
data class LearningWordEntity(
    @PrimaryKey val normalized: String,
    val id: String,
    val word: String,
    val phonetic: String,
    val chineseDefinition: String,
    val status: String,
    val occurrenceCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val dueAt: Long,
    val notes: String
)

@Entity(tableName = "word_contexts")
data class WordContextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordNormalized: String,
    val sourceItemId: String,
    val sourceTitle: String,
    val captionId: String,
    val captionStartMs: Long,
    val captionEndMs: Long,
    val englishSentence: String,
    val chineseSentence: String,
    val sourceType: String,
    val sourceUrl: String,
    val paragraphIndex: Int,
    val createdAt: Long
)

@Entity(tableName = "learning_reviews")
data class LearningReviewEntity(
    @PrimaryKey val id: String,
    val normalized: String,
    val rating: String,
    val reviewedAt: Long,
    val nextDueAt: Long,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reviewCount: Int,
    val lapses: Int
)

@Entity(tableName = "fsrs_card_states")
data class FsrsCardStateEntity(
    @PrimaryKey val normalized: String,
    val stability: Double,
    val difficulty: Double,
    val reviewCount: Int,
    val lapses: Int,
    val lastReviewAt: Long,
    val scheduledDays: Int,
    val dueAt: Long
)

@Entity(
    tableName = "study_task_attempts",
    indices = [
        Index(value = ["taskType", "createdAt"]),
        Index(value = ["taskItemId"]),
        Index(value = ["wordNormalized"]),
        Index(value = ["contentId"])
    ]
)
data class StudyTaskAttemptEntity(
    @PrimaryKey val id: String,
    val taskType: String,
    val taskItemId: String,
    val contentId: String,
    val wordNormalized: String,
    val result: String,
    val score: Int,
    val status: String,
    val durationMs: Long,
    val createdAt: Long,
    val completedAt: Long
)

@Entity(tableName = "platform_metadata", indices = [Index(value = ["platform"])])
data class PlatformMetadataEntity(
    @PrimaryKey val contentId: String,
    val platform: String,
    val requestedUrl: String,
    val canonicalUrl: String,
    val importUrl: String,
    val title: String,
    val coverUrl: String,
    val mediaUrl: String,
    val subtitleUrl: String,
    val createdAt: Long
)

@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey val normalized: String,
    val word: String,
    val phonetic: String,
    val definition: String,
    val phrases: String,
    val examples: String,
    val source: String
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val contentId: String,
    val captionId: String,
    val cueId: String,
    val paragraphIndex: Int,
    val positionMs: Long,
    val updatedAt: Long
)

@Entity(tableName = "tts_audio_cache")
data class TtsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val text: String,
    val voiceId: String,
    val speed: Float,
    val volume: Float,
    val audioPath: String,
    val byteSize: Long,
    val createdAt: Long,
    val lastUsedAt: Long,
    val hitCount: Int
)

@Entity(
    tableName = "vocabulary_decks",
    indices = [
        Index(value = ["goalMode"]),
        Index(value = ["stage"])
    ]
)
data class VocabularyDeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stage: String,
    val goalMode: String,
    val description: String,
    val licenseSource: String,
    val itemTarget: Int,
    val displayOrder: Int,
    val createdAt: Long
)

@Entity(
    tableName = "lexical_items",
    indices = [
        Index(value = ["normalized"]),
        Index(value = ["deckId"]),
        Index(value = ["stage"])
    ]
)
data class LexicalItemEntity(
    @PrimaryKey val id: String,
    val word: String,
    val normalized: String,
    val phonetic: String,
    val definition: String,
    val cefr: String,
    val deckId: String,
    val stage: String,
    val tags: String,
    val phrases: String,
    val example: String,
    val licenseSource: String,
    val difficulty: Int,
    val createdAt: Long
)

@Entity(
    tableName = "phrase_chunks",
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["useCase"])
    ]
)
data class PhraseChunkEntity(
    @PrimaryKey val id: String,
    val english: String,
    val chinese: String,
    val useCase: String,
    val deckId: String,
    val keywords: String,
    val licenseSource: String,
    val ttsCacheKey: String,
    val createdAt: Long
)

@Entity(
    tableName = "study_path_attempts",
    indices = [
        Index(value = ["goalMode", "createdAt"]),
        Index(value = ["stage", "createdAt"]),
        Index(value = ["contentId"]),
        Index(value = ["wordNormalized"])
    ]
)
data class StudyPathAttemptEntity(
    @PrimaryKey val id: String,
    val goalMode: String,
    val stage: String,
    val taskType: String,
    val contentId: String,
    val wordNormalized: String,
    val targetValue: Int,
    val completedValue: Int,
    val result: String,
    val status: String,
    val durationMs: Long,
    val sessionId: String,
    val createdAt: Long,
    val completedAt: Long
)

@Entity(
    tableName = "practice_sessions",
    indices = [
        Index(value = ["mode", "createdAt"]),
        Index(value = ["sourceId"])
    ]
)
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val sourceId: String,
    val sourceType: String,
    val targetCount: Int,
    val completedCount: Int,
    val score: Int,
    val durationMs: Long,
    val createdAt: Long,
    val completedAt: Long
)

@Entity(
    tableName = "typing_attempts",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["wordNormalized"]),
        Index(value = ["createdAt"])
    ]
)
data class TypingAttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val mode: String,
    val promptId: String,
    val expected: String,
    val answer: String,
    val wordNormalized: String,
    val correct: Boolean,
    val accuracy: Int,
    val typoCount: Int,
    val wpm: Int,
    val durationMs: Long,
    val createdAt: Long
)

@Entity(tableName = "achievements", indices = [Index(value = ["type"])])
data class AchievementEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String,
    val threshold: Int,
    val unlocked: Boolean,
    val unlockedAt: Long
)

@Entity(
    tableName = "achievement_events",
    indices = [
        Index(value = ["achievementId"]),
        Index(value = ["createdAt"])
    ]
)
data class AchievementEventEntity(
    @PrimaryKey val id: String,
    val achievementId: String,
    val source: String,
    val createdAt: Long
)

@Entity(
    tableName = "word_game_sessions",
    indices = [
        Index(value = ["gameType", "createdAt"]),
        Index(value = ["targetWord"])
    ]
)
data class WordGameSessionEntity(
    @PrimaryKey val id: String,
    val gameType: String,
    val targetWord: String,
    val won: Boolean,
    val attempts: Int,
    val score: Int,
    val createdAt: Long,
    val completedAt: Long
)

@Entity(
    tableName = "query_history",
    indices = [
        Index(value = ["normalized"]),
        Index(value = ["createdAt"])
    ]
)
data class QueryHistoryEntity(
    @PrimaryKey val id: String,
    val word: String,
    val normalized: String,
    val source: String,
    val favorite: Boolean,
    val createdAt: Long
)

@Entity(tableName = "app_config")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "typewords_dicts",
    indices = [
        Index(value = ["dictType"]),
        Index(value = ["importedAt"])
    ]
)
data class TypeWordsDictEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val category: String,
    val tags: String,
    val translateLanguage: String,
    val dictType: String,
    val language: String,
    val lastLearnIndex: Int,
    val perDayStudyNumber: Int,
    val length: Int,
    val custom: Boolean,
    val complete: Boolean,
    val enName: String,
    val createdBy: String,
    val cover: String,
    val rawJson: String,
    val importedAt: Long
)

@Entity(
    tableName = "typewords_words",
    primaryKeys = ["dictId", "normalized"],
    indices = [
        Index(value = ["dictId"]),
        Index(value = ["normalized"])
    ]
)
data class TypeWordsWordEntity(
    val dictId: String,
    val normalized: String,
    val orderIndex: Int,
    val word: String,
    val phonetic0: String,
    val phonetic1: String,
    val definitionText: String,
    val rawJson: String,
    val importedAt: Long
)

@Entity(
    tableName = "typewords_articles",
    primaryKeys = ["dictId", "articleKey"],
    indices = [
        Index(value = ["dictId"]),
        Index(value = ["title"])
    ]
)
data class TypeWordsArticleEntity(
    val dictId: String,
    val articleKey: String,
    val orderIndex: Int,
    val title: String,
    val titleTranslate: String,
    val text: String,
    val textTranslate: String,
    val audioSrc: String,
    val rawJson: String,
    val importedAt: Long
)

@Entity(tableName = "typewords_settings")
data class TypeWordsSettingEntity(
    @PrimaryKey val key: String,
    val valueJson: String,
    val updatedAt: Long
)

@Entity(
    tableName = "mujing_vocabularies",
    indices = [
        Index(value = ["vocabularyType"]),
        Index(value = ["importedAt"])
    ]
)
data class MuJingVocabularyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val vocabularyType: String,
    val language: String,
    val size: Int,
    val relateVideoPath: String,
    val subtitlesTrackId: Int,
    val rawJson: String,
    val importedAt: Long
)

@Entity(
    tableName = "mujing_words",
    primaryKeys = ["vocabularyId", "normalized"],
    indices = [
        Index(value = ["vocabularyId"]),
        Index(value = ["normalized"])
    ]
)
data class MuJingWordEntity(
    val vocabularyId: String,
    val normalized: String,
    val orderIndex: Int,
    val value: String,
    val usphone: String,
    val ukphone: String,
    val definition: String,
    val translation: String,
    val pos: String,
    val collins: Int,
    val oxford: Boolean,
    val tag: String,
    val bnc: Int?,
    val frq: Int?,
    val exchange: String,
    val importedAt: Long
)

@Entity(
    tableName = "mujing_captions",
    primaryKeys = ["vocabularyId", "wordNormalized", "captionKind", "captionIndex"],
    indices = [
        Index(value = ["vocabularyId"]),
        Index(value = ["wordNormalized"])
    ]
)
data class MuJingCaptionEntity(
    val vocabularyId: String,
    val wordNormalized: String,
    val captionKind: String,
    val captionIndex: Int,
    val relateVideoPath: String,
    val subtitlesTrackId: Int,
    val subtitlesName: String,
    val startText: String,
    val endText: String,
    val content: String,
    val importedAt: Long
)
