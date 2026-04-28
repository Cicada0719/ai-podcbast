package com.xingyue.english.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM contents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM contents ORDER BY createdAt DESC")
    fun all(): List<ContentEntity>

    @Query("SELECT * FROM contents WHERE id = :id LIMIT 1")
    fun get(id: String): ContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(content: ContentEntity)

    @Query("UPDATE contents SET favorite = :favorite WHERE id = :id")
    fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM contents WHERE id = :id")
    fun delete(id: String)
}

@Dao
interface CaptionDao {
    @Query("SELECT * FROM caption_cues WHERE sourceItemId = :contentId ORDER BY startMs ASC")
    fun observeByContentId(contentId: String): Flow<List<CaptionCueEntity>>

    @Query("SELECT * FROM caption_cues WHERE sourceItemId = :contentId ORDER BY startMs ASC")
    fun findByContentId(contentId: String): List<CaptionCueEntity>

    @Query("SELECT * FROM caption_cues WHERE captionId = :captionId ORDER BY startMs ASC")
    fun get(captionId: String): List<CaptionCueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(cues: List<CaptionCueEntity>)

    @Query("DELETE FROM caption_cues WHERE sourceItemId = :contentId")
    fun deleteForContent(contentId: String)
}

@Dao
interface LearningWordDao {
    @Query("SELECT * FROM learning_words ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LearningWordEntity>>

    @Query("SELECT * FROM learning_words ORDER BY updatedAt DESC")
    fun all(): List<LearningWordEntity>

    @Query("SELECT * FROM learning_words WHERE normalized = :normalized LIMIT 1")
    fun get(normalized: String): LearningWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(word: LearningWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContext(context: WordContextEntity)

    @Query("SELECT * FROM word_contexts WHERE wordNormalized = :normalized ORDER BY createdAt ASC")
    fun contextsForWord(normalized: String): List<WordContextEntity>

    @Query("SELECT * FROM word_contexts WHERE captionId = :captionId ORDER BY createdAt ASC")
    fun contextsForCaption(captionId: String): List<WordContextEntity>

    @Query("SELECT * FROM word_contexts WHERE sourceItemId = :contentId ORDER BY createdAt ASC")
    fun contextsForContent(contentId: String): List<WordContextEntity>

    @Query("SELECT * FROM word_contexts ORDER BY createdAt ASC")
    fun allContexts(): List<WordContextEntity>

    @Query("DELETE FROM word_contexts WHERE sourceItemId = :contentId")
    fun deleteContextsForContent(contentId: String)

    @Query("DELETE FROM learning_words WHERE normalized IN (:normalized)")
    fun deleteWords(normalized: List<String>)
}

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(review: LearningReviewEntity)

    @Query("SELECT * FROM learning_reviews WHERE normalized = :normalized ORDER BY reviewedAt DESC")
    fun reviewsForWord(normalized: String): List<LearningReviewEntity>

    @Query("SELECT * FROM learning_reviews ORDER BY reviewedAt DESC")
    fun all(): List<LearningReviewEntity>
}

@Dao
interface FsrsCardStateDao {
    @Query("SELECT * FROM fsrs_card_states WHERE normalized = :normalized LIMIT 1")
    fun get(normalized: String): FsrsCardStateEntity?

    @Query("SELECT * FROM fsrs_card_states")
    fun all(): List<FsrsCardStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: FsrsCardStateEntity)
}

@Dao
interface StudyTaskAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(attempt: StudyTaskAttemptEntity)

    @Query("SELECT * FROM study_task_attempts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudyTaskAttemptEntity>>

    @Query("SELECT * FROM study_task_attempts ORDER BY createdAt DESC")
    fun all(): List<StudyTaskAttemptEntity>

    @Query("SELECT * FROM study_task_attempts WHERE taskItemId = :taskItemId ORDER BY createdAt DESC")
    fun forTaskItem(taskItemId: String): List<StudyTaskAttemptEntity>

    @Query("SELECT * FROM study_task_attempts WHERE wordNormalized = :normalized ORDER BY createdAt DESC")
    fun forWord(normalized: String): List<StudyTaskAttemptEntity>

    @Query("SELECT * FROM study_task_attempts WHERE contentId = :contentId ORDER BY createdAt DESC")
    fun forContent(contentId: String): List<StudyTaskAttemptEntity>
}

