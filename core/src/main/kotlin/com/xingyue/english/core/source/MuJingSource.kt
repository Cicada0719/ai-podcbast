package com.xingyue.english.core.source

import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.SourceContext
import com.xingyue.english.core.TextTools
import org.json.JSONArray
import org.json.JSONObject

enum class MuJingVocabularyType {
    DOCUMENT,
    SUBTITLES,
    MKV
}

data class MuJingVocabulary(
    val name: String = "",
    val type: MuJingVocabularyType = MuJingVocabularyType.DOCUMENT,
    val language: String = "",
    val size: Int = 0,
    val relateVideoPath: String = "",
    val subtitlesTrackId: Int = 0,
    val wordList: List<MuJingWord> = emptyList()
) {
    val normalizedSize: Int
        get() = if (size == wordList.size) size else wordList.size
}

data class MuJingWord(
    val value: String,
    val usphone: String = "",
    val ukphone: String = "",
    val definition: String = "",
    val translation: String = "",
    val pos: String = "",
    val collins: Int = 0,
    val oxford: Boolean = false,
    val tag: String = "",
    val bnc: Int? = 0,
    val frq: Int? = 0,
    val exchange: String = "",
    val externalCaptions: List<MuJingExternalCaption> = emptyList(),
    val captions: List<MuJingCaption> = emptyList()
) {
    val normalized: String
        get() = TextTools.normalizeWord(value)

    fun sameWord(other: MuJingWord): Boolean =
        normalized == other.normalized
}

data class MuJingCaption(
    val start: String,
    val end: String,
    val content: String
) {
    override fun toString(): String = content
}

data class MuJingExternalCaption(
    val relateVideoPath: String,
    val subtitlesTrackId: Int,
    val subtitlesName: String,
    val start: String,
    val end: String,
    val content: String
) {
    override fun toString(): String = content
}

object MuJingVocabularyParser {
    fun parse(text: String, fallbackName: String = ""): MuJingVocabulary {
        val json = JSONObject(text)
        val words = json.optJSONArray("wordList").orEmptyObjects().map(::parseWord)
        val name = json.optString("name").ifBlank { fallbackName }
        val type = runCatching {
            MuJingVocabularyType.valueOf(json.optString("type").ifBlank { MuJingVocabularyType.DOCUMENT.name })
        }.getOrDefault(MuJingVocabularyType.DOCUMENT)
        return MuJingVocabulary(
            name = name,
            type = type,
            language = json.optString("language"),
            size = json.optInt("size", words.size),
            relateVideoPath = json.optString("relateVideoPath"),
            subtitlesTrackId = json.optInt("subtitlesTrackId", 0),
            wordList = words
        )
    }

    fun toJson(vocabulary: MuJingVocabulary): JSONObject =
        JSONObject()
            .put("name", vocabulary.name)
            .put("type", vocabulary.type.name)
            .put("language", vocabulary.language)
            .put("size", vocabulary.normalizedSize)
            .put("relateVideoPath", vocabulary.relateVideoPath)
            .put("subtitlesTrackId", vocabulary.subtitlesTrackId)
            .put("wordList", JSONArray(vocabulary.wordList.map(::wordToJson)))

    private fun parseWord(json: JSONObject): MuJingWord =
        MuJingWord(
            value = json.optString("value"),
            usphone = json.optString("usphone"),
            ukphone = json.optString("ukphone"),
            definition = json.optString("definition"),
            translation = json.optString("translation"),
            pos = json.optString("pos"),
            collins = json.optInt("collins", 0),
            oxford = json.optBoolean("oxford", false),
            tag = json.optString("tag"),
            bnc = json.optNullableInt("bnc"),
            frq = json.optNullableInt("frq"),
            exchange = json.optString("exchange"),
            externalCaptions = json.optJSONArray("externalCaptions").orEmptyObjects().map(::parseExternalCaption),
            captions = json.optJSONArray("captions").orEmptyObjects().map(::parseCaption)
        )

    private fun parseCaption(json: JSONObject): MuJingCaption =
        MuJingCaption(
            start = json.optString("start"),
            end = json.optString("end"),
            content = json.optString("content")
        )

    private fun parseExternalCaption(json: JSONObject): MuJingExternalCaption =
        MuJingExternalCaption(
            relateVideoPath = json.optString("relateVideoPath"),
            subtitlesTrackId = json.optInt("subtitlesTrackId", 0),
            subtitlesName = json.optString("subtitlesName"),
            start = json.optString("start"),
            end = json.optString("end"),
            content = json.optString("content")
        )

    private fun wordToJson(word: MuJingWord): JSONObject =
        JSONObject()
            .put("value", word.value)
            .put("usphone", word.usphone)
            .put("ukphone", word.ukphone)
            .put("definition", word.definition)
            .put("translation", word.translation)
            .put("pos", word.pos)
            .put("collins", word.collins)
            .put("oxford", word.oxford)
            .put("tag", word.tag)
            .put("bnc", word.bnc)
            .put("frq", word.frq)
            .put("exchange", word.exchange)
            .put("externalCaptions", JSONArray(word.externalCaptions.map(::externalCaptionToJson)))
            .put("captions", JSONArray(word.captions.map(::captionToJson)))

    private fun captionToJson(caption: MuJingCaption): JSONObject =
        JSONObject()
            .put("start", caption.start)
            .put("end", caption.end)
            .put("content", caption.content)

