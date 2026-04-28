package com.xingyue.english.core.source

import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.TextTools
import org.json.JSONArray
import org.json.JSONObject

enum class TypeWordsDictType {
    COLLECT,
    SIMPLE,
    WRONG,
    KNOWN,
    WORD,
    ARTICLE
}

enum class TypeWordsSort {
    NORMAL,
    RANDOM,
    REVERSE,
    REVERSE_ALL,
    RANDOM_ALL
}

enum class TypeWordsPracticeArticleWordType {
    SYMBOL,
    NUMBER,
    WORD
}

enum class TypeWordsPracticeMode {
    SYSTEM,
    FREE,
    IDENTIFY_ONLY,
    DICTATION_ONLY,
    LISTEN_ONLY,
    SHUFFLE,
    REVIEW,
    SHUFFLE_WORDS_TEST,
    REVIEW_WORDS_TEST
}

enum class TypeWordsPracticeType {
    FOLLOW_WRITE,
    SPELL,
    IDENTIFY,
    LISTEN,
    DICTATION
}

enum class TypeWordsPracticeStage {
    FOLLOW_WRITE_NEW_WORD,
    IDENTIFY_NEW_WORD,
    LISTEN_NEW_WORD,
    DICTATION_NEW_WORD,
    FOLLOW_WRITE_REVIEW,
    IDENTIFY_REVIEW,
    LISTEN_REVIEW,
    DICTATION_REVIEW,
    SHUFFLE,
    COMPLETE
}

enum class TypeWordsIdentifyMethod {
    SELF_ASSESSMENT,
    WORD_TEST
}

data class TypeWordsTranslation(
    val pos: String = "",
    val cn: String = ""
)

data class TypeWordsSentenceExample(
    val c: String = "",
    val cn: String = ""
)

data class TypeWordsPhrase(
    val c: String = "",
    val cn: String = ""
)

data class TypeWordsSynonym(
    val pos: String = "",
    val cn: String = "",
    val ws: List<String> = emptyList()
)

data class TypeWordsRelatedWordGroup(
    val pos: String = "",
    val words: List<TypeWordsPhrase> = emptyList()
)

data class TypeWordsRelatedWords(
    val root: String = "",
    val rels: List<TypeWordsRelatedWordGroup> = emptyList()
)

data class TypeWordsEtymology(
    val t: String = "",
    val d: String = ""
)

data class TypeWordsWord(
    val id: String? = null,
    val custom: Boolean = false,
    val word: String = "",
    val phonetic0: String = "",
    val phonetic1: String = "",
    val trans: List<TypeWordsTranslation> = emptyList(),
    val sentences: List<TypeWordsSentenceExample> = emptyList(),
    val phrases: List<TypeWordsPhrase> = emptyList(),
    val synos: List<TypeWordsSynonym> = emptyList(),
    val relWords: TypeWordsRelatedWords = TypeWordsRelatedWords(),
    val etymology: List<TypeWordsEtymology> = emptyList()
) {
    val normalized: String
        get() = TextTools.normalizeWord(word)

    fun definitionText(): String =
        trans.joinToString("; ") { item ->
            listOf(item.pos, item.cn).filter { it.isNotBlank() }.joinToString(" ")
        }.ifBlank {
            phrases.firstOrNull()?.cn.orEmpty()
        }
}

data class TypeWordsArticleWord(
    val base: TypeWordsWord = TypeWordsWord(),
    val nextSpace: Boolean = true,
    val symbolPosition: String = "",
    val input: String = "",
    val type: TypeWordsPracticeArticleWordType = TypeWordsPracticeArticleWordType.WORD
)

data class TypeWordsSentence(
    val text: String = "",
    val translate: String = "",
    val words: List<TypeWordsArticleWord> = emptyList(),
    val audioPosition: List<Int> = emptyList()
)

data class TypeWordsArticleQuestion(
    val stem: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: List<String> = emptyList(),
    val explanation: String = ""
)

data class TypeWordsArticleQuote(
    val start: Int = 0,
    val text: String = "",
    val translate: String = "",
    val end: Int = 0
)

