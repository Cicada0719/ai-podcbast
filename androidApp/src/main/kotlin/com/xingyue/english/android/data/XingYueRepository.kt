package com.xingyue.english.android.data

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import com.xingyue.english.android.data.db.ConfigEntity
import com.xingyue.english.android.data.db.AchievementEntity
import com.xingyue.english.android.data.db.AchievementEventEntity
import com.xingyue.english.android.data.db.LexicalItemEntity
import com.xingyue.english.android.data.db.PhraseChunkEntity
import com.xingyue.english.android.data.db.PlatformMetadataEntity
import com.xingyue.english.android.data.db.PracticeSessionEntity
import com.xingyue.english.android.data.db.QueryHistoryEntity
import com.xingyue.english.android.data.db.ReadingProgressEntity
import com.xingyue.english.android.data.db.StudyPathAttemptEntity
import com.xingyue.english.android.data.db.TypingAttemptEntity
import com.xingyue.english.android.data.db.VocabularyDeckEntity
import com.xingyue.english.android.data.db.WordGameSessionEntity
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.android.data.db.enumValueOrDefault
import com.xingyue.english.android.data.db.toCaption
import com.xingyue.english.android.data.db.toDomain
import com.xingyue.english.android.data.db.toEntity
import com.xingyue.english.android.data.db.toSpeechSynthesisResult
import com.xingyue.english.android.data.db.toTtsCacheEntity
import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.BuiltInStudyLexicon
import com.xingyue.english.core.AchievementEngine
import com.xingyue.english.core.CloudLanguageService
import com.xingyue.english.core.DictionaryEntry
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.LearningGoalMode
import com.xingyue.english.core.LearningReview
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.LexicalItem
import com.xingyue.english.core.PhraseChunk
import com.xingyue.english.core.PhraseUseCase
import com.xingyue.english.core.ReviewScheduler
import com.xingyue.english.core.OfflineLanguageService
import com.xingyue.english.core.ReviewRating
import com.xingyue.english.core.SpeechSynthesisResult
import com.xingyue.english.core.SpeechSynthesisService
import com.xingyue.english.core.PracticeMode
import com.xingyue.english.core.StudyAttemptStatus
import com.xingyue.english.core.StudyPathStep
import com.xingyue.english.core.StudyRecord
import com.xingyue.english.core.StudyTaskType
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.TypingPracticeResult
import com.xingyue.english.core.VocabularyDeck
import com.xingyue.english.core.VocabularyDeckStage
import com.xingyue.english.core.WordSelectionContext
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AppConfig(
    val bailianKey: String = "",
    val region: BailianRegion = BailianRegion.MAINLAND,
    val cacheClearedAt: Long = 0L,
    val ttsEnabled: Boolean = false,
    val ttsVoiceId: String = BailianTtsVoices.defaultVoice.id,
    val ttsSpeed: Float = 1f,
    val ttsVolume: Float = 1f,
    val learningGoalMode: LearningGoalMode = LearningGoalMode.GENERAL
)

