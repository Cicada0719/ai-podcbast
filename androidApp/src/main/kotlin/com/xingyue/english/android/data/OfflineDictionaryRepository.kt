package com.xingyue.english.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.JsonReader
import android.util.JsonToken
import com.xingyue.english.android.data.db.DictionaryEntryDao
import com.xingyue.english.android.data.db.toDomain
import com.xingyue.english.android.data.db.toEntity
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CloudLanguageService
import com.xingyue.english.core.DictionaryEntry
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.WordDefinition
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

class OfflineDictionaryRepository(
    private val context: Context,
    private val dictionaryDao: DictionaryEntryDao
) {
    fun seedIfNeeded() {
        if (dictionaryDao.count() > 0) return
        val seen = mutableSetOf<String>()
        val seeded = runCatching { readEcdictEntries() }.getOrDefault(seedEntries())
        upsertDistinct(seeded, seen)
        runCatching { streamIntegratedSourceEntries(seen) }
        if (dictionaryDao.count() == 0) upsertDistinct(seedEntries(), seen)
    }

    fun lookup(word: String): DictionaryEntry? {
        val normalized = TextTools.normalizeWord(word)
        if (normalized.isBlank()) return null
        exactOrStem(normalized)?.let { return it }
        lookupPackagedEcdict(normalized)?.let { entry ->
            dictionaryDao.upsertAll(listOf(entry.toEntity()))
            return entry
        }
        return dictionaryDao.search(normalized, limit = 1).firstOrNull()?.toDomain()
    }

    fun search(query: String, limit: Int = 30): List<DictionaryEntry> {
        val normalized = TextTools.normalizeWord(query)
        if (normalized.isBlank()) return emptyList()
        val local = dictionaryDao.search(normalized, limit).map { it.toDomain() }
        if (local.size >= limit) return local
        val packaged = searchPackagedEcdict(normalized, limit - local.size)
        if (packaged.isNotEmpty()) dictionaryDao.upsertAll(packaged.map { it.toEntity() })
        return (local + packaged).distinctBy { it.normalized }.take(limit)
    }

    fun importDictionary(file: File): Int {
        if (!file.exists()) return 0
        val text = file.readText()
        val entries = when {
            text.trimStart().startsWith("[") -> jsonArrayToEntries(JSONArray(text), "用户词典")
            text.trimStart().startsWith("{") -> jsonObjectToEntries(JSONObject(text), "用户词典")
            file.extension.equals("csv", ignoreCase = true) -> csvToEntries(text)
            else -> emptyList()
        }
        if (entries.isNotEmpty()) dictionaryDao.upsertAll(entries.map { it.toEntity() })
        return entries.size
    }

    fun rebuildIndex() {
        seedIfNeeded()
    }

    private fun exactOrStem(normalized: String): DictionaryEntry? {
        dictionaryDao.get(normalized)?.let { return it.toDomain() }
        val candidates = buildList {
            if (normalized.endsWith("ing") && normalized.length > 5) add(normalized.dropLast(3))
            if (normalized.endsWith("ed") && normalized.length > 4) add(normalized.dropLast(2))
            if (normalized.endsWith("ies") && normalized.length > 4) add(normalized.dropLast(3) + "y")
            if (normalized.endsWith("s") && normalized.length > 3) add(normalized.dropLast(1))
        }
        return candidates.firstNotNullOfOrNull { stem -> dictionaryDao.get(stem)?.toDomain() }
    }

    private fun readAssetEntries(): List<DictionaryEntry> {
        val ecdict = readEcdictEntries()
        val integrated = readIntegratedSourceEntries()
        return ecdict + integrated
    }

    private fun readEcdictEntries(): List<DictionaryEntry> {
        val text = context.assets.open("dictionaries/ecdict_seed.json").bufferedReader().use { it.readText() }
        return jsonArrayToEntries(JSONArray(text), "内置 ECDICT")
    }

    private fun lookupPackagedEcdict(normalized: String): DictionaryEntry? =
        queryPackagedEcdict(
            sql = """
                SELECT word, american_phonetic, british_phonetic, translation, definition, exchange
                FROM ecdict
                WHERE lower(word) = ?
                LIMIT 1
            """.trimIndent(),
            args = arrayOf(normalized.lowercase(Locale.ROOT)),
            limit = 1
        ).firstOrNull()

    private fun searchPackagedEcdict(prefix: String, limit: Int): List<DictionaryEntry> {
        if (limit <= 0) return emptyList()
        return queryPackagedEcdict(
            sql = """
                SELECT word, american_phonetic, british_phonetic, translation, definition, exchange
                FROM ecdict
                WHERE lower(word) LIKE ?
                ORDER BY CASE WHEN lower(word) = ? THEN 0 ELSE 1 END, word
                LIMIT ?
            """.trimIndent(),
            args = arrayOf("${prefix.lowercase(Locale.ROOT)}%", prefix.lowercase(Locale.ROOT), limit.toString()),
            limit = limit
        )
    }

    private fun queryPackagedEcdict(sql: String, args: Array<String>, limit: Int): List<DictionaryEntry> =
        runCatching {
            val dbFile = packagedEcdictFile()
            if (!dbFile.exists() || dbFile.length() == 0L) return@runCatching emptyList()
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery(sql, args).use { cursor ->
                    val rows = mutableListOf<DictionaryEntry>()
                    while (cursor.moveToNext() && rows.size < limit) {
                        val word = cursor.getString(0).orEmpty()
                        if (word.isBlank()) continue
                        val american = cursor.getString(1).orEmpty()
                        val british = cursor.getString(2).orEmpty()
                        val translation = cursor.getString(3).orEmpty()
                        val definition = cursor.getString(4).orEmpty()
                        val exchange = cursor.getString(5).orEmpty()
                        rows += DictionaryEntry(
                            word = word,
                            phonetic = american.ifBlank { british },
                            definition = translation.ifBlank { definition }.ifBlank { "待补全释义" },
                            phrases = exchange
                                .split('/', ';', ',')
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                            source = "完整 ECDICT"
                        )
                    }
                    rows
                }
            }
        }.getOrDefault(emptyList())

    private fun packagedEcdictFile(): File {
        val dir = File(context.filesDir, "dictionaries").apply { mkdirs() }
        val target = File(dir, "ecdict.db")
        if (target.exists() && target.length() > 0L) return target
        runCatching {
            context.assets.open("dictionaries/ecdict.db").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure {
            target.delete()
        }
        return target
    }

    private fun upsertDistinct(entries: List<DictionaryEntry>, seen: MutableSet<String>) {
        entries.asSequence()
            .filter { seen.add(TextTools.normalizeWord(it.word)) }
            .map { it.toEntity() }
            .chunked(500)
            .forEach { dictionaryDao.upsertAll(it) }
    }

    private fun streamIntegratedSourceEntries(seen: MutableSet<String>) {
        context.assets.open("vocabulary/integrated_sources_seed.json").use { input ->
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                val chunk = mutableListOf<DictionaryEntry>()
                fun flush() {
                    if (chunk.isNotEmpty()) {
                        upsertDistinct(chunk.toList(), seen)
                        chunk.clear()
                    }
                }
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "items" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                readIntegratedDictionaryEntry(reader)?.let { entry ->
                                    chunk += entry
                                    if (chunk.size >= 500) flush()
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
    }

    private fun readIntegratedDictionaryEntry(reader: JsonReader): DictionaryEntry? {
        var word = ""
        var phonetic = ""
        var definition = ""
        var phrases = emptyList<String>()
        var example = ""
        var source = "整合词库"
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "word" -> word = reader.nextStringValue()
                "phonetic" -> phonetic = reader.nextStringValue()
                "definition" -> definition = reader.nextStringValue()
                "phrases" -> phrases = reader.nextStringArray()
                "example" -> example = reader.nextStringValue()
                "licenseSource" -> source = reader.nextStringValue().ifBlank { "整合词库" }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (word.isBlank()) return null
        return DictionaryEntry(
            word = word,
            phonetic = phonetic,
            definition = definition.ifBlank { "待补全释义" },
            phrases = phrases,
            examples = example.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
            source = source
        )
    }

    private fun readIntegratedSourceEntries(): List<DictionaryEntry> =
        runCatching {
            val text = context.assets.open("vocabulary/integrated_sources_seed.json")
                .bufferedReader()
                .use { it.readText() }
            val items = JSONObject(text).optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                val word = item.optString("word")
                if (word.isBlank()) return@mapNotNull null
                DictionaryEntry(
                    word = word,
                    phonetic = item.optString("phonetic"),
                    definition = item.optString("definition").ifBlank { "待补全释义" },
                    phrases = item.optJSONArray("phrases")?.toStringList().orEmpty(),
                    examples = item.optString("example").takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
                    source = item.optString("licenseSource").ifBlank { "整合词库" }
                )
            }
        }.getOrDefault(emptyList())

    private fun jsonArrayToEntries(array: JSONArray, source: String): List<DictionaryEntry> =
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val word = item.optString("word").ifBlank { return@mapNotNull null }
            DictionaryEntry(
                word = word,
                phonetic = item.optString("phonetic"),
                definition = item.optString("definition").ifBlank { item.optString("translation") },
                phrases = item.optJSONArray("phrases")?.toStringList().orEmpty(),
                examples = item.optJSONArray("examples")?.toStringList().orEmpty(),
                source = item.optString("source", source)
            )
        }

    private fun jsonObjectToEntries(root: JSONObject, source: String): List<DictionaryEntry> {
        val wordList = root.optJSONArray("wordList")
        if (wordList != null) {
            return (0 until wordList.length()).mapNotNull { index ->
                val item = wordList.optJSONObject(index) ?: return@mapNotNull null
                val word = item.optString("value").ifBlank { item.optString("word") }
                if (word.isBlank()) return@mapNotNull null
                DictionaryEntry(
                    word = word,
                    phonetic = item.optString("usphone").ifBlank { item.optString("ukphone") },
                    definition = item.optString("translation").ifBlank { item.optString("definition") },
                    phrases = item.optString("exchange")
                        .split('/')
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                    examples = item.optJSONArray("captions")?.let { captions ->
                        (0 until captions.length()).mapNotNull { captions.optJSONObject(it)?.optString("content")?.takeIf(String::isNotBlank) }
                    }.orEmpty(),
                    source = root.optString("name").ifBlank { source }
                )
            }
        }
        return jsonArrayToEntries(JSONArray().put(root), source)
    }

    private fun csvToEntries(text: String): List<DictionaryEntry> =
        text.lineSequence()
            .dropWhile { it.startsWith("word,", ignoreCase = true) }
            .mapNotNull { line ->
                val cells = line.split(',').map { it.trim() }
                val word = cells.getOrNull(0).orEmpty()
                if (word.isBlank()) null else DictionaryEntry(
                    word = word,
                    phonetic = cells.getOrNull(1).orEmpty(),
                    definition = cells.drop(2).joinToString("，").ifBlank { "待补全释义" },
                    source = "用户词典"
                )
            }
            .toList()

    private fun seedEntries(): List<DictionaryEntry> =
        listOf(
            DictionaryEntry("wondering", "wonder", "/ˈwʌndərɪŋ/", "想知道；感到疑惑", listOf("be wondering about sth. 想知道某事"), listOf("I was wondering if you could help me with this.")),
            DictionaryEntry("context", "context", "/ˈkɑːntekst/", "语境；背景", examples = listOf("The word is easier to remember in context.")),
            DictionaryEntry("practice", "practice", "/ˈpræktɪs/", "练习；实践", phrases = listOf("listening practice 听力练习")),
            DictionaryEntry("memory", "memory", "/ˈmeməri/", "记忆；记忆力"),
            DictionaryEntry("subtitle", "subtitle", "/ˈsʌbtaɪtl/", "字幕"),
            DictionaryEntry("listening", "listening", "/ˈlɪsənɪŋ/", "听力；倾听"),
            DictionaryEntry("repeat", "repeat", "/rɪˈpiːt/", "重复；复述"),
            DictionaryEntry("durable", "durable", "/ˈdʊrəbəl/", "持久的；耐用的"),
            DictionaryEntry("curiosity", "curiosity", "/ˌkjʊriˈɑːsəti/", "好奇心"),
            DictionaryEntry("reflection", "reflection", "/rɪˈflekʃən/", "反思；映像"),
            DictionaryEntry("actually", "actually", "/ˈæktʃuəli/", "实际上；事实上"),
            DictionaryEntry("interrupt", "interrupt", "/ˌɪntəˈrʌpt/", "打断；中断")
        )
}