data class TypeWordsArticle(
    val id: String? = null,
    val title: String = "",
    val titleTranslate: String = "",
    val text: String = "",
    val textTranslate: String = "",
    val newWords: List<TypeWordsWord> = emptyList(),
    val sections: List<List<TypeWordsSentence>> = emptyList(),
    val audioSrc: String = "",
    val audioFileId: String = "",
    val lrcPosition: List<List<Int>> = emptyList(),
    val nameList: List<String> = emptyList(),
    val questions: List<TypeWordsArticleQuestion> = emptyList(),
    val quote: TypeWordsArticleQuote? = null,
    val question: TypeWordsArticleQuote? = null
)

data class TypeWordsStatistics(
    val startDate: Long = 0L,
    val spend: Long = 0L,
    val total: Int = 0,
    val new: Int = 0,
    val review: Int = 0,
    val wrong: Int = 0,
    val title: String = ""
)

data class TypeWordsDictResource(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val url: String = "",
    val length: Int = 0,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val translateLanguage: String = "",
    val type: TypeWordsDictType = TypeWordsDictType.WORD,
    val version: Int = 0,
    val language: String = "en"
)

data class TypeWordsDict(
    val resource: TypeWordsDictResource = TypeWordsDictResource(),
    val lastLearnIndex: Int = 0,
    val perDayStudyNumber: Int = 20,
    val words: List<TypeWordsWord> = emptyList(),
    val articles: List<TypeWordsArticle> = emptyList(),
    val statistics: List<TypeWordsStatistics> = emptyList(),
    val custom: Boolean = false,
    val complete: Boolean = false,
    val enName: String = "",
    val createdBy: String = "",
    val categoryId: Int? = null,
    val isDefault: Boolean = false,
    val update: Boolean = false,
    val cover: String = "",
    val sync: Boolean = false,
    val userDictId: Int? = null
) {
    val id: String
        get() = resource.id

    val name: String
        get() = resource.name

    val length: Int
        get() = if (resource.length > 0) resource.length else words.size.coerceAtLeast(articles.size)
}

data class TypeWordsFsrsCard(
    val dueAt: Long,
    val state: String = "",
    val stability: Double = 0.0,
    val difficulty: Double = 0.0
)

data class TypeWordsTaskWords(
    val new: List<TypeWordsWord> = emptyList(),
    val review: List<TypeWordsWord> = emptyList()
)

data class TypeWordsCandidate(
    val word: String,
    val wordObj: TypeWordsWord? = null,
    val label: String = ""
)

data class TypeWordsQuestion(
    val stem: TypeWordsWord = TypeWordsWord(),
    val candidates: List<TypeWordsCandidate> = emptyList(),
    val correctIndex: Int = -1,
    val selectedIndex: Int = -1,
    val submitted: Boolean = false
)

data class TypeWordsPracticeData(
    val index: Int = 0,
    val words: List<TypeWordsWord> = emptyList(),
    val wrongWords: List<TypeWordsWord> = emptyList(),
    val excludeWords: List<String> = emptyList(),
    val allWrongWords: List<String> = emptyList(),
    val isTypingWrongWord: Boolean = false,
    val wrongTimesMap: Map<String, Int> = emptyMap(),
    val wrongTimes: Int = 0,
    val ratingMap: Map<String, String> = emptyMap(),
    val question: TypeWordsQuestion = TypeWordsQuestion()
)

data class TypeWordsFontSizeSetting(
    val articleForeignFontSize: Int = 48,
    val articleTranslateFontSize: Int = 20,
    val wordForeignFontSize: Int = 48,
    val wordTranslateFontSize: Int = 20
)

data class TypeWordsFsrsParameters(
    val requestRetention: Double = 0.9,
    val maximumInterval: Int = 36500,
    val w: List<Double> = TypeWordsDefaults.fsrsWeights,
    val enableFuzz: Boolean = false,
    val enableShortTerm: Boolean = true,
    val learningSteps: List<String> = listOf("1m", "10m"),
    val relearningSteps: List<String> = listOf("10m")
)

data class TypeWordsTtsVoiceMap(
    val key: String,
    val voice: String
)