class XingYueRepository(
    context: Context,
    private val database: XingYueDatabase = XingYueDatabase.get(context.applicationContext),
    private val ttsServiceFactory: (AppConfig, File) -> SpeechSynthesisService = { config, cacheDir ->
        BailianTtsService(
            apiKey = config.bailianKey,
            region = config.region,
            cacheDir = cacheDir
        )
    }
) {
    private val appContext = context.applicationContext
    private val configDao = database.configDao()

    val contentStore = RoomImportedContentStore(database.contentDao())
    val captionRepository = RoomBilingualCaptionRepository(database.captionDao())
    val learningWordRepository = RoomLearningWordRepository(database.learningWordDao())
    val dictionaryRepository = OfflineDictionaryRepository(appContext, database.dictionaryEntryDao())
    private val importEvents = MutableSharedFlow<ImportUiEvent>(extraBufferCapacity = 16)

    fun observeImportEvents(): SharedFlow<ImportUiEvent> = importEvents

    suspend fun initialize() = withContext(Dispatchers.IO) {
        LegacyStoreMigrator(appContext, database).migrateIfNeeded()
        dictionaryRepository.seedIfNeeded()
        seedStudyLexiconIfNeeded()
        seedAchievementsIfNeeded()
    }

    fun observeContents(): Flow<List<ImportedContent>> =
        database.contentDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun observeCaption(contentId: String): Flow<BilingualCaption?> =
        database.captionDao().observeByContentId(contentId)
            .map { it.toCaption() }
            .flowOn(Dispatchers.IO)

    fun observeWords(): Flow<List<LearningWord>> =
        database.learningWordDao().observeAll()
            .map { rows ->
                rows.map { word ->
                    word.toDomain(database.learningWordDao().contextsForWord(word.normalized))
                }
            }
            .flowOn(Dispatchers.IO)

    fun observeStudyRecords(): Flow<List<StudyRecord>> =
        database.studyTaskAttemptDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun observeConfig(): Flow<AppConfig> =
        configDao.observeAll()
            .map { rows -> rows.toConfig() }
            .flowOn(Dispatchers.IO)

    fun observeLexicalItems(): Flow<List<LexicalItem>> =
        database.lexicalItemDao().observeStarter(STARTUP_LEXICAL_LIMIT)
            .map { rows -> rows.map { it.toLexicalItem() } }
            .flowOn(Dispatchers.IO)

    fun observePhraseChunks(): Flow<List<PhraseChunk>> =
        database.phraseChunkDao().observeAll()
            .map { rows -> rows.map { it.toPhraseChunk() } }
            .flowOn(Dispatchers.IO)

    suspend fun importUri(uri: Uri): ImportedContent = withContext(Dispatchers.IO) {
        runCatching { ImportProcessor(appContext, this@XingYueRepository).importUri(uri) }
            .onSuccess { emitImportResult(it, "文件已导入") }
            .onFailure { emitImportFailure(it, "文件导入失败") }
            .getOrThrow()
    }

    suspend fun importDirectUrl(url: String): ImportedContent = withContext(Dispatchers.IO) {
        runCatching { ImportProcessor(appContext, this@XingYueRepository).importDirectUrl(url) }
            .onSuccess { emitImportResult(it, "链接已导入") }
            .onFailure { emitImportFailure(it, "链接导入失败") }
            .getOrThrow()
    }

    internal fun importSourceProjectJson(content: ImportedContent, rawText: String): ImportedContent? =
        SourceProjectImporter(database).importIfRecognized(content, rawText)

    suspend fun importDictionary(uri: Uri): Int = withContext(Dispatchers.IO) {
        val extension = uri.lastPathSegment?.substringAfterLast('.', "json") ?: "json"
        val target = File(appContext.cacheDir, "dictionary-import-${System.currentTimeMillis()}.$extension")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext 0
        dictionaryRepository.importDictionary(target)
    }

    suspend fun exportBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val json = SqliteBackupManager(database).exportJson()
        appContext.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("无法写入备份文件")
        BackupResult(
            tableCount = JSONObject(json).getJSONObject("tables").length(),
            rowCount = JSONObject(json).getJSONObject("tables").keys().asSequence().sumOf { table ->
                JSONObject(json).getJSONObject("tables").getJSONArray(table).length()
            }
        )
    }

    suspend fun importBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("无法读取备份文件")
        SqliteBackupManager(database).importJson(text)
    }

    suspend fun retryProcessing(contentId: String): ImportedContent? = withContext(Dispatchers.IO) {
        ImportProcessor(appContext, this@XingYueRepository).process(contentId)
    }

    suspend fun addFromSelection(context: WordSelectionContext): LearningWord = withContext(Dispatchers.IO) {
        learningWordRepository.addFromSelection(context).also {
            unlockCurrentAchievements()
        }
    }

    suspend fun markWord(normalized: String, status: LearningWordStatus): LearningWord? = withContext(Dispatchers.IO) {
        learningWordRepository.setStatus(normalized, status)
    }

    suspend fun updateNotes(normalized: String, notes: String): LearningWord? = withContext(Dispatchers.IO) {
        learningWordRepository.updateNotes(normalized, notes)
    }

    suspend fun reviewWord(normalized: String, rating: ReviewRating): LearningReview? = withContext(Dispatchers.IO) {
        val current = learningWordRepository.search(normalized).firstOrNull { it.normalized == normalized } ?: return@withContext null
        val currentState = database.fsrsCardStateDao().get(normalized)?.toDomain()
        val result = ReviewScheduler.review(current, currentState, rating)
        database.learningWordDao().upsert(result.word.toEntity())
        database.fsrsCardStateDao().upsert(result.cardState.toEntity())
        val review = result.review
        database.reviewDao().insert(review.toEntity())
        review
    }

    suspend fun recordStudyTask(
        taskType: StudyTaskType,
        taskItemId: String = "",
        contentId: String = "",
        wordNormalized: String = "",
        result: String = "completed",
        score: Int = 0,
        status: StudyAttemptStatus = StudyAttemptStatus.COMPLETED,
        durationMs: Long = 0L
    ): StudyRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val record = StudyRecord(
            id = "attempt-${UUID.randomUUID()}",
            taskType = taskType,
            taskItemId = taskItemId,
            contentId = contentId,
            wordNormalized = wordNormalized,
            result = result,
            score = score,
            status = status,
            durationMs = durationMs,
            createdAt = now,
            completedAt = if (status == StudyAttemptStatus.STARTED) 0L else now
        )
        database.studyTaskAttemptDao().insert(record.toEntity())
        record
    }

    suspend fun recordStudyPathStep(
        step: StudyPathStep,
        sessionId: String = "session-${System.currentTimeMillis()}",
        completedValue: Int = step.goalValue,
        durationMs: Long = step.estimatedMinutes * 60_000L
    ): StudyRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.studyPathAttemptDao().insert(
            StudyPathAttemptEntity(
                id = "path-attempt-${UUID.randomUUID()}",
                goalMode = step.goalMode.name,
                stage = step.stage.name,
                taskType = step.taskType?.name.orEmpty(),
                contentId = step.contentId,
                wordNormalized = step.wordNormalized,
                targetValue = step.goalValue,
                completedValue = completedValue.coerceAtLeast(step.progressValue),
                result = "path:${step.stage.name}",
                status = StudyAttemptStatus.COMPLETED.name,
                durationMs = durationMs,
                sessionId = sessionId,
                createdAt = now,
                completedAt = now
            )
        )
        recordStudyTask(
            taskType = step.taskType ?: StudyTaskType.NEW_WORDS,
            taskItemId = step.id,
            contentId = step.contentId,
            wordNormalized = step.wordNormalized,
            result = "path:${step.stage.name}",
            score = 100,
            status = StudyAttemptStatus.COMPLETED,
            durationMs = durationMs
        )
    }

    suspend fun recordTypingPractice(
        mode: PracticeMode,
        result: TypingPracticeResult,
        sourceId: String = "",
        sourceType: String = "",
        durationMs: Long = 0L
    ): StudyRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sessionId = "practice-session-${UUID.randomUUID()}"
        database.practiceSessionDao().upsert(
            PracticeSessionEntity(
                id = sessionId,
                mode = mode.name,
                sourceId = sourceId,
                sourceType = sourceType,
                targetCount = 1,
                completedCount = 1,
                score = result.accuracy,
                durationMs = durationMs,
                createdAt = now,
                completedAt = now
            )
        )
        database.typingAttemptDao().insert(
            TypingAttemptEntity(
                id = "typing-${UUID.randomUUID()}",
                sessionId = sessionId,
                mode = mode.name,
                promptId = result.promptId,
                expected = result.expected,
                answer = result.answer,
                wordNormalized = result.normalizedExpected.ifBlank { TextTools.normalizeWord(result.expected) },
                correct = result.correct,
                accuracy = result.accuracy,
                typoCount = result.typoCount,
                wpm = result.wpm,
                durationMs = durationMs,
                createdAt = now
            )
        )
        unlockCurrentAchievements(typingResults = listOf(result))
        recordStudyTask(
            taskType = mode.toStudyTaskType(),
            taskItemId = result.promptId,
            contentId = sourceId,
            wordNormalized = result.normalizedExpected,
            result = if (result.correct) "typing-correct" else "typing-wrong",
            score = result.accuracy,
            status = if (result.correct) StudyAttemptStatus.COMPLETED else StudyAttemptStatus.FAILED,
            durationMs = durationMs
        )
    }

    suspend fun recordTypingPracticeSession(
        mode: PracticeMode,
        results: List<TypingPracticeResult>,
        sourceId: String = "",
        sourceType: String = "",
        targetCount: Int = results.size,
        durationMs: Long = 0L
    ): StudyRecord? = withContext(Dispatchers.IO) {
        val scoped = results.distinctBy { it.promptId }
        if (scoped.isEmpty()) return@withContext null
        val now = System.currentTimeMillis()
        val sessionId = "practice-session-${UUID.randomUUID()}"
        val averageScore = scoped.sumOf { it.accuracy } / scoped.size
        database.practiceSessionDao().upsert(
            PracticeSessionEntity(
                id = sessionId,
                mode = mode.name,
                sourceId = sourceId,
                sourceType = sourceType,
                targetCount = targetCount.coerceAtLeast(scoped.size),
                completedCount = scoped.size,
                score = averageScore,
                durationMs = durationMs,
                createdAt = now,
                completedAt = now
            )
        )
        scoped.forEach { result ->
            database.typingAttemptDao().insert(
                TypingAttemptEntity(
                    id = "typing-${UUID.randomUUID()}",
                    sessionId = sessionId,
                    mode = mode.name,
                    promptId = result.promptId,
                    expected = result.expected,
                    answer = result.answer,
                    wordNormalized = result.normalizedExpected.ifBlank { TextTools.normalizeWord(result.expected) },
                    correct = result.correct,
                    accuracy = result.accuracy,
                    typoCount = result.typoCount,
                    wpm = result.wpm,
                    durationMs = durationMs / scoped.size.coerceAtLeast(1),
                    createdAt = now
                )
            )
        }
        unlockCurrentAchievements(typingResults = scoped)
        recordStudyTask(
            taskType = mode.toStudyTaskType(),
            taskItemId = sessionId,
            contentId = sourceId,
            wordNormalized = scoped.firstOrNull()?.normalizedExpected.orEmpty(),
            result = "practice-session:${mode.name}:${scoped.count { it.correct }}/${targetCount.coerceAtLeast(scoped.size)}",
            score = averageScore,
            status = StudyAttemptStatus.COMPLETED,
            durationMs = durationMs
        )
    }

    private fun PracticeMode.toStudyTaskType(): StudyTaskType =
        when (this) {
            PracticeMode.DICTATION,
            PracticeMode.CN_TO_EN,
            PracticeMode.SPELLING_MEMORY,
            PracticeMode.TYPEWORDS_SYSTEM,
            PracticeMode.TYPEWORDS_FREE,
            PracticeMode.TYPEWORDS_REVIEW,
            PracticeMode.TYPEWORDS_SHUFFLE -> StudyTaskType.SPELLING
            PracticeMode.COPY_TYPING,
            PracticeMode.SELF_TEST -> StudyTaskType.NEW_WORDS
        }

    suspend fun recordWordGame(
        gameType: String,
        targetWord: String,
        won: Boolean,
        attempts: Int,
        score: Int
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.wordGameSessionDao().insert(
            WordGameSessionEntity(
                id = "word-game-${UUID.randomUUID()}",
                gameType = gameType,
                targetWord = TextTools.normalizeWord(targetWord),
                won = won,
                attempts = attempts,
                score = score,
                createdAt = now,
                completedAt = now
            )
        )
        unlockCurrentAchievements(wonWordGames = if (won) 1 else 0)
    }

    suspend fun recordQueryHistory(word: String, source: String) = withContext(Dispatchers.IO) {
        val normalized = TextTools.normalizeWord(word)
        if (normalized.isBlank()) return@withContext
        database.queryHistoryDao().insert(
            QueryHistoryEntity(
                id = "query-${UUID.randomUUID()}",
                word = word,
                normalized = normalized,
                source = source,
                favorite = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveReadingProgress(
        contentId: String,
        captionId: String,
        cueId: String,
        paragraphIndex: Int,
        positionMs: Long
    ) = withContext(Dispatchers.IO) {
        database.readingProgressDao().upsert(
            ReadingProgressEntity(
                contentId = contentId,
                captionId = captionId,
                cueId = cueId,
                paragraphIndex = paragraphIndex,
                positionMs = positionMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun readingProgress(contentId: String): ReadingProgressEntity? = withContext(Dispatchers.IO) {
        database.readingProgressDao().get(contentId)
    }

    suspend fun toggleFavorite(contentId: String) = withContext(Dispatchers.IO) {
        val content = database.contentDao().get(contentId) ?: return@withContext
        database.contentDao().setFavorite(contentId, !content.favorite)
    }

    suspend fun deleteContent(contentId: String, deleteSourceWords: Boolean) = withContext(Dispatchers.IO) {
        val affectedWords = database.learningWordDao().contextsForContent(contentId).map { it.wordNormalized }.toSet()
        database.contentDao().delete(contentId)
        database.captionDao().deleteForContent(contentId)
        database.readingProgressDao().delete(contentId)
        database.platformMetadataDao().delete(contentId)
        if (deleteSourceWords) {
            database.learningWordDao().deleteContextsForContent(contentId)
            val orphaned = affectedWords.filter { database.learningWordDao().contextsForWord(it).isEmpty() }
            if (orphaned.isNotEmpty()) database.learningWordDao().deleteWords(orphaned)
        }
    }

    suspend fun saveBailianKey(key: String) = withContext(Dispatchers.IO) {
        configDao.put(ConfigEntity(LegacyStoreMigrator.KEY_BAILIAN_KEY, key.trim()))
    }

    suspend fun saveRegion(region: BailianRegion) = withContext(Dispatchers.IO) {
        configDao.put(ConfigEntity(LegacyStoreMigrator.KEY_REGION, region.name))
    }

    suspend fun clearCacheOnly() = withContext(Dispatchers.IO) {
        deleteTtsCacheFiles()
        database.ttsCacheDao().clear()
        configDao.put(ConfigEntity("cache_cleared_at", System.currentTimeMillis().toString()))
    }

    suspend fun saveTtsConfig(enabled: Boolean, voiceId: String, speed: Float, volume: Float) = withContext(Dispatchers.IO) {
        configDao.put(ConfigEntity(KEY_TTS_ENABLED, enabled.toString()))
        configDao.put(ConfigEntity(KEY_TTS_VOICE, voiceId))
        configDao.put(ConfigEntity(KEY_TTS_SPEED, speed.coerceIn(0.5f, 2.0f).toString()))
        configDao.put(ConfigEntity(KEY_TTS_VOLUME, volume.coerceIn(0.1f, 2.0f).toString()))
    }

    suspend fun saveLearningGoalMode(mode: LearningGoalMode) = withContext(Dispatchers.IO) {
        configDao.put(ConfigEntity(KEY_LEARNING_GOAL, mode.name))
    }

    suspend fun testBailianConnection(keyOverride: String? = null): CloudCheckResult = withContext(Dispatchers.IO) {
        val config = configDao.all().toConfig()
        val key = keyOverride?.trim() ?: config.bailianKey
        BailianLanguageService(key, config.region).testConnection()
    }

    suspend fun lookupWord(word: String): DictionaryEntry = withContext(Dispatchers.IO) {
        recordQueryHistory(word, "lookup")
        val local = dictionaryRepository.lookup(word)
        if (local != null) return@withContext local
        val cloud = languageService().lookup(word)
        val entry = DictionaryEntry(
            word = cloud.word,
            phonetic = cloud.phonetic,
            definition = cloud.chinese.ifBlank { BailianLanguageService.fallbackDefinition(word) },
            phrases = cloud.phrases,
            examples = cloud.examples,
            source = if (cloud.chinese.isBlank()) "本地词典" else "云端词典"
        )
        if (cloud.chinese.isNotBlank() || cloud.phonetic.isNotBlank() || cloud.phrases.isNotEmpty() || cloud.examples.isNotEmpty()) {
            database.dictionaryEntryDao().upsertAll(listOf(entry.toEntity()))
        }
        entry
    }

    suspend fun synthesizeSpeech(text: String): SpeechSynthesisResult? = withContext(Dispatchers.IO) {
        val config = configDao.all().toConfig()
        val spokenText = text.trim().take(MAX_TTS_TEXT_LENGTH)
        if (!config.ttsEnabled || config.bailianKey.isBlank() || spokenText.isBlank()) return@withContext null

        val voiceId = BailianTtsVoices.find(config.ttsVoiceId).id
        val speed = config.ttsSpeed.coerceIn(0.5f, 2.0f)
        val volume = config.ttsVolume.coerceIn(0.1f, 2.0f)
        val cacheDir = ttsCacheDir()
        val cacheKey = BailianTtsService.cacheKey(spokenText, voiceId, speed, volume)
        val cached = database.ttsCacheDao().get(cacheKey)
        if (cached != null) {
            val cachedFile = File(cached.audioPath)
            if (cachedFile.exists() && cachedFile.length() > 0) {
                database.ttsCacheDao().markUsed(cacheKey, System.currentTimeMillis())
                return@withContext cached.toSpeechSynthesisResult()
            }
            database.ttsCacheDao().delete(cacheKey)
        }

        val result = ttsServiceFactory(config, cacheDir).synthesize(spokenText, voiceId, speed, volume)
        val audioFile = File(result.audioPath)
        if (audioFile.exists() && audioFile.length() > 0) {
            database.ttsCacheDao().upsert(result.toTtsCacheEntity(cacheKey, speed, volume))
            pruneTtsCache()
        }
        result
    }

    fun savePlatformMetadata(
        contentId: String,
        resolution: PlatformLinkResolver.LinkResolution
    ) {
        database.platformMetadataDao().upsert(
            PlatformMetadataEntity(
                contentId = contentId,
                platform = resolution.platform.name,
                requestedUrl = resolution.requestedUrl,
                canonicalUrl = resolution.sourceUrl,
                importUrl = resolution.importUrl,
                title = resolution.title,
                coverUrl = "",
                mediaUrl = if (resolution.kind == com.xingyue.english.core.SourceType.AUDIO || resolution.kind == com.xingyue.english.core.SourceType.VIDEO) {
                    resolution.importUrl
                } else {
                    ""
                },
                subtitleUrl = if (resolution.kind == com.xingyue.english.core.SourceType.SUBTITLE) resolution.importUrl else "",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    internal fun savePlatformMetadata(
        contentId: String,
        result: PlatformImportResult.Success
    ) {
        database.platformMetadataDao().upsert(
            PlatformMetadataEntity(
                contentId = contentId,
                platform = result.mediaInfo.platform.name,
                requestedUrl = result.mediaInfo.originalUrl,
                canonicalUrl = result.mediaInfo.canonicalUrl,
                importUrl = result.selectedMedia?.url ?: result.selectedSubtitle?.url.orEmpty(),
                title = result.mediaInfo.title,
                coverUrl = result.mediaInfo.thumbnailUrl,
                mediaUrl = result.selectedMedia?.url.orEmpty(),
                subtitleUrl = result.selectedSubtitle?.url.orEmpty(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    internal fun savePlatformMetadata(
        contentId: String,
        result: PlatformImportResult.Failure
    ) {
        database.platformMetadataDao().upsert(
            PlatformMetadataEntity(
                contentId = contentId,
                platform = result.platform.name,
                requestedUrl = result.originalUrl,
                canonicalUrl = result.originalUrl,
                importUrl = "",
                title = result.message,
                coverUrl = "",
                mediaUrl = "",
                subtitleUrl = "",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun languageService(): CloudLanguageService {
        val config = configDao.all().toConfig()
        return if (config.bailianKey.isBlank()) {
            OfflineDictionaryLanguageService(dictionaryRepository)
        } else {
            DictionaryFirstLanguageService(
                offlineDictionary = dictionaryRepository,
                cloud = BailianLanguageService(config.bailianKey, config.region)
            )
        }
    }

    suspend fun reportImportIssue(message: String, severity: ImportUiSeverity = ImportUiSeverity.ERROR) {
        importEvents.emit(ImportUiEvent(message, severity))
    }

    private suspend fun emitImportResult(content: ImportedContent, prefix: String) {
        val severity = when (content.status) {
            com.xingyue.english.core.ImportProcessingStatus.READY_TO_LEARN -> ImportUiSeverity.SUCCESS
            com.xingyue.english.core.ImportProcessingStatus.FAILED,
            com.xingyue.english.core.ImportProcessingStatus.NEEDS_CLOUD_KEY,
            com.xingyue.english.core.ImportProcessingStatus.NEEDS_MEDIA_ENGINE -> ImportUiSeverity.WARNING
            else -> ImportUiSeverity.INFO
        }
        val message = "$prefix：${content.title}，${content.statusMessage}"
        importEvents.emit(ImportUiEvent(message, severity, actionLabel = if (severity == ImportUiSeverity.WARNING) "查看" else null))
    }

    private suspend fun emitImportFailure(error: Throwable, prefix: String) {
        val raw = error.message.orEmpty()
        val message = when {
            raw.contains("file", ignoreCase = true) ||
                raw.contains("权限") ||
                raw.contains("EACCES", ignoreCase = true) ||
                raw.contains("Permission", ignoreCase = true) ->
                "$prefix：没有文件读取权限，请通过系统文件选择器导入。"
            raw.isNotBlank() -> "$prefix：$raw"
            else -> "$prefix：请检查文件格式或网络连接。"
        }
        importEvents.emit(ImportUiEvent(message, ImportUiSeverity.ERROR))
    }

    private fun List<ConfigEntity>.toConfig(): AppConfig {
        val values = associateBy({ it.key }, { it.value })
        return AppConfig(
            bailianKey = values[LegacyStoreMigrator.KEY_BAILIAN_KEY].orEmpty(),
            region = enumValueOrDefault(values[LegacyStoreMigrator.KEY_REGION].orEmpty(), BailianRegion.MAINLAND),
            cacheClearedAt = values["cache_cleared_at"]?.toLongOrNull() ?: 0L,
            ttsEnabled = values[KEY_TTS_ENABLED]?.toBooleanStrictOrNull() ?: false,
            ttsVoiceId = values[KEY_TTS_VOICE].orEmpty().ifBlank { BailianTtsVoices.defaultVoice.id },
            ttsSpeed = values[KEY_TTS_SPEED]?.toFloatOrNull() ?: 1f,
            ttsVolume = values[KEY_TTS_VOLUME]?.toFloatOrNull() ?: 1f,
            learningGoalMode = enumValueOrDefault(values[KEY_LEARNING_GOAL].orEmpty(), LearningGoalMode.GENERAL)
        )
    }

    private fun seedStudyLexiconIfNeeded() {
        val now = System.currentTimeMillis()
        if (database.vocabularyDeckDao().count() == 0) {
            val decks = (BuiltInStudyLexicon.decks + readIntegratedDecks()).distinctBy { it.id }
            database.vocabularyDeckDao().upsertAll(
                decks.flatMap { deck ->
                    deck.goalModes.map { goalMode ->
                        VocabularyDeckEntity(
                            id = "${deck.id}-${goalMode.name.lowercase()}",
                            name = deck.name,
                            stage = deck.stage.name,
                            goalMode = goalMode.name,
                            description = deck.description,
                            licenseSource = deck.licenseSource,
                            itemTarget = deck.itemTarget,
                            displayOrder = deck.order,
                            createdAt = now
                        )
                    }
                }
            )
        }
        if (database.lexicalItemDao().count() == 0) {
            database.lexicalItemDao().upsertAll(BuiltInStudyLexicon.lexicalItems.map { it.toEntity(now) })
            streamIntegratedLexicalItems(now, BuiltInStudyLexicon.lexicalItems.mapTo(mutableSetOf()) { it.id })
        }
        if (database.phraseChunkDao().count() == 0) {
            val builtIn = BuiltInStudyLexicon.phraseChunks.map { it.toEntity(now) }
            if (builtIn.isNotEmpty()) database.phraseChunkDao().upsertAll(builtIn)
            streamIntegratedPhraseChunks(now, BuiltInStudyLexicon.phraseChunks.mapTo(mutableSetOf()) { it.id })
        }
    }

    private fun readIntegratedDecks(): List<VocabularyDeck> =
        streamIntegratedSeed { reader ->
            val decks = mutableListOf<VocabularyDeck>()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "decks" -> {
                        reader.beginArray()
                        while (reader.hasNext()) readIntegratedDeck(reader)?.let(decks::add)
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            decks
        }.getOrDefault(emptyList())

    private fun streamIntegratedLexicalItems(now: Long, seenIds: MutableSet<String>) {
        streamIntegratedSeed { reader ->
            val chunk = mutableListOf<LexicalItemEntity>()
            fun flush() {
                if (chunk.isNotEmpty()) {
                    database.lexicalItemDao().upsertAll(chunk.toList())
                    chunk.clear()
                }
            }
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "items" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            readIntegratedLexicalEntity(reader, now)?.let { entity ->
                                if (seenIds.add(entity.id)) {
                                    chunk += entity
                                    if (chunk.size >= SEED_CHUNK_SIZE) flush()
                                }
                            }
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            flush()
        }
    }

    private fun streamIntegratedPhraseChunks(now: Long, seenIds: MutableSet<String>) {
        streamIntegratedSeed { reader ->
            val chunk = mutableListOf<PhraseChunkEntity>()
            fun flush() {
                if (chunk.isNotEmpty()) {
                    database.phraseChunkDao().upsertAll(chunk.toList())
                    chunk.clear()
                }
            }
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "phrases" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            readIntegratedPhraseEntity(reader, now)?.let { entity ->
                                if (seenIds.add(entity.id)) {
                                    chunk += entity
                                    if (chunk.size >= SEED_CHUNK_SIZE) flush()
                                }
                            }
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            flush()
        }
    }

    private fun <T> streamIntegratedSeed(block: (JsonReader) -> T): Result<T> =
        runCatching {
            appContext.assets.open("vocabulary/integrated_sources_seed.json").use { input ->
                JsonReader(InputStreamReader(input, Charsets.UTF_8)).use(block)
            }
        }

    private fun readIntegratedDeck(reader: JsonReader): VocabularyDeck? {
        var id = ""
        var name = ""
        var stage = VocabularyDeckStage.CORE_3500
        var goalModes = setOf(LearningGoalMode.GENERAL)
        var description = ""
        var licenseSource = "Integrated source asset"
        var itemTarget = 0
        var order = 1000
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextStringValue()
                "name" -> name = reader.nextStringValue()
                "stage" -> stage = enumValueOrDefault(reader.nextStringValue(), VocabularyDeckStage.CORE_3500)
                "goalModes" -> goalModes = reader.nextStringArray()
                    .map { enumValueOrDefault(it, LearningGoalMode.GENERAL) }
                    .toSet()
                    .ifEmpty { setOf(LearningGoalMode.GENERAL) }
                "description" -> description = reader.nextStringValue()
                "licenseSource" -> licenseSource = reader.nextStringValue().ifBlank { "Integrated source asset" }
                "itemTarget" -> itemTarget = reader.nextIntValue()
                "order" -> order = reader.nextIntValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (id.isBlank()) return null
        return VocabularyDeck(
            id = id,
            name = name.ifBlank { id },
            stage = stage,
            goalModes = goalModes,
            description = description,
            licenseSource = licenseSource,
            itemTarget = itemTarget,
            order = order
        )
    }

    private fun readIntegratedLexicalEntity(reader: JsonReader, now: Long): LexicalItemEntity? {
        var id = ""
        var word = ""
        var normalized = ""
        var phonetic = ""
        var definition = ""
        var cefr = ""
        var deckId = "core-3500"
        var stage = VocabularyDeckStage.CORE_3500
        var tags = emptyList<String>()
        var phrases = emptyList<String>()
        var example = ""
        var licenseSource = "Integrated source asset"
        var difficulty = 1
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextStringValue()
                "word" -> word = reader.nextStringValue()
                "normalized" -> normalized = reader.nextStringValue()
                "phonetic" -> phonetic = reader.nextStringValue()
                "definition" -> definition = reader.nextStringValue()
                "cefr" -> cefr = reader.nextStringValue()
                "deckId" -> deckId = reader.nextStringValue().ifBlank { "core-3500" }
                "stage" -> stage = enumValueOrDefault(reader.nextStringValue(), VocabularyDeckStage.CORE_3500)
                "tags" -> tags = reader.nextStringArray()
                "phrases" -> phrases = reader.nextStringArray()
                "example" -> example = reader.nextStringValue()
                "licenseSource" -> licenseSource = reader.nextStringValue().ifBlank { "Integrated source asset" }
                "difficulty" -> difficulty = reader.nextIntValue().coerceAtLeast(1)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        normalized = normalized.ifBlank { TextTools.normalizeWord(word) }
        if (word.isBlank() || normalized.isBlank()) return null
        return LexicalItemEntity(
            id = id.ifBlank { "asset-$normalized" },
            word = word,
            normalized = normalized,
            phonetic = phonetic,
            definition = definition.ifBlank { "待补全释义" },
            cefr = cefr,
            deckId = deckId,
            stage = stage.name,
            tags = tags.joinToString("|"),
            phrases = phrases.joinToString("|"),
            example = example,
            licenseSource = licenseSource,
            difficulty = difficulty,
            createdAt = now
        )
    }

    private fun readIntegratedPhraseEntity(reader: JsonReader, now: Long): PhraseChunkEntity? {
        var id = ""
        var english = ""
        var chinese = ""
        var useCase = PhraseUseCase.DAILY
        var deckId = "core-phrases-1200"
        var keywords = emptyList<String>()
        var licenseSource = "Integrated source asset"
        var ttsCacheKey = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextStringValue()
                "english" -> english = reader.nextStringValue()
                "chinese" -> chinese = reader.nextStringValue()
                "useCase" -> useCase = enumValueOrDefault(reader.nextStringValue(), PhraseUseCase.DAILY)
                "deckId" -> deckId = reader.nextStringValue().ifBlank { "core-phrases-1200" }
                "keywords" -> keywords = reader.nextStringArray()
                "licenseSource" -> licenseSource = reader.nextStringValue().ifBlank { "Integrated source asset" }
                "ttsCacheKey" -> ttsCacheKey = reader.nextStringValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (english.isBlank()) return null
        return PhraseChunkEntity(
            id = id.ifBlank { "asset-phrase-${TextTools.normalizeWord(english).take(24)}" },
            english = english,
            chinese = chinese,
            useCase = useCase.name,
            deckId = deckId,
            keywords = keywords.joinToString("|"),
            licenseSource = licenseSource,
            ttsCacheKey = ttsCacheKey,
            createdAt = now
        )
    }

    private fun readIntegratedLexiconSeed(): IntegratedLexiconSeed =
        runCatching {
            val text = appContext.assets.open("vocabulary/integrated_sources_seed.json")
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(text)
            IntegratedLexiconSeed(
                decks = root.optJSONArray("decks").orEmptyObjects().mapNotNull { item ->
                    val id = item.optString("id")
                    if (id.isBlank()) return@mapNotNull null
                    val stage = enumValueOrDefault(item.optString("stage"), VocabularyDeckStage.CORE_3500)
                    VocabularyDeck(
                        id = id,
                        name = item.optString("name").ifBlank { id },
                        stage = stage,
                        goalModes = item.optJSONArray("goalModes").orEmptyStrings()
                            .map { enumValueOrDefault(it, LearningGoalMode.GENERAL) }
                            .toSet()
                            .ifEmpty { setOf(LearningGoalMode.GENERAL) },
                        description = item.optString("description"),
                        licenseSource = item.optString("licenseSource").ifBlank { "Integrated source asset" },
                        itemTarget = item.optInt("itemTarget", 0),
                        order = item.optInt("order", 1000)
                    )
                },
                items = root.optJSONArray("items").orEmptyObjects().mapNotNull { item ->
                    val word = item.optString("word")
                    val normalized = item.optString("normalized").ifBlank { TextTools.normalizeWord(word) }
                    if (word.isBlank() || normalized.isBlank()) return@mapNotNull null
                    LexicalItem(
                        id = item.optString("id").ifBlank { "asset-$normalized" },
                        word = word,
                        normalized = normalized,
                        phonetic = item.optString("phonetic"),
                        definition = item.optString("definition").ifBlank { "待补全释义" },
                        cefr = item.optString("cefr"),
                        deckId = item.optString("deckId").ifBlank { "core-3500" },
                        stage = enumValueOrDefault(item.optString("stage"), VocabularyDeckStage.CORE_3500),
                        tags = item.optJSONArray("tags").orEmptyStrings().toSet(),
                        phrases = item.optJSONArray("phrases").orEmptyStrings(),
                        example = item.optString("example"),
                        licenseSource = item.optString("licenseSource").ifBlank { "Integrated source asset" },
                        difficulty = item.optInt("difficulty", 1).coerceAtLeast(1)
                    )
                },
                phrases = root.optJSONArray("phrases").orEmptyObjects().mapNotNull { item ->
                    val english = item.optString("english")
                    if (english.isBlank()) return@mapNotNull null
                    PhraseChunk(
                        id = item.optString("id").ifBlank { "asset-phrase-${TextTools.normalizeWord(english).take(24)}" },
                        english = english,
                        chinese = item.optString("chinese"),
                        useCase = enumValueOrDefault(item.optString("useCase"), PhraseUseCase.DAILY),
                        deckId = item.optString("deckId").ifBlank { "core-phrases-1200" },
                        keywords = item.optJSONArray("keywords").orEmptyStrings(),
                        licenseSource = item.optString("licenseSource").ifBlank { "Integrated source asset" },
                        ttsCacheKey = item.optString("ttsCacheKey")
                    )
                }
            )
        }.getOrDefault(IntegratedLexiconSeed())

    private data class IntegratedLexiconSeed(
        val decks: List<VocabularyDeck> = emptyList(),
        val items: List<LexicalItem> = emptyList(),
        val phrases: List<PhraseChunk> = emptyList()
    )

    private fun LexicalItem.toEntity(now: Long): LexicalItemEntity =
        LexicalItemEntity(
            id = id,
            word = word,
            normalized = normalized,
            phonetic = phonetic,
            definition = definition,
            cefr = cefr,
            deckId = deckId,
            stage = stage.name,
            tags = tags.joinToString("|"),
            phrases = phrases.joinToString("|"),
            example = example,
            licenseSource = licenseSource,
            difficulty = difficulty,
            createdAt = now
        )

    private fun PhraseChunk.toEntity(now: Long): PhraseChunkEntity =
        PhraseChunkEntity(
            id = id,
            english = english,
            chinese = chinese,
            useCase = useCase.name,
            deckId = deckId,
            keywords = keywords.joinToString("|"),
            licenseSource = licenseSource,
            ttsCacheKey = ttsCacheKey,
            createdAt = now
        )

    private fun JsonReader.nextStringValue(): String =
        when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                ""
            }
            JsonToken.NUMBER -> nextString()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            else -> nextString()
        }

    private fun JsonReader.nextIntValue(): Int =
        when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                0
            }
            JsonToken.NUMBER -> nextInt()
            else -> nextString().toIntOrNull() ?: 0
        }

    private fun JsonReader.nextStringArray(): List<String> {
        if (peek() == JsonToken.NULL) {
            nextNull()
            return emptyList()
        }
        val values = mutableListOf<String>()
        beginArray()
        while (hasNext()) {
            val value = nextStringValue()
            if (value.isNotBlank()) values += value
        }
        endArray()
        return values
    }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optJSONObject(index) }
    }

    private fun JSONArray?.orEmptyStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
    }

    private fun LexicalItemEntity.toLexicalItem(): LexicalItem =
        LexicalItem(
            id = id,
            word = word,
            normalized = normalized,
            phonetic = phonetic,
            definition = definition,
            cefr = cefr,
            deckId = deckId,
            stage = enumValueOrDefault(stage, VocabularyDeckStage.CORE_3500),
            tags = tags.split('|').map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            phrases = phrases.split('|').map { it.trim() }.filter { it.isNotBlank() },
            example = example,
            licenseSource = licenseSource,
            difficulty = difficulty
        )

    private fun PhraseChunkEntity.toPhraseChunk(): PhraseChunk =
        PhraseChunk(
            id = id,
            english = english,
            chinese = chinese,
            useCase = enumValueOrDefault(useCase, PhraseUseCase.DAILY),
            deckId = deckId,
            keywords = keywords.split('|').map { it.trim() }.filter { it.isNotBlank() },
            licenseSource = licenseSource,
            ttsCacheKey = ttsCacheKey
        )

    private fun seedAchievementsIfNeeded() {
        val existing = database.achievementDao().all().map { it.id }.toSet()
        val rows = AchievementEngine.defaultDefinitions
            .filter { it.id !in existing }
            .map { definition ->
                AchievementEntity(
                    id = definition.id,
                    type = definition.type.name,
                    title = definition.title,
                    description = definition.description,
                    threshold = definition.threshold,
                    unlocked = false,
                    unlockedAt = 0L
                )
            }
        if (rows.isNotEmpty()) database.achievementDao().upsertAll(rows)
    }

    private fun unlockCurrentAchievements(
        typingResults: List<TypingPracticeResult> = emptyList(),
        wonWordGames: Int = 0
    ) {
        seedAchievementsIfNeeded()
        val rows = database.achievementDao().all()
        val unlockedIds = rows.filter { it.unlocked }.map { it.id }.toSet()
        val contents = database.contentDao().all().map { it.toDomain() }
        val words = database.learningWordDao().all().map { word ->
            word.toDomain(database.learningWordDao().contextsForWord(word.normalized))
        }
        val attempts = database.studyTaskAttemptDao().all().map { it.toDomain() }
        val unlocks = AchievementEngine.evaluate(
            contents = contents,
            words = words,
            attempts = attempts,
            typingResults = typingResults,
            wonWordGames = wonWordGames,
            unlockedIds = unlockedIds
        )
        if (unlocks.isEmpty()) return
        val now = System.currentTimeMillis()
        val updated = unlocks.map { unlock ->
            AchievementEntity(
                id = unlock.definition.id,
                type = unlock.definition.type.name,
                title = unlock.definition.title,
                description = unlock.definition.description,
                threshold = unlock.definition.threshold,
                unlocked = true,
                unlockedAt = unlock.unlockedAt
            )
        }
        database.achievementDao().upsertAll(updated)
        unlocks.forEach { unlock ->
            database.achievementEventDao().insert(
                AchievementEventEntity(
                    id = "achievement-event-${UUID.randomUUID()}",
                    achievementId = unlock.definition.id,
                    source = unlock.source.ifBlank { "system" },
                    createdAt = now
                )
            )
        }
    }

    private fun ttsCacheDir(): File =
        File(appContext.cacheDir, "tts")

    private fun deleteTtsCacheFiles() {
        val cacheRoot = ttsCacheDir()
        if (cacheRoot.exists()) {
            cacheRoot.listFiles().orEmpty().forEach { child ->
                child.deleteRecursively()
            }
        }
        cacheRoot.mkdirs()
    }

    private fun pruneTtsCache(maxBytes: Long = MAX_TTS_CACHE_BYTES) {
        var used = 0L
        database.ttsCacheDao().all().forEach { entry ->
            val file = File(entry.audioPath)
            val size = if (file.exists()) file.length() else 0L
            if (size <= 0L || used + size > maxBytes) {
                file.delete()
                database.ttsCacheDao().delete(entry.cacheKey)
            } else {
                used += size
            }
        }
    }

    companion object {
        const val KEY_TTS_ENABLED = "tts_enabled"
        const val KEY_TTS_VOICE = "tts_voice"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_TTS_VOLUME = "tts_volume"
        const val KEY_LEARNING_GOAL = "learning_goal"
        private const val STARTUP_LEXICAL_LIMIT = 30000
        private const val SEED_CHUNK_SIZE = 500
        private const val MAX_TTS_TEXT_LENGTH = 600
        private const val MAX_TTS_CACHE_BYTES = 80L * 1024L * 1024L
    }
}