    private fun externalCaptionToJson(caption: MuJingExternalCaption): JSONObject =
        JSONObject()
            .put("relateVideoPath", caption.relateVideoPath)
            .put("subtitlesTrackId", caption.subtitlesTrackId)
            .put("subtitlesName", caption.subtitlesName)
            .put("start", caption.start)
            .put("end", caption.end)
            .put("content", caption.content)

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) runCatching { getInt(name) }.getOrNull() else null
}

object MuJingVocabularyAdapter {
    fun wordToLearningWord(
        word: MuJingWord,
        vocabulary: MuJingVocabulary,
        now: Long = System.currentTimeMillis()
    ): LearningWord =
        LearningWord(
            id = "mujing-${word.normalized}",
            word = word.value,
            normalized = word.normalized,
            phonetic = word.usphone.ifBlank { word.ukphone },
            chineseDefinition = word.translation.ifBlank { word.definition },
            status = LearningWordStatus.NEW_WORD,
            contexts = wordContexts(word, vocabulary, now),
            occurrenceCount = word.captions.size + word.externalCaptions.size,
            createdAt = now,
            updatedAt = now,
            dueAt = now,
            notes = listOf(word.pos, word.exchange, word.tag).filter { it.isNotBlank() }.joinToString(" | ")
        )

    fun vocabularyToLearningWords(
        vocabulary: MuJingVocabulary,
        now: Long = System.currentTimeMillis()
    ): List<LearningWord> =
        vocabulary.wordList
            .filter { it.value.isNotBlank() }
            .distinctBy { it.normalized }
            .map { wordToLearningWord(it, vocabulary, now) }

    fun vocabularyToCaption(
        vocabulary: MuJingVocabulary,
        sourceItemId: String = vocabulary.relateVideoPath.ifBlank { vocabulary.name }
    ): BilingualCaption {
        val cues = vocabulary.wordList
            .flatMap { word ->
                word.externalCaptions.map { caption ->
                    CaptionCue(
                        id = "${word.normalized}-${caption.start}-${caption.end}",
                        startMs = parseTimeMs(caption.start),
                        endMs = parseTimeMs(caption.end),
                        english = caption.content,
                        chinese = ""
                    )
                } + word.captions.map { caption ->
                    CaptionCue(
                        id = "${word.normalized}-${caption.start}-${caption.end}",
                        startMs = parseTimeMs(caption.start),
                        endMs = parseTimeMs(caption.end),
                        english = caption.content,
                        chinese = ""
                    )
                }
            }
            .distinctBy { "${it.startMs}-${it.endMs}-${it.english}" }
            .sortedBy { it.startMs }
        return BilingualCaption(
            id = "mujing-${sourceItemId.hashCode()}",
            sourceItemId = sourceItemId,
            cues = cues,
            source = when (vocabulary.type) {
                MuJingVocabularyType.SUBTITLES, MuJingVocabularyType.MKV -> CaptionSource.IMPORTED_SUBTITLE
                MuJingVocabularyType.DOCUMENT -> CaptionSource.DOCUMENT
            }
        )
    }

    private fun wordContexts(
        word: MuJingWord,
        vocabulary: MuJingVocabulary,
        now: Long
    ): List<SourceContext> {
        val external = word.externalCaptions.mapIndexed { index, caption ->
            SourceContext(
                sourceItemId = caption.relateVideoPath.ifBlank { vocabulary.relateVideoPath.ifBlank { vocabulary.name } },
                sourceTitle = caption.subtitlesName.ifBlank { vocabulary.name },
                captionId = "track-${caption.subtitlesTrackId}",
                captionStartMs = parseTimeMs(caption.start),
                captionEndMs = parseTimeMs(caption.end),
                englishSentence = caption.content,
                chineseSentence = "",
                sourceType = if (vocabulary.type == MuJingVocabularyType.DOCUMENT) LearningSourceType.DOCUMENT else LearningSourceType.SUBTITLE,
                paragraphIndex = index,
                createdAt = now
            )
        }
        val internal = word.captions.mapIndexed { index, caption ->
            SourceContext(
                sourceItemId = vocabulary.relateVideoPath.ifBlank { vocabulary.name },
                sourceTitle = vocabulary.name,
                captionId = "track-${vocabulary.subtitlesTrackId}",
                captionStartMs = parseTimeMs(caption.start),
                captionEndMs = parseTimeMs(caption.end),
                englishSentence = caption.content,
                chineseSentence = "",
                sourceType = if (vocabulary.type == MuJingVocabularyType.DOCUMENT) LearningSourceType.DOCUMENT else LearningSourceType.SUBTITLE,
                paragraphIndex = index,
                createdAt = now
            )
        }
        return external + internal
    }

    fun parseTimeMs(value: String): Long {
        val clean = value.trim()
        if (clean.isBlank()) return 0L
        val time = clean.replace(',', '.')
        val parts = time.split(':')
        return runCatching {
            when (parts.size) {
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toDouble()
                    ((hours * 3600 + minutes * 60) * 1000 + seconds * 1000).toLong()
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toDouble()
                    ((minutes * 60) * 1000 + seconds * 1000).toLong()
                }
                else -> (time.toDouble() * 1000).toLong()
            }
        }.getOrDefault(0L).coerceAtLeast(0L)
    }
}