data class TypeWordsSettingState(
    val soundType: String = "us",
    val wordSound: Boolean = true,
    val wordSoundVolume: Int = 100,
    val wordSoundSpeed: Double = 1.0,
    val wordReviewRatio: Int = 3,
    val articleSound: Boolean = true,
    val articleAutoPlayNext: Boolean = false,
    val articleSoundVolume: Int = 100,
    val articleSoundSpeed: Double = 1.0,
    val keyboardSound: Boolean = true,
    val keyboardSoundVolume: Int = 100,
    val keyboardSoundFile: String = "笔记本键盘",
    val effectSound: Boolean = true,
    val effectSoundVolume: Int = 100,
    val repeatCount: Int = 1,
    val repeatCustomCount: Int? = null,
    val dictation: Boolean = false,
    val translate: Boolean = true,
    val showNearWord: Boolean = true,
    val ignoreCase: Boolean = true,
    val allowWordTip: Boolean = true,
    val waitTimeForChangeWord: Int = 300,
    val fontSize: TypeWordsFontSizeSetting = TypeWordsFontSizeSetting(),
    val showToolbar: Boolean = true,
    val showPanel: Boolean = true,
    val sideExpand: Boolean = true,
    val theme: String = "auto",
    val shortcutKeyMap: Map<String, String> = TypeWordsDefaults.shortcutKeyMap,
    val first: Boolean = true,
    val firstTime: Long = System.currentTimeMillis(),
    val webAppVersion: Int = 3,
    val load: Boolean = false,
    val conflictNotice: Boolean = true,
    val showConflictNotice2: Boolean = true,
    val showUsageTips: Boolean = true,
    val ignoreSimpleWord: Boolean = false,
    val wordPracticeMode: TypeWordsPracticeMode = TypeWordsPracticeMode.SYSTEM,
    val wordPracticeType: TypeWordsPracticeType = TypeWordsPracticeType.FOLLOW_WRITE,
    val autoNextWord: Boolean = true,
    val inputWrongClear: Boolean = false,
    val mobileNavCollapsed: Boolean = false,
    val ignoreSymbol: Boolean = true,
    val practiceSentence: Boolean = false,
    val fsrsEasyLimit: Int = 0,
    val fsrsGoodLimit: Int = 3,
    val fsrsHardLimit: Int = 6,
    val fsrsParameters: TypeWordsFsrsParameters = TypeWordsFsrsParameters(),
    val identifyMethod: TypeWordsIdentifyMethod = TypeWordsIdentifyMethod.SELF_ASSESSMENT,
    val quickIdentify: Boolean = false,
    val ttsVoiceMap: List<TypeWordsTtsVoiceMap> = emptyList()
)

object TypeWordsDefaults {
    const val WORD_COLLECT_ID = "wordCollect"
    const val WORD_WRONG_ID = "wordWrong"
    const val WORD_KNOWN_ID = "wordKnown"
    const val ARTICLE_COLLECT_ID = "articleCollect"

    val simpleWords: List<String> = listOf(
        "a", "an", "i", "my", "me", "you", "your", "he", "his", "she", "her", "it",
        "what", "who", "where", "how", "when", "which", "be", "am", "is", "was",
        "are", "were", "do", "did", "can", "could", "will", "would", "the", "that",
        "this", "and", "not", "no", "yes", "to", "of", "for", "at", "in"
    )

    val shortcutKeyMap: Map<String, String> = mapOf(
        "EditArticle" to "Ctrl+E",
        "ShowWord" to "Escape",
        "Previous" to "Ctrl+⬅",
        "Next" to "Ctrl+➡",
        "Ignore" to "Tab",
        "ToggleSimple" to "`",
        "ToggleCollect" to "Enter",
        "PreviousChapter" to "Alt+⬅",
        "NextChapter" to "Alt+➡",
        "NextStep" to "Shift+➡",
        "RepeatChapter" to "Ctrl+Enter",
        "DictationChapter" to "Alt+Enter",
        "PlayWordPronunciation" to "Ctrl+P",
        "ToggleShowTranslate" to "Ctrl+Z",
        "ToggleDictation" to "Ctrl+I",
        "ToggleTheme" to "Ctrl+Q",
        "ToggleToolbar" to "Ctrl+B",
        "TogglePanel" to "Ctrl+L",
        "RandomWrite" to "Ctrl+R",
        "KnowWord" to "1",
        "UnknownWord" to "2",
        "MasteredWord" to "3",
        "ChooseA" to "1",
        "ChooseB" to "2",
        "ChooseC" to "3",
        "ChooseD" to "4"
    )

