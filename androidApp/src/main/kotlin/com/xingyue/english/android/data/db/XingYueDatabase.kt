package com.xingyue.english.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ContentEntity::class,
        CaptionCueEntity::class,
        LearningWordEntity::class,
        WordContextEntity::class,
        LearningReviewEntity::class,
        FsrsCardStateEntity::class,
        StudyTaskAttemptEntity::class,
        DictionaryEntryEntity::class,
        ReadingProgressEntity::class,
        TtsCacheEntity::class,
        PlatformMetadataEntity::class,
        VocabularyDeckEntity::class,
        LexicalItemEntity::class,
        PhraseChunkEntity::class,
        StudyPathAttemptEntity::class,
        PracticeSessionEntity::class,
        TypingAttemptEntity::class,
        AchievementEntity::class,
        AchievementEventEntity::class,
        WordGameSessionEntity::class,
        QueryHistoryEntity::class,
        ConfigEntity::class,
        TypeWordsDictEntity::class,
        TypeWordsWordEntity::class,
        TypeWordsArticleEntity::class,
        TypeWordsSettingEntity::class,
        MuJingVocabularyEntity::class,
        MuJingWordEntity::class,
        MuJingCaptionEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class XingYueDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun captionDao(): CaptionDao
    abstract fun learningWordDao(): LearningWordDao
    abstract fun reviewDao(): ReviewDao
    abstract fun fsrsCardStateDao(): FsrsCardStateDao
    abstract fun studyTaskAttemptDao(): StudyTaskAttemptDao
    abstract fun dictionaryEntryDao(): DictionaryEntryDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun ttsCacheDao(): TtsCacheDao
    abstract fun platformMetadataDao(): PlatformMetadataDao
    abstract fun vocabularyDeckDao(): VocabularyDeckDao
    abstract fun lexicalItemDao(): LexicalItemDao
    abstract fun phraseChunkDao(): PhraseChunkDao
    abstract fun studyPathAttemptDao(): StudyPathAttemptDao
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun typingAttemptDao(): TypingAttemptDao
    abstract fun achievementDao(): AchievementDao
    abstract fun achievementEventDao(): AchievementEventDao
    abstract fun wordGameSessionDao(): WordGameSessionDao
    abstract fun queryHistoryDao(): QueryHistoryDao
    abstract fun configDao(): ConfigDao
    abstract fun sourceModelDao(): SourceModelDao

    companion object {
        @Volatile
        private var instance: XingYueDatabase? = null

        fun get(context: Context): XingYueDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XingYueDatabase::class.java,
                    "xingyue_english.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS learning_reviews (
                        id TEXT NOT NULL PRIMARY KEY,
                        normalized TEXT NOT NULL,
                        rating TEXT NOT NULL,
                        reviewedAt INTEGER NOT NULL,
                        nextDueAt INTEGER NOT NULL,
                        stability REAL NOT NULL,
                        difficulty REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        taskType TEXT NOT NULL,
                        contentId TEXT NOT NULL,
                        wordNormalized TEXT NOT NULL,
                        result TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contents ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contents ADD COLUMN importSource TEXT NOT NULL DEFAULT 'LOCAL_FILE'")
                database.execSQL("ALTER TABLE word_contexts ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE word_contexts ADD COLUMN paragraphIndex INTEGER NOT NULL DEFAULT -1")
                database.execSQL("ALTER TABLE learning_reviews ADD COLUMN elapsedDays INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE learning_reviews ADD COLUMN scheduledDays INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE learning_reviews ADD COLUMN reviewCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE learning_reviews ADD COLUMN lapses INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fsrs_card_states (
                        normalized TEXT NOT NULL PRIMARY KEY,
                        stability REAL NOT NULL,
                        difficulty REAL NOT NULL,
                        reviewCount INTEGER NOT NULL,
                        lapses INTEGER NOT NULL,
                        lastReviewAt INTEGER NOT NULL,
                        scheduledDays INTEGER NOT NULL,
                        dueAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dictionary_entries (
                        normalized TEXT NOT NULL PRIMARY KEY,
                        word TEXT NOT NULL,
                        phonetic TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        phrases TEXT NOT NULL,
                        examples TEXT NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_progress (
                        contentId TEXT NOT NULL PRIMARY KEY,
                        captionId TEXT NOT NULL,
                        cueId TEXT NOT NULL,
                        paragraphIndex INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tts_audio_cache (
                        cacheKey TEXT NOT NULL PRIMARY KEY,
                        text TEXT NOT NULL,
                        voiceId TEXT NOT NULL,
                        speed REAL NOT NULL,
                        volume REAL NOT NULL,
                        audioPath TEXT NOT NULL,
                        byteSize INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastUsedAt INTEGER NOT NULL,
                        hitCount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_task_attempts (
                        id TEXT NOT NULL PRIMARY KEY,
                        taskType TEXT NOT NULL,
                        taskItemId TEXT NOT NULL,
                        contentId TEXT NOT NULL,
                        wordNormalized TEXT NOT NULL,
                        result TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO study_task_attempts (
                        id,
                        taskType,
                        taskItemId,
                        contentId,
                        wordNormalized,
                        result,
                        score,
                        status,
                        durationMs,
                        createdAt,
                        completedAt
                    )
                    SELECT
                        id,
                        taskType,
                        '',
                        contentId,
                        wordNormalized,
                        result,
                        score,
                        'COMPLETED',
                        0,
                        createdAt,
                        createdAt
                    FROM study_records
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS platform_metadata (
                        contentId TEXT NOT NULL PRIMARY KEY,
                        platform TEXT NOT NULL,
                        requestedUrl TEXT NOT NULL,
                        canonicalUrl TEXT NOT NULL,
                        importUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        mediaUrl TEXT NOT NULL,
                        subtitleUrl TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_task_attempts_taskType_createdAt ON study_task_attempts(taskType, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_task_attempts_taskItemId ON study_task_attempts(taskItemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_task_attempts_wordNormalized ON study_task_attempts(wordNormalized)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_task_attempts_contentId ON study_task_attempts(contentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_platform_metadata_platform ON platform_metadata(platform)")
            }
        }

        private val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabulary_decks (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        stage TEXT NOT NULL,
                        goalMode TEXT NOT NULL,
                        description TEXT NOT NULL,
                        licenseSource TEXT NOT NULL,
                        itemTarget INTEGER NOT NULL,
                        displayOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS lexical_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        word TEXT NOT NULL,
                        normalized TEXT NOT NULL,
                        phonetic TEXT NOT NULL,
                        definition TEXT NOT NULL,
                        cefr TEXT NOT NULL,
                        deckId TEXT NOT NULL,
                        stage TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        phrases TEXT NOT NULL,
                        example TEXT NOT NULL,
                        licenseSource TEXT NOT NULL,
                        difficulty INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS phrase_chunks (
                        id TEXT NOT NULL PRIMARY KEY,
                        english TEXT NOT NULL,
                        chinese TEXT NOT NULL,
                        useCase TEXT NOT NULL,
                        deckId TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        licenseSource TEXT NOT NULL,
                        ttsCacheKey TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_path_attempts (
                        id TEXT NOT NULL PRIMARY KEY,
                        goalMode TEXT NOT NULL,
                        stage TEXT NOT NULL,
                        taskType TEXT NOT NULL,
                        contentId TEXT NOT NULL,
                        wordNormalized TEXT NOT NULL,
                        targetValue INTEGER NOT NULL,
                        completedValue INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        status TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        sessionId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_decks_goalMode ON vocabulary_decks(goalMode)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_decks_stage ON vocabulary_decks(stage)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_lexical_items_normalized ON lexical_items(normalized)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_lexical_items_deckId ON lexical_items(deckId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_lexical_items_stage ON lexical_items(stage)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_phrase_chunks_deckId ON phrase_chunks(deckId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_phrase_chunks_useCase ON phrase_chunks(useCase)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_path_attempts_goalMode_createdAt ON study_path_attempts(goalMode, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_path_attempts_stage_createdAt ON study_path_attempts(stage, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_path_attempts_contentId ON study_path_attempts(contentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_path_attempts_wordNormalized ON study_path_attempts(wordNormalized)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS practice_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        mode TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        targetCount INTEGER NOT NULL,
                        completedCount INTEGER NOT NULL,
                        score INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS typing_attempts (
                        id TEXT NOT NULL PRIMARY KEY,
                        sessionId TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        promptId TEXT NOT NULL,
                        expected TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        wordNormalized TEXT NOT NULL,
                        correct INTEGER NOT NULL,
                        accuracy INTEGER NOT NULL,
                        typoCount INTEGER NOT NULL,
                        wpm INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievements (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        threshold INTEGER NOT NULL,
                        unlocked INTEGER NOT NULL,
                        unlockedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievement_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        achievementId TEXT NOT NULL,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS word_game_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        gameType TEXT NOT NULL,
                        targetWord TEXT NOT NULL,
                        won INTEGER NOT NULL,
                        attempts INTEGER NOT NULL,
                        score INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS query_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        word TEXT NOT NULL,
                        normalized TEXT NOT NULL,
                        source TEXT NOT NULL,
                        favorite INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_practice_sessions_mode_createdAt ON practice_sessions(mode, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_practice_sessions_sourceId ON practice_sessions(sourceId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_typing_attempts_sessionId ON typing_attempts(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_typing_attempts_wordNormalized ON typing_attempts(wordNormalized)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_typing_attempts_createdAt ON typing_attempts(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_achievements_type ON achievements(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_achievement_events_achievementId ON achievement_events(achievementId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_achievement_events_createdAt ON achievement_events(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_word_game_sessions_gameType_createdAt ON word_game_sessions(gameType, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_word_game_sessions_targetWord ON word_game_sessions(targetWord)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_query_history_normalized ON query_history(normalized)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_query_history_createdAt ON query_history(createdAt)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.createSourceProjectTables()
            }
        }

        private fun SupportSQLiteDatabase.createSourceProjectTables() {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS typewords_dicts (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    url TEXT NOT NULL,
                    category TEXT NOT NULL,
                    tags TEXT NOT NULL,
                    translateLanguage TEXT NOT NULL,
                    dictType TEXT NOT NULL,
                    language TEXT NOT NULL,
                    lastLearnIndex INTEGER NOT NULL,
                    perDayStudyNumber INTEGER NOT NULL,
                    length INTEGER NOT NULL,
                    custom INTEGER NOT NULL,
                    complete INTEGER NOT NULL,
                    enName TEXT NOT NULL,
                    createdBy TEXT NOT NULL,
                    cover TEXT NOT NULL,
                    rawJson TEXT NOT NULL,
                    importedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS typewords_words (
                    dictId TEXT NOT NULL,
                    normalized TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    word TEXT NOT NULL,
                    phonetic0 TEXT NOT NULL,
                    phonetic1 TEXT NOT NULL,
                    definitionText TEXT NOT NULL,
                    rawJson TEXT NOT NULL,
                    importedAt INTEGER NOT NULL,
                    PRIMARY KEY(dictId, normalized)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS typewords_articles (
                    dictId TEXT NOT NULL,
                    articleKey TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    titleTranslate TEXT NOT NULL,
                    text TEXT NOT NULL,
                    textTranslate TEXT NOT NULL,
                    audioSrc TEXT NOT NULL,
                    rawJson TEXT NOT NULL,
                    importedAt INTEGER NOT NULL,
                    PRIMARY KEY(dictId, articleKey)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS typewords_settings (
                    `key` TEXT NOT NULL PRIMARY KEY,
                    valueJson TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS mujing_vocabularies (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    vocabularyType TEXT NOT NULL,
                    language TEXT NOT NULL,
                    size INTEGER NOT NULL,
                    relateVideoPath TEXT NOT NULL,
                    subtitlesTrackId INTEGER NOT NULL,
                    rawJson TEXT NOT NULL,
                    importedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS mujing_words (
                    vocabularyId TEXT NOT NULL,
                    normalized TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    value TEXT NOT NULL,
                    usphone TEXT NOT NULL,
                    ukphone TEXT NOT NULL,
                    definition TEXT NOT NULL,
                    translation TEXT NOT NULL,
                    pos TEXT NOT NULL,
                    collins INTEGER NOT NULL,
                    oxford INTEGER NOT NULL,
                    tag TEXT NOT NULL,
                    bnc INTEGER,
                    frq INTEGER,
                    exchange TEXT NOT NULL,
                    importedAt INTEGER NOT NULL,
                    PRIMARY KEY(vocabularyId, normalized)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS mujing_captions (
                    vocabularyId TEXT NOT NULL,
                    wordNormalized TEXT NOT NULL,
                    captionKind TEXT NOT NULL,
                    captionIndex INTEGER NOT NULL,
                    relateVideoPath TEXT NOT NULL,
                    subtitlesTrackId INTEGER NOT NULL,
                    subtitlesName TEXT NOT NULL,
                    startText TEXT NOT NULL,
                    endText TEXT NOT NULL,
                    content TEXT NOT NULL,
                    importedAt INTEGER NOT NULL,
                    PRIMARY KEY(vocabularyId, wordNormalized, captionKind, captionIndex)
                )
                """.trimIndent()
            )
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_dicts_dictType ON typewords_dicts(dictType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_dicts_importedAt ON typewords_dicts(importedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_words_dictId ON typewords_words(dictId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_words_normalized ON typewords_words(normalized)")
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_articles_dictId ON typewords_articles(dictId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_typewords_articles_title ON typewords_articles(title)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_vocabularies_vocabularyType ON mujing_vocabularies(vocabularyType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_vocabularies_importedAt ON mujing_vocabularies(importedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_words_vocabularyId ON mujing_words(vocabularyId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_words_normalized ON mujing_words(normalized)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_captions_vocabularyId ON mujing_captions(vocabularyId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_mujing_captions_wordNormalized ON mujing_captions(wordNormalized)")
        }
    }
}
