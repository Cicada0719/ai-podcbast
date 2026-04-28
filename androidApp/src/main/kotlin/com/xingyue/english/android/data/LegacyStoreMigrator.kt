package com.xingyue.english.android.data

import android.content.Context
import com.xingyue.english.android.data.db.CaptionCueEntity
import com.xingyue.english.android.data.db.ConfigEntity
import com.xingyue.english.android.data.db.ContentEntity
import com.xingyue.english.android.data.db.LearningWordEntity
import com.xingyue.english.android.data.db.WordContextEntity
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.SourceType
import org.json.JSONArray

class LegacyStoreMigrator(
    private val context: Context,
    private val database: XingYueDatabase
) {
    fun migrateIfNeeded() {
        if (database.configDao().get(KEY_MIGRATED)?.value == "true") return

        val prefs = context.getSharedPreferences("xingyue_english_store", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val contents = JSONArray(prefs.getString("contents", "[]"))
        for (index in 0 until contents.length()) {
            val item = contents.getJSONObject(index)
            val id = item.optString("id", "legacy-content-$index")
            database.contentDao().upsert(
                ContentEntity(
                    id = id,
                    title = item.optString("title", "导入内容"),
                    kind = item.optString("kind", SourceType.DOCUMENT.name),
                    extension = item.optString("extension"),
                    uri = item.optString("uri"),
                    localPath = item.optString("uri"),
                    sourceUrl = "",
                    importSource = "LOCAL_FILE",
                    originalText = item.optString("originalText"),
                    durationMs = 0L,
                    coverPath = "",
                    status = item.optString("status", ImportProcessingStatus.IMPORTED.name),
                    statusMessage = item.optString("statusMessage", "已导入"),
                    progress = item.optInt("progress", 0),
                    captionId = item.optString("captionId"),
                    wordCount = item.optInt("wordCount", 0),
                    favorite = item.optBoolean("favorite", false),
                    createdAt = item.optLong("createdAt", now)
                )
            )
            val captionId = item.optString("captionId")
            val captions = item.optJSONArray("captions") ?: JSONArray()
            for (captionIndex in 0 until captions.length()) {
                val cue = captions.getJSONObject(captionIndex)
                if (captionId.isNotBlank()) {
                    database.captionDao().upsertAll(
                        listOf(
                            CaptionCueEntity(
                                captionId = captionId,
                                cueId = cue.optString("id", "legacy-cue-$captionIndex"),
                                sourceItemId = id,
                                source = "IMPORTED_SUBTITLE",
                                startMs = cue.optLong("startMs"),
                                endMs = cue.optLong("endMs"),
                                english = cue.optString("english"),
                                chinese = cue.optString("chinese"),
                                createdAt = now
                            )
                        )
                    )
                }
            }
        }

        val words = JSONArray(prefs.getString("words", "[]"))
        for (index in 0 until words.length()) {
            val word = words.getJSONObject(index)
            val normalized = word.optString("normalized").ifBlank { word.optString("word").lowercase() }
            if (normalized.isBlank()) continue
            database.learningWordDao().upsert(
                LearningWordEntity(
                    normalized = normalized,
                    id = word.optString("id", "legacy-word-$normalized"),
                    word = word.optString("word", normalized),
                    phonetic = word.optString("phonetic"),
                    chineseDefinition = word.optString("chineseDefinition"),
                    status = word.optString("status", LearningWordStatus.NEW_WORD.name),
                    occurrenceCount = word.optInt("occurrenceCount", 1),
                    createdAt = word.optLong("createdAt", now),
                    updatedAt = word.optLong("updatedAt", now),
                    dueAt = word.optLong("updatedAt", now),
                    notes = word.optString("notes")
                )
            )
            database.learningWordDao().insertContext(
                WordContextEntity(
                    wordNormalized = normalized,
                    sourceItemId = word.optString("sourceItemId"),
                    sourceTitle = word.optString("sourceTitle"),
                    captionId = word.optString("captionId"),
                    captionStartMs = word.optLong("captionStartMs"),
                    captionEndMs = word.optLong("captionEndMs"),
                    englishSentence = word.optString("englishSentence"),
                    chineseSentence = word.optString("chineseSentence"),
                    sourceType = word.optString("sourceType", LearningSourceType.DOCUMENT.name),
                    sourceUrl = "",
                    paragraphIndex = -1,
                    createdAt = word.optLong("createdAt", now)
                )
            )
        }

        prefs.getString("bailian_key", "").orEmpty().takeIf { it.isNotBlank() }?.let {
            database.configDao().put(ConfigEntity(KEY_BAILIAN_KEY, it))
        }
        database.configDao().put(ConfigEntity(KEY_REGION, BailianRegion.MAINLAND.name))
        database.configDao().put(ConfigEntity(KEY_MIGRATED, "true"))
    }

    companion object {
        const val KEY_MIGRATED = "legacy_prefs_migrated"
        const val KEY_BAILIAN_KEY = "bailian_key"
        const val KEY_REGION = "bailian_region"
    }
}

enum class BailianRegion(
    val label: String,
    val compatibleChatEndpoint: String,
    val dashScopeBaseUrl: String,
    val asrSupported: Boolean
) {
    MAINLAND(
        label = "中国内地",
        compatibleChatEndpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        dashScopeBaseUrl = "https://dashscope.aliyuncs.com/api/v1",
        asrSupported = true
    ),
    SINGAPORE(
        label = "国际",
        compatibleChatEndpoint = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
        dashScopeBaseUrl = "https://dashscope-intl.aliyuncs.com/api/v1",
        asrSupported = true
    ),
    US(
        label = "美国",
        compatibleChatEndpoint = "https://dashscope-us.aliyuncs.com/compatible-mode/v1/chat/completions",
        dashScopeBaseUrl = "https://dashscope-us.aliyuncs.com/api/v1",
        asrSupported = true
    ),
    HONG_KONG(
        label = "中国香港",
        compatibleChatEndpoint = "https://cn-hongkong.dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        dashScopeBaseUrl = "https://cn-hongkong.dashscope.aliyuncs.com/api/v1",
        asrSupported = false
    );
}