    val fsrsWeights: List<Double> = listOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
        0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
        0.1542
    )

    fun defaultWord(value: String = ""): TypeWordsWord =
        TypeWordsWord(word = value)

    fun defaultArticle(value: TypeWordsArticle = TypeWordsArticle()): TypeWordsArticle =
        value

    fun defaultDict(
        id: String = "",
        name: String = "",
        type: TypeWordsDictType = TypeWordsDictType.WORD
    ): TypeWordsDict =
        TypeWordsDict(
            resource = TypeWordsDictResource(id = id, name = name, type = type),
            perDayStudyNumber = 20
        )

    fun defaultWordBookList(): List<TypeWordsDict> = listOf(
        defaultDict(WORD_COLLECT_ID, "收藏", TypeWordsDictType.COLLECT),
        defaultDict(WORD_WRONG_ID, "错词", TypeWordsDictType.WRONG),
        defaultDict(WORD_KNOWN_ID, "已掌握", TypeWordsDictType.KNOWN).copy(
            resource = TypeWordsDictResource(
                id = WORD_KNOWN_ID,
                name = "已掌握",
                description = "已掌握后的单词不会出现在练习中",
                type = TypeWordsDictType.KNOWN
            )
        )
    )

    fun defaultArticleBookList(): List<TypeWordsDict> = listOf(
        defaultDict(ARTICLE_COLLECT_ID, "收藏", TypeWordsDictType.COLLECT)
    )
}