@Dao
interface PlatformMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(metadata: PlatformMetadataEntity)

    @Query("SELECT * FROM platform_metadata WHERE contentId = :contentId LIMIT 1")
    fun get(contentId: String): PlatformMetadataEntity?

    @Query("DELETE FROM platform_metadata WHERE contentId = :contentId")
    fun delete(contentId: String)
}

@Dao
interface DictionaryEntryDao {
    @Query("SELECT COUNT(*) FROM dictionary_entries")
    fun count(): Int

    @Query("SELECT * FROM dictionary_entries WHERE normalized = :normalized LIMIT 1")
    fun get(normalized: String): DictionaryEntryEntity?

    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE normalized LIKE :prefix || '%' OR word LIKE :prefix || '%'
        ORDER BY CASE WHEN normalized = :prefix THEN 0 ELSE 1 END, normalized
        LIMIT :limit
        """
    )
    fun search(prefix: String, limit: Int = 30): List<DictionaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entries: List<DictionaryEntryEntity>)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE contentId = :contentId LIMIT 1")
    fun get(contentId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE contentId = :contentId")
    fun delete(contentId: String)
}

@Dao
interface TtsCacheDao {
    @Query("SELECT * FROM tts_audio_cache WHERE cacheKey = :cacheKey LIMIT 1")
    fun get(cacheKey: String): TtsCacheEntity?

    @Query("SELECT * FROM tts_audio_cache ORDER BY lastUsedAt DESC")
    fun all(): List<TtsCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(cache: TtsCacheEntity)

    @Query("UPDATE tts_audio_cache SET lastUsedAt = :lastUsedAt, hitCount = hitCount + 1 WHERE cacheKey = :cacheKey")
    fun markUsed(cacheKey: String, lastUsedAt: Long)

    @Query("DELETE FROM tts_audio_cache WHERE cacheKey = :cacheKey")
    fun delete(cacheKey: String)

    @Query("DELETE FROM tts_audio_cache")
    fun clear()
}

@Dao
interface VocabularyDeckDao {
    @Query("SELECT COUNT(*) FROM vocabulary_decks")
    fun count(): Int

    @Query("SELECT * FROM vocabulary_decks ORDER BY displayOrder ASC")
    fun all(): List<VocabularyDeckEntity>

    @Query("SELECT * FROM vocabulary_decks ORDER BY displayOrder ASC")
    fun observeAll(): Flow<List<VocabularyDeckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(decks: List<VocabularyDeckEntity>)
}

@Dao
interface LexicalItemDao {
    @Query("SELECT * FROM lexical_items ORDER BY difficulty ASC, word ASC")
    fun all(): List<LexicalItemEntity>

    @Query("SELECT * FROM lexical_items ORDER BY difficulty ASC, word ASC")
    fun observeAll(): Flow<List<LexicalItemEntity>>

    @Query("SELECT * FROM lexical_items ORDER BY difficulty ASC, word ASC LIMIT :limit")
    fun observeStarter(limit: Int): Flow<List<LexicalItemEntity>>

    @Query("SELECT COUNT(*) FROM lexical_items")
    fun count(): Int

    @Query("SELECT * FROM lexical_items WHERE stage = :stage ORDER BY difficulty ASC, word ASC")
    fun byStage(stage: String): List<LexicalItemEntity>

    @Query("SELECT * FROM lexical_items WHERE normalized = :normalized ORDER BY difficulty ASC LIMIT 1")
    fun get(normalized: String): LexicalItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(items: List<LexicalItemEntity>)
}

@Dao
interface PhraseChunkDao {
    @Query("SELECT * FROM phrase_chunks ORDER BY id ASC")
    fun all(): List<PhraseChunkEntity>

    @Query("SELECT * FROM phrase_chunks ORDER BY id ASC")
    fun observeAll(): Flow<List<PhraseChunkEntity>>

    @Query("SELECT COUNT(*) FROM phrase_chunks")
    fun count(): Int

    @Query("SELECT * FROM phrase_chunks WHERE useCase = :useCase ORDER BY id ASC")
    fun byUseCase(useCase: String): List<PhraseChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(chunks: List<PhraseChunkEntity>)
}

@Dao
interface StudyPathAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(attempt: StudyPathAttemptEntity)

    @Query("SELECT * FROM study_path_attempts ORDER BY createdAt DESC")
    fun all(): List<StudyPathAttemptEntity>

    @Query("SELECT * FROM study_path_attempts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudyPathAttemptEntity>>

    @Query("SELECT * FROM study_path_attempts WHERE stage = :stage ORDER BY createdAt DESC")
    fun byStage(stage: String): List<StudyPathAttemptEntity>
}

@Dao
interface PracticeSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(session: PracticeSessionEntity)

    @Query("SELECT * FROM practice_sessions ORDER BY createdAt DESC")
    fun all(): List<PracticeSessionEntity>

    @Query("SELECT * FROM practice_sessions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PracticeSessionEntity>>
}

@Dao
interface TypingAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(attempt: TypingAttemptEntity)

    @Query("SELECT * FROM typing_attempts WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun bySession(sessionId: String): List<TypingAttemptEntity>

    @Query("SELECT * FROM typing_attempts ORDER BY createdAt DESC")
    fun all(): List<TypingAttemptEntity>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, unlockedAt DESC, id ASC")
    fun all(): List<AchievementEntity>

    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, unlockedAt DESC, id ASC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE unlocked = 1")
    fun unlocked(): List<AchievementEntity>
}

@Dao
interface AchievementEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(event: AchievementEventEntity)

    @Query("SELECT * FROM achievement_events ORDER BY createdAt DESC")
    fun all(): List<AchievementEventEntity>
}

@Dao
interface WordGameSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: WordGameSessionEntity)

    @Query("SELECT * FROM word_game_sessions ORDER BY createdAt DESC")
    fun all(): List<WordGameSessionEntity>
}

@Dao
interface QueryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: QueryHistoryEntity)

    @Query("SELECT * FROM query_history ORDER BY createdAt DESC LIMIT :limit")
    fun recent(limit: Int = 50): List<QueryHistoryEntity>

    @Query("UPDATE query_history SET favorite = :favorite WHERE normalized = :normalized")
    fun setFavorite(normalized: String, favorite: Boolean)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    fun get(key: String): ConfigEntity?

    @Query("SELECT * FROM app_config")
    fun all(): List<ConfigEntity>

    @Query("SELECT * FROM app_config")
    fun observeAll(): Flow<List<ConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(config: ConfigEntity)

    @Query("DELETE FROM app_config WHERE `key` = :key")
    fun delete(key: String)
}

@Dao
interface SourceModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTypeWordsDict(dict: TypeWordsDictEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTypeWordsWords(words: List<TypeWordsWordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTypeWordsArticles(articles: List<TypeWordsArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putTypeWordsSetting(setting: TypeWordsSettingEntity)

    @Query("SELECT * FROM typewords_dicts ORDER BY importedAt DESC")
    fun typeWordsDicts(): List<TypeWordsDictEntity>

    @Query("SELECT * FROM typewords_words WHERE dictId = :dictId ORDER BY orderIndex ASC")
    fun typeWordsWords(dictId: String): List<TypeWordsWordEntity>

    @Query("SELECT * FROM typewords_articles WHERE dictId = :dictId ORDER BY orderIndex ASC")
    fun typeWordsArticles(dictId: String): List<TypeWordsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMuJingVocabulary(vocabulary: MuJingVocabularyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMuJingWords(words: List<MuJingWordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMuJingCaptions(captions: List<MuJingCaptionEntity>)

    @Query("SELECT * FROM mujing_vocabularies ORDER BY importedAt DESC")
    fun muJingVocabularies(): List<MuJingVocabularyEntity>

    @Query("SELECT * FROM mujing_words WHERE vocabularyId = :vocabularyId ORDER BY orderIndex ASC")
    fun muJingWords(vocabularyId: String): List<MuJingWordEntity>

    @Query("SELECT * FROM mujing_captions WHERE vocabularyId = :vocabularyId ORDER BY captionIndex ASC")
    fun muJingCaptions(vocabularyId: String): List<MuJingCaptionEntity>
}