class OfflineDictionaryLanguageService(
    private val dictionaryRepository: OfflineDictionaryRepository
) : CloudLanguageService {
    override val hasCloudKey: Boolean = false
    override fun transcribeAudio(audioPath: String): List<CaptionCue> = emptyList()
    override fun translateToChinese(english: String): String = ""
    override fun lookup(word: String): WordDefinition {
        val entry = dictionaryRepository.lookup(word)
        return WordDefinition(
            word = entry?.word ?: word,
            phonetic = entry?.phonetic.orEmpty(),
            chinese = entry?.definition ?: BailianLanguageService.fallbackDefinition(word),
            phrases = entry?.phrases.orEmpty(),
            examples = entry?.examples.orEmpty()
        )
    }
}

class DictionaryFirstLanguageService(
    private val offlineDictionary: OfflineDictionaryRepository,
    private val cloud: CloudLanguageService
) : CloudLanguageService {
    override val hasCloudKey: Boolean = cloud.hasCloudKey
    override fun transcribeAudio(audioPath: String): List<CaptionCue> = cloud.transcribeAudio(audioPath)
    override fun translateToChinese(english: String): String = cloud.translateToChinese(english)
    override fun lookup(word: String): WordDefinition {
        val local = offlineDictionary.lookup(word)
        val cloudDefinition = runCatching { cloud.lookup(word) }.getOrNull()
        return WordDefinition(
            word = cloudDefinition?.word?.ifBlank { null } ?: local?.word ?: word,
            phonetic = cloudDefinition?.phonetic?.ifBlank { null } ?: local?.phonetic.orEmpty(),
            chinese = cloudDefinition?.chinese?.ifBlank { null } ?: local?.definition ?: BailianLanguageService.fallbackDefinition(word),
            phrases = cloudDefinition?.phrases?.ifEmpty { null } ?: local?.phrases.orEmpty(),
            examples = cloudDefinition?.examples?.ifEmpty { null } ?: local?.examples.orEmpty(),
            roots = cloudDefinition?.roots.orEmpty()
        )
    }
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }

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