object TypeWordsPracticeRules {
    val stageMap: Map<TypeWordsPracticeMode, List<TypeWordsPracticeStage>?> = mapOf(
        TypeWordsPracticeMode.FREE to listOf(
            TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.IDENTIFY_ONLY to listOf(
            TypeWordsPracticeStage.IDENTIFY_NEW_WORD,
            TypeWordsPracticeStage.IDENTIFY_REVIEW,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.DICTATION_ONLY to listOf(
            TypeWordsPracticeStage.DICTATION_NEW_WORD,
            TypeWordsPracticeStage.DICTATION_REVIEW,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.LISTEN_ONLY to listOf(
            TypeWordsPracticeStage.LISTEN_NEW_WORD,
            TypeWordsPracticeStage.LISTEN_REVIEW,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.SYSTEM to listOf(
            TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
            TypeWordsPracticeStage.LISTEN_NEW_WORD,
            TypeWordsPracticeStage.DICTATION_NEW_WORD,
            TypeWordsPracticeStage.IDENTIFY_REVIEW,
            TypeWordsPracticeStage.LISTEN_REVIEW,
            TypeWordsPracticeStage.DICTATION_REVIEW,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.SHUFFLE to listOf(
            TypeWordsPracticeStage.SHUFFLE,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.REVIEW to listOf(
            TypeWordsPracticeStage.IDENTIFY_REVIEW,
            TypeWordsPracticeStage.LISTEN_REVIEW,
            TypeWordsPracticeStage.DICTATION_REVIEW,
            TypeWordsPracticeStage.COMPLETE
        ),
        TypeWordsPracticeMode.SHUFFLE_WORDS_TEST to null,
        TypeWordsPracticeMode.REVIEW_WORDS_TEST to null
    )

    val stageNameMap: Map<TypeWordsPracticeStage, String> = mapOf(
        TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD to "跟写新词",
        TypeWordsPracticeStage.IDENTIFY_NEW_WORD to "自测新词",
        TypeWordsPracticeStage.LISTEN_NEW_WORD to "听写新词",
        TypeWordsPracticeStage.DICTATION_NEW_WORD to "默写新词",
        TypeWordsPracticeStage.FOLLOW_WRITE_REVIEW to "跟写旧词",
        TypeWordsPracticeStage.IDENTIFY_REVIEW to "自测旧词",
        TypeWordsPracticeStage.LISTEN_REVIEW to "听写旧词",
        TypeWordsPracticeStage.DICTATION_REVIEW to "默写旧词",
        TypeWordsPracticeStage.COMPLETE to "完成学习",
        TypeWordsPracticeStage.SHUFFLE to "随机复习"
    )

    val modeNameMap: Map<TypeWordsPracticeMode, String> = mapOf(
        TypeWordsPracticeMode.SYSTEM to "学习",
        TypeWordsPracticeMode.FREE to "自由练习",
        TypeWordsPracticeMode.IDENTIFY_ONLY to "自测",
        TypeWordsPracticeMode.DICTATION_ONLY to "默写",
        TypeWordsPracticeMode.LISTEN_ONLY to "听写",
        TypeWordsPracticeMode.SHUFFLE to "随机复习",
        TypeWordsPracticeMode.REVIEW to "复习",
        TypeWordsPracticeMode.SHUFFLE_WORDS_TEST to "随机单词测试",
        TypeWordsPracticeMode.REVIEW_WORDS_TEST to "单词测试"
    )

    fun nextStage(
        mode: TypeWordsPracticeMode,
        current: TypeWordsPracticeStage
    ): TypeWordsPracticeStage? =
        stageMap[mode]
            ?.let { stages -> stages.getOrNull(stages.indexOf(current) + 1) }

    fun gradeByWrongTimes(settings: TypeWordsSettingState, wrongTimes: Int?): String =
        when {
            wrongTimes == null -> "Easy"
            wrongTimes <= settings.fsrsEasyLimit -> "Easy"
            wrongTimes <= settings.fsrsGoodLimit -> "Good"
            wrongTimes <= settings.fsrsHardLimit -> "Hard"
            else -> "Again"
        }

    fun currentStudyWords(
        dict: TypeWordsDict,
        settings: TypeWordsSettingState = TypeWordsSettingState(),
        knownWords: List<String> = emptyList(),
        simpleWords: List<String> = TypeWordsDefaults.simpleWords,
        fsrsDueByWord: Map<String, TypeWordsFsrsCard> = emptyMap(),
        now: Long = System.currentTimeMillis()
    ): TypeWordsTaskWords {
        val resultNew = mutableListOf<TypeWordsWord>()
        val resultReview = mutableListOf<TypeWordsWord>()
        val words = dict.words
        if (words.isEmpty()) return TypeWordsTaskWords()

        val known = knownWords.map { it.lowercase() }.toSet()
        val simple = simpleWords.map { it.lowercase() }.toSet()
        val ignoreSet = if (settings.ignoreSimpleWord) known + simple else known
        val perDay = dict.perDayStudyNumber.coerceAtLeast(0)
        val start = dict.lastLearnIndex.coerceAtLeast(0)
        val complete = dict.complete
        val isEnd = start >= dict.length - 1

        var end = start
        if (!isEnd) {
            for (index in start until words.size) {
                val item = words[index]
                if (resultNew.size >= perDay) break
                if (item.normalized !in ignoreSet) {
                    resultNew += item
                }
                end++
            }
        }

        val reviewRatio = settings.wordReviewRatio
        if (reviewRatio >= 1 || complete || isEnd) {
            val wordMap = words.associateBy { it.normalized }
            val totalNeed = perDay * if (isEnd) reviewRatio.coerceAtLeast(1) else reviewRatio
            val newSet = resultNew.map { it.normalized }.toSet()

            val dueReviewWords = fsrsDueByWord.entries
                .filter { (word, card) ->
                    val normalized = TextTools.normalizeWord(word)
                    normalized !in ignoreSet &&
                        card.dueAt <= now &&
                        normalized in wordMap &&
                        normalized !in newSet
                }
                .sortedBy { it.value.dueAt }
                .mapNotNull { wordMap[TextTools.normalizeWord(it.key)] }

            resultReview += dueReviewWords.take(totalNeed)

            if (resultReview.size < totalNeed) {
                val blocked = ignoreSet +
                    fsrsDueByWord.keys.map { TextTools.normalizeWord(it) } +
                    newSet +
                    resultReview.map { it.normalized }.toSet()
                var fallback = words.take(start).asReversed()
                if (complete) fallback = fallback + words.drop(end).asReversed()
                resultReview += fallback
                    .filter { it.normalized !in blocked }
                    .take(totalNeed - resultReview.size)
            }
        }

        return TypeWordsTaskWords(new = resultNew, review = resultReview)
    }
}

object TypeWordsJsonParser {
    fun parseDicts(text: String, fallbackName: String = "导入词书"): List<TypeWordsDict> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        return when {
            trimmed.startsWith("[") -> listOf(parseLooseArray(JSONArray(trimmed), fallbackName))
            else -> parseRoot(JSONObject(trimmed), fallbackName)
        }.filter { it.words.isNotEmpty() || it.articles.isNotEmpty() || it.id.isNotBlank() }
    }

    fun parseRoot(json: JSONObject, fallbackName: String = "导入词书"): List<TypeWordsDict> {
        val value = json.optJSONObject("val")
        if (json.has("version") && value != null) {
            return parseBaseState(value)
        }
        if (json.opt("word") is JSONObject || json.opt("article") is JSONObject) {
            return parseBaseState(json)
        }
        if (json.has("words") || json.has("articles") || json.has("lastLearnIndex") || json.has("perDayStudyNumber")) {
            return listOf(parseDict(json, fallbackName))
        }
        if (json.has("word") && json.has("trans")) {
            return listOf(TypeWordsDefaults.defaultDict(name = fallbackName).copy(words = listOf(parseWord(json))))
        }
        return emptyList()
    }

    fun parseBaseState(json: JSONObject): List<TypeWordsDict> {
        val wordBooks = json.optJSONObject("word")
            ?.optJSONArray("bookList")
            .orEmptyObjects()
            .map { parseDict(it, "导入单词本") }
        val articleBooks = json.optJSONObject("article")
            ?.optJSONArray("bookList")
            .orEmptyObjects()
            .map { parseDict(it, "导入文章本", TypeWordsDictType.ARTICLE) }
        return wordBooks + articleBooks
    }

    fun parseDict(
        json: JSONObject,
        fallbackName: String = "导入词书",
        fallbackType: TypeWordsDictType = TypeWordsDictType.WORD
    ): TypeWordsDict {
        val type = parseDictType(json.optString("type"), fallbackType)
        val words = json.optJSONArray("words").orEmptyObjects().map(::parseWord)
        val articles = json.optJSONArray("articles").orEmptyObjects().map(::parseArticle)
        val resource = TypeWordsDictResource(
            id = json.optAnyString("id").ifBlank { "typewords-${json.optAnyString("name").ifBlank { fallbackName }.hashCode()}" },
            name = json.optAnyString("name").ifBlank { fallbackName },
            description = json.optAnyString("description"),
            url = json.optAnyString("url"),
            length = json.optInt("length", words.size.coerceAtLeast(articles.size)),
            category = json.optAnyString("category"),
            tags = json.optJSONArray("tags").orEmptyStrings(),
            translateLanguage = json.optAnyString("translateLanguage"),
            type = type,
            version = json.optInt("version", 0),
            language = json.optAnyString("language").ifBlank { "en" }
        )
        return TypeWordsDict(
            resource = resource,
            lastLearnIndex = json.optInt("lastLearnIndex", 0),
            perDayStudyNumber = json.optInt("perDayStudyNumber", 20),
            words = words,
            articles = articles,
            statistics = json.optJSONArray("statistics").orEmptyObjects().map(::parseStatistics),
            custom = json.optBoolean("custom", false),
            complete = json.optBoolean("complete", false),
            enName = json.optAnyString("en_name").ifBlank { json.optAnyString("enName") },
            createdBy = json.optAnyString("createdBy"),
            categoryId = json.optNullableInt("category_id") ?: json.optNullableInt("categoryId"),
            isDefault = json.optBoolean("is_default", json.optBoolean("isDefault", false)),
            update = json.optBoolean("update", false),
            cover = json.optAnyString("cover"),
            sync = json.optBoolean("sync", false),
            userDictId = json.optNullableInt("userDictId")
        )
    }

    fun parseWord(json: JSONObject): TypeWordsWord =
        TypeWordsWord(
            id = json.optNullableString("id"),
            custom = json.optBoolean("custom", false),
            word = json.optAnyString("word"),
            phonetic0 = json.optAnyString("phonetic0"),
            phonetic1 = json.optAnyString("phonetic1"),
            trans = json.optJSONArray("trans").orEmptyObjects().map {
                TypeWordsTranslation(pos = it.optAnyString("pos"), cn = it.optAnyString("cn"))
            },
            sentences = json.optJSONArray("sentences").orEmptyObjects().map {
                TypeWordsSentenceExample(c = it.optAnyString("c"), cn = it.optAnyString("cn"))
            },
            phrases = json.optJSONArray("phrases").orEmptyObjects().map {
                TypeWordsPhrase(c = it.optAnyString("c"), cn = it.optAnyString("cn"))
            },
            synos = json.optJSONArray("synos").orEmptyObjects().map {
                TypeWordsSynonym(
                    pos = it.optAnyString("pos"),
                    cn = it.optAnyString("cn"),
                    ws = it.optJSONArray("ws").orEmptyStrings()
                )
            },
            relWords = parseRelatedWords(json.optJSONObject("relWords")),
            etymology = json.optJSONArray("etymology").orEmptyObjects().map {
                TypeWordsEtymology(t = it.optAnyString("t"), d = it.optAnyString("d"))
            }
        )

    fun parseArticle(json: JSONObject): TypeWordsArticle =
        TypeWordsArticle(
            id = json.optNullableString("id"),
            title = json.optAnyString("title"),
            titleTranslate = json.optAnyString("titleTranslate"),
            text = json.optAnyString("text"),
            textTranslate = json.optAnyString("textTranslate"),
            newWords = json.optJSONArray("newWords").orEmptyObjects().map(::parseWord),
            sections = json.optJSONArray("sections").orEmptyArrays().map { section ->
                section.orEmptyObjects().map(::parseSentence)
            },
            audioSrc = json.optAnyString("audioSrc"),
            audioFileId = json.optAnyString("audioFileId"),
            lrcPosition = json.optJSONArray("lrcPosition").orEmptyArrays().map { it.orEmptyInts() },
            nameList = json.optJSONArray("nameList").orEmptyStrings(),
            questions = json.optJSONArray("questions").orEmptyObjects().map {
                TypeWordsArticleQuestion(
                    stem = it.optAnyString("stem"),
                    options = it.optJSONArray("options").orEmptyStrings(),
                    correctAnswer = it.optJSONArray("correctAnswer").orEmptyStrings(),
                    explanation = it.optAnyString("explanation")
                )
            },
            quote = parseQuote(json.optJSONObject("quote")),
            question = parseQuote(json.optJSONObject("question"))
        )

    fun wordToJson(word: TypeWordsWord): JSONObject =
        JSONObject()
            .put("id", word.id)
            .put("custom", word.custom)
            .put("word", word.word)
            .put("phonetic0", word.phonetic0)
            .put("phonetic1", word.phonetic1)
            .put("trans", JSONArray(word.trans.map { JSONObject().put("pos", it.pos).put("cn", it.cn) }))
            .put("sentences", JSONArray(word.sentences.map { JSONObject().put("c", it.c).put("cn", it.cn) }))
            .put("phrases", JSONArray(word.phrases.map { JSONObject().put("c", it.c).put("cn", it.cn) }))
            .put("synos", JSONArray(word.synos.map { JSONObject().put("pos", it.pos).put("cn", it.cn).put("ws", JSONArray(it.ws)) }))
            .put(
                "relWords",
                JSONObject()
                    .put("root", word.relWords.root)
                    .put(
                        "rels",
                        JSONArray(word.relWords.rels.map { group ->
                            JSONObject()
                                .put("pos", group.pos)
                                .put("words", JSONArray(group.words.map { JSONObject().put("c", it.c).put("cn", it.cn) }))
                        })
                    )
            )
            .put("etymology", JSONArray(word.etymology.map { JSONObject().put("t", it.t).put("d", it.d) }))

    private fun parseLooseArray(array: JSONArray, fallbackName: String): TypeWordsDict {
        val objects = array.orEmptyObjects()
        val looksLikeArticle = objects.firstOrNull()?.has("sections") == true || objects.firstOrNull()?.has("textTranslate") == true
        return if (looksLikeArticle) {
            TypeWordsDefaults.defaultDict(name = fallbackName, type = TypeWordsDictType.ARTICLE).copy(
                articles = objects.map(::parseArticle)
            )
        } else {
            TypeWordsDefaults.defaultDict(name = fallbackName, type = TypeWordsDictType.WORD).copy(
                words = objects.map(::parseWord)
            )
        }
    }

    private fun parseSentence(json: JSONObject): TypeWordsSentence =
        TypeWordsSentence(
            text = json.optAnyString("text"),
            translate = json.optAnyString("translate"),
            words = json.optJSONArray("words").orEmptyObjects().map { parseArticleWord(it) },
            audioPosition = json.optJSONArray("audioPosition").orEmptyInts()
        )

    private fun parseArticleWord(json: JSONObject): TypeWordsArticleWord =
        TypeWordsArticleWord(
            base = parseWord(json),
            nextSpace = json.optBoolean("nextSpace", true),
            symbolPosition = json.optAnyString("symbolPosition"),
            input = json.optAnyString("input"),
            type = parseArticleWordType(json.optAnyString("type"))
        )

    private fun parseRelatedWords(json: JSONObject?): TypeWordsRelatedWords {
        if (json == null) return TypeWordsRelatedWords()
        return TypeWordsRelatedWords(
            root = json.optAnyString("root"),
            rels = json.optJSONArray("rels").orEmptyObjects().map { group ->
                TypeWordsRelatedWordGroup(
                    pos = group.optAnyString("pos"),
                    words = group.optJSONArray("words").orEmptyObjects().map {
                        TypeWordsPhrase(c = it.optAnyString("c"), cn = it.optAnyString("cn"))
                    }
                )
            }
        )
    }

    private fun parseQuote(json: JSONObject?): TypeWordsArticleQuote? {
        if (json == null) return null
        return TypeWordsArticleQuote(
            start = json.optInt("start", 0),
            text = json.optAnyString("text"),
            translate = json.optAnyString("translate"),
            end = json.optInt("end", 0)
        )
    }

    private fun parseStatistics(json: JSONObject): TypeWordsStatistics =
        TypeWordsStatistics(
            startDate = json.optLong("startDate", 0L),
            spend = json.optLong("spend", 0L),
            total = json.optInt("total", 0),
            new = json.optInt("new", 0),
            review = json.optInt("review", 0),
            wrong = json.optInt("wrong", 0),
            title = json.optAnyString("title")
        )

    private fun parseDictType(value: String, fallback: TypeWordsDictType): TypeWordsDictType {
        val normalized = value.trim()
        return when {
            normalized.equals("collect", true) -> TypeWordsDictType.COLLECT
            normalized.equals("simple", true) -> TypeWordsDictType.SIMPLE
            normalized.equals("wrong", true) -> TypeWordsDictType.WRONG
            normalized.equals("known", true) -> TypeWordsDictType.KNOWN
            normalized.equals("word", true) -> TypeWordsDictType.WORD
            normalized.equals("article", true) -> TypeWordsDictType.ARTICLE
            else -> runCatching { TypeWordsDictType.valueOf(normalized.uppercase()) }.getOrDefault(fallback)
        }
    }

    private fun parseArticleWordType(value: String): TypeWordsPracticeArticleWordType =
        when {
            value.equals("symbol", true) || value == "0" -> TypeWordsPracticeArticleWordType.SYMBOL
            value.equals("number", true) || value == "1" -> TypeWordsPracticeArticleWordType.NUMBER
            else -> TypeWordsPracticeArticleWordType.WORD
        }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private fun JSONArray?.orEmptyArrays(): List<JSONArray> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONArray(it) }
    }

    private fun JSONArray?.orEmptyStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }

    private fun JSONArray?.orEmptyInts(): List<Int> {
        if (this == null) return emptyList()
        return (0 until length()).map { optInt(it) }
    }

    private fun JSONObject.optAnyString(name: String): String {
        if (!has(name) || isNull(name)) return ""
        return opt(name)?.toString().orEmpty()
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) opt(name)?.toString() else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) runCatching { getInt(name) }.getOrNull() else null
}

fun TypeWordsWord.toLearningWord(
    sourceTitle: String = "导入词书",
    now: Long = System.currentTimeMillis()
): LearningWord =
    LearningWord(
        id = id ?: "typewords-${normalized}",
        word = word,
        normalized = normalized,
        phonetic = phonetic0.ifBlank { phonetic1 },
        chineseDefinition = definitionText(),
        status = LearningWordStatus.NEW_WORD,
        contexts = sentences.take(3).mapIndexed { index, sentence ->
            com.xingyue.english.core.SourceContext(
                sourceItemId = "typewords-${sourceTitle}",
                sourceTitle = sourceTitle,
                captionId = "",
                captionStartMs = 0L,
                captionEndMs = 0L,
                englishSentence = sentence.c,
                chineseSentence = sentence.cn,
                sourceType = LearningSourceType.DOCUMENT,
                paragraphIndex = index
            )
        },
        createdAt = now,
        updatedAt = now,
        dueAt = now,
        notes = relWords.root
    )
