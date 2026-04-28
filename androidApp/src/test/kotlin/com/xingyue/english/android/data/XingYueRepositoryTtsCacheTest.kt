package com.xingyue.english.android.data

import android.content.Context
import android.content.ContextWrapper
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.xingyue.english.android.data.db.CaptionDao
import com.xingyue.english.android.data.db.AchievementDao
import com.xingyue.english.android.data.db.AchievementEventDao
import com.xingyue.english.android.data.db.ConfigDao
import com.xingyue.english.android.data.db.ConfigEntity
import com.xingyue.english.android.data.db.ContentDao
import com.xingyue.english.android.data.db.DictionaryEntryDao
import com.xingyue.english.android.data.db.FsrsCardStateDao
import com.xingyue.english.android.data.db.LearningWordDao
import com.xingyue.english.android.data.db.LexicalItemDao
import com.xingyue.english.android.data.db.PhraseChunkDao
import com.xingyue.english.android.data.db.PlatformMetadataDao
import com.xingyue.english.android.data.db.PracticeSessionDao
import com.xingyue.english.android.data.db.QueryHistoryDao
import com.xingyue.english.android.data.db.ReadingProgressDao
import com.xingyue.english.android.data.db.ReviewDao
import com.xingyue.english.android.data.db.SourceModelDao
import com.xingyue.english.android.data.db.StudyPathAttemptDao
import com.xingyue.english.android.data.db.StudyTaskAttemptDao
import com.xingyue.english.android.data.db.TypingAttemptDao
import com.xingyue.english.android.data.db.TtsCacheDao
import com.xingyue.english.android.data.db.TtsCacheEntity
import com.xingyue.english.android.data.db.VocabularyDeckDao
import com.xingyue.english.android.data.db.WordGameSessionDao
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.core.SpeechSynthesisResult
import com.xingyue.english.core.SpeechSynthesisService
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XingYueRepositoryTtsCacheTest {
    @Test
    fun saveTtsConfigPersistsExplicitOnOffSwitchState() {
        runBlocking {
            val root = Files.createTempDirectory("repo-tts-switch").toFile()
            val context = FakeContext(root)
            val repository = repositoryWithFakeTts(context, FakeXingYueDatabase(), AtomicInteger(0))

            repository.saveTtsConfig(enabled = false, voiceId = "longanyang", speed = 0.8f, volume = 1f)
            val disabled = repository.observeConfig().first()
            repository.saveTtsConfig(enabled = true, voiceId = "longxiaoxia_v3", speed = 1.2f, volume = 1f)
            val enabled = repository.observeConfig().first()

            assertFalse(disabled.ttsEnabled)
            assertEquals(0.8f, disabled.ttsSpeed)
            assertTrue(enabled.ttsEnabled)
            assertEquals("longxiaoxia_v3", enabled.ttsVoiceId)
            assertEquals(1.2f, enabled.ttsSpeed)
        }
    }

    @Test
    fun synthesizeSpeechRecordsAndHitsCacheIndex() {
        runBlocking {
            val root = Files.createTempDirectory("repo-tts-cache").toFile()
            val context = FakeContext(root)
            val database = FakeXingYueDatabase()
            val calls = AtomicInteger(0)
            val repository = repositoryWithFakeTts(context, database, calls)

            repository.saveBailianKey("test-key")
            repository.saveTtsConfig(enabled = true, voiceId = "longanyang", speed = 1f, volume = 1f)

            val first = repository.synthesizeSpeech(" hello ")
            val second = repository.synthesizeSpeech("hello")

            assertNotNull(first)
            assertNotNull(second)
            assertEquals(1, calls.get())
            assertFalse(first.cached)
            assertTrue(second.cached)
            assertEquals(first.audioPath, second.audioPath)
            assertTrue(File(second.audioPath).exists())

            val cacheRows = database.ttsCacheDao().all()
            assertEquals(1, cacheRows.size)
            assertEquals(1, cacheRows.single().hitCount)
        }
    }

    @Test
    fun clearCacheOnlyDeletesTtsFilesAndIndex() {
        runBlocking {
            val root = Files.createTempDirectory("repo-tts-clear").toFile()
            val context = FakeContext(root)
            val database = FakeXingYueDatabase()
            val repository = repositoryWithFakeTts(context, database, AtomicInteger(0))

            repository.saveBailianKey("test-key")
            repository.saveTtsConfig(enabled = true, voiceId = "longanyang", speed = 1f, volume = 1f)
            val result = repository.synthesizeSpeech("hello")
            assertNotNull(result)
            assertTrue(File(result.audioPath).exists())
            assertEquals(1, database.ttsCacheDao().all().size)

            repository.clearCacheOnly()

            assertFalse(File(result.audioPath).exists())
            assertTrue(File(context.cacheDir, "tts").exists())
            assertTrue(database.ttsCacheDao().all().isEmpty())
            assertNotNull(database.configDao().get("cache_cleared_at"))
        }
    }

    private fun repositoryWithFakeTts(
        context: Context,
        database: XingYueDatabase,
        calls: AtomicInteger
    ): XingYueRepository =
        XingYueRepository(
            context = context,
            database = database,
            ttsServiceFactory = { _, cacheDir ->
                object : SpeechSynthesisService {
                    override val available: Boolean = true

                    override fun synthesize(
                        text: String,
                        voiceId: String,
                        speed: Float,
                        volume: Float
                    ): SpeechSynthesisResult {
                        calls.incrementAndGet()
                        cacheDir.mkdirs()
                        val audio = BailianTtsService.cacheFile(cacheDir, text, voiceId, speed, volume)
                        audio.writeText("audio:$text")
                        return SpeechSynthesisResult(
                            text = text,
                            voiceId = voiceId,
                            audioPath = audio.absolutePath,
                            cached = false
                        )
                    }
                }
            }
        )

    private class FakeContext(root: File) : ContextWrapper(null) {
        private val fakeCacheDir = File(root, "cache").also { it.mkdirs() }

        override fun getApplicationContext(): Context = this

        override fun getCacheDir(): File = fakeCacheDir
    }

    private class FakeXingYueDatabase : XingYueDatabase() {
        private val configDao = InMemoryConfigDao()
        private val ttsCacheDao = InMemoryTtsCacheDao()

        override fun configDao(): ConfigDao = configDao

        override fun ttsCacheDao(): TtsCacheDao = ttsCacheDao

        override fun platformMetadataDao(): PlatformMetadataDao = unsupportedDao(PlatformMetadataDao::class.java)

        override fun contentDao(): ContentDao = unsupportedDao(ContentDao::class.java)

        override fun captionDao(): CaptionDao = unsupportedDao(CaptionDao::class.java)

        override fun learningWordDao(): LearningWordDao = unsupportedDao(LearningWordDao::class.java)

        override fun reviewDao(): ReviewDao = unsupportedDao(ReviewDao::class.java)

        override fun fsrsCardStateDao(): FsrsCardStateDao = unsupportedDao(FsrsCardStateDao::class.java)

        override fun studyTaskAttemptDao(): StudyTaskAttemptDao = unsupportedDao(StudyTaskAttemptDao::class.java)

        override fun dictionaryEntryDao(): DictionaryEntryDao = unsupportedDao(DictionaryEntryDao::class.java)

        override fun readingProgressDao(): ReadingProgressDao = unsupportedDao(ReadingProgressDao::class.java)

        override fun vocabularyDeckDao(): VocabularyDeckDao = unsupportedDao(VocabularyDeckDao::class.java)

        override fun lexicalItemDao(): LexicalItemDao = unsupportedDao(LexicalItemDao::class.java)

        override fun phraseChunkDao(): PhraseChunkDao = unsupportedDao(PhraseChunkDao::class.java)

        override fun studyPathAttemptDao(): StudyPathAttemptDao = unsupportedDao(StudyPathAttemptDao::class.java)

        override fun practiceSessionDao(): PracticeSessionDao = unsupportedDao(PracticeSessionDao::class.java)

        override fun typingAttemptDao(): TypingAttemptDao = unsupportedDao(TypingAttemptDao::class.java)

        override fun achievementDao(): AchievementDao = unsupportedDao(AchievementDao::class.java)

        override fun achievementEventDao(): AchievementEventDao = unsupportedDao(AchievementEventDao::class.java)

        override fun wordGameSessionDao(): WordGameSessionDao = unsupportedDao(WordGameSessionDao::class.java)

        override fun queryHistoryDao(): QueryHistoryDao = unsupportedDao(QueryHistoryDao::class.java)

        override fun sourceModelDao(): SourceModelDao = unsupportedDao(SourceModelDao::class.java)

        override fun clearAllTables() = Unit

        protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper =
            error("unused")

        protected override fun createInvalidationTracker(): InvalidationTracker =
            InvalidationTracker(this, "app_config", "tts_audio_cache")
    }

    private class InMemoryConfigDao : ConfigDao {
        private val configs = linkedMapOf<String, ConfigEntity>()

        override fun get(key: String): ConfigEntity? =
            configs[key]

        override fun all(): List<ConfigEntity> =
            configs.values.toList()

        override fun observeAll(): Flow<List<ConfigEntity>> =
            flowOf(all())

        override fun put(config: ConfigEntity) {
            configs[config.key] = config
        }

        override fun delete(key: String) {
            configs.remove(key)
        }
    }

    private class InMemoryTtsCacheDao : TtsCacheDao {
        private val entries = linkedMapOf<String, TtsCacheEntity>()

        override fun get(cacheKey: String): TtsCacheEntity? =
            entries[cacheKey]

        override fun all(): List<TtsCacheEntity> =
            entries.values.sortedByDescending { it.lastUsedAt }

        override fun upsert(cache: TtsCacheEntity) {
            entries[cache.cacheKey] = cache
        }

        override fun markUsed(cacheKey: String, lastUsedAt: Long) {
            val previous = entries[cacheKey] ?: return
            entries[cacheKey] = previous.copy(
                lastUsedAt = lastUsedAt,
                hitCount = previous.hitCount + 1
            )
        }

        override fun delete(cacheKey: String) {
            entries.remove(cacheKey)
        }

        override fun clear() {
            entries.clear()
        }
    }

    private companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> unsupportedDao(type: Class<T>): T =
            Proxy.newProxyInstance(
                type.classLoader,
                arrayOf(type)
            ) { _, method, _ -> error("Unused DAO method: ${method.name}") } as T
    }
}
