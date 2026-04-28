package com.xingyue.english.core

import com.xingyue.english.core.source.TypeWordsDefaults
import com.xingyue.english.core.source.TypeWordsPracticeMode
import com.xingyue.english.core.source.TypeWordsPracticeRules
import com.xingyue.english.core.source.TypeWordsPracticeStage
import com.xingyue.english.core.source.TypeWordsSettingState
import com.xingyue.english.core.source.TypeWordsTranslation
import com.xingyue.english.core.source.TypeWordsWord
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

enum class PracticeMode {
    COPY_TYPING,
    DICTATION,
    SELF_TEST,
    CN_TO_EN,
    SPELLING_MEMORY,
    TYPEWORDS_SYSTEM,
    TYPEWORDS_FREE,
    TYPEWORDS_REVIEW,
    TYPEWORDS_SHUFFLE
}

data class PracticePrompt(
    val id: String,
    val expected: String,
    val hint: String = "",
    val chinese: String = "",
    val sourceId: String = "",
    val sourceType: String = "",
    val normalized: String = TextTools.normalizeWord(expected)
)

data class TypingPracticeResult(
    val promptId: String,
    val expected: String,
    val answer: String,
    val correct: Boolean,
    val accuracy: Int,
    val wpm: Int,
    val typoCount: Int,
    val normalizedExpected: String,
    val normalizedAnswer: String
)

data class PracticeSessionSummary(
    val targetCount: Int,
    val completedCount: Int,
    val correctCount: Int,
    val averageAccuracy: Int,
    val completed: Boolean
) {
    val progressFraction: Float
        get() = if (targetCount <= 0) 0f else (completedCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
}

object PracticeSessionEngine {
    fun summarize(prompts: List<PracticePrompt>, results: List<TypingPracticeResult>): PracticeSessionSummary {
        val promptIds = prompts.map { it.id }.toSet()
        val scoped = results
            .filter { it.promptId in promptIds }
            .distinctBy { it.promptId }
        val completedCount = scoped.size.coerceAtMost(prompts.size)
        return PracticeSessionSummary(
            targetCount = prompts.size,
            completedCount = completedCount,
            correctCount = scoped.count { it.correct },
            averageAccuracy = if (scoped.isEmpty()) 0 else scoped.map { it.accuracy }.average().roundToInt().coerceIn(0, 100),
            completed = prompts.isNotEmpty() && completedCount >= prompts.size
        )
    }

    fun nextIndex(currentIndex: Int, prompts: List<PracticePrompt>, results: List<TypingPracticeResult>): Int? {
        if (prompts.isEmpty()) return null
        val answered = results.map { it.promptId }.toSet()
        val nextUnanswered = prompts.indexOfFirst { it.id !in answered }
        return when {
            nextUnanswered >= 0 -> nextUnanswered
            currentIndex < prompts.lastIndex -> currentIndex + 1
            else -> null
        }
    }
}

object TypingPracticeEngine {
    fun promptsFromLexicalItems(items: List<LexicalItem>, mode: PracticeMode, limit: Int = 20): List<PracticePrompt> =
        items.take(limit).map { item ->
            PracticePrompt(
                id = item.id,
                expected = item.word,
                hint = when (mode) {
                    PracticeMode.COPY_TYPING -> item.word
                    PracticeMode.DICTATION -> item.phonetic.ifBlank { "听发音后输入单词" }
                    PracticeMode.SELF_TEST -> item.definition
                    PracticeMode.CN_TO_EN -> item.definition
                    PracticeMode.SPELLING_MEMORY -> item.example.ifBlank { item.definition }
                    PracticeMode.TYPEWORDS_SYSTEM,
                    PracticeMode.TYPEWORDS_FREE,
                    PracticeMode.TYPEWORDS_REVIEW,
                    PracticeMode.TYPEWORDS_SHUFFLE -> item.word
                },
                chinese = item.definition,
                sourceId = item.deckId,
                sourceType = item.stage.name,
                normalized = item.normalized
            )
        }

    fun evaluate(
        prompt: PracticePrompt,
        answer: String,
        elapsedMs: Long,
        strictPunctuation: Boolean = false
    ): TypingPracticeResult {
        val expectedNormalized = normalizeAnswer(prompt.expected, strictPunctuation)
        val answerNormalized = normalizeAnswer(answer, strictPunctuation)
        val distance = levenshtein(expectedNormalized, answerNormalized)
        val base = max(expectedNormalized.length, 1)
        val accuracy = ((1.0 - distance.toDouble() / base.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
        val minutes = (elapsedMs.coerceAtLeast(1L)).toDouble() / 60_000.0
        val wpm = if (minutes <= 0.0) 0 else (answerNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }.size / minutes)
            .roundToInt()
            .coerceAtLeast(0)
        return TypingPracticeResult(
            promptId = prompt.id,
            expected = prompt.expected,
            answer = answer,
            correct = expectedNormalized == answerNormalized,
            accuracy = if (expectedNormalized == answerNormalized) 100 else accuracy,
            wpm = wpm,
            typoCount = distance,
            normalizedExpected = expectedNormalized,
            normalizedAnswer = answerNormalized
        )
    }

    private fun normalizeAnswer(value: String, strictPunctuation: Boolean): String {
        val lowered = value.lowercase().trim()
        return if (strictPunctuation) {
            lowered.replace(Regex("\\s+"), " ")
        } else {
            lowered
                .replace(Regex("[^a-z0-9\\s']"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        val costs = IntArray(right.length + 1) { it }
        for (i in 1..left.length) {
            var previous = costs[0]
            costs[0] = i
            for (j in 1..right.length) {
                val old = costs[j]
                val replaceCost = if (left[i - 1] == right[j - 1]) previous else previous + 1
                costs[j] = minOf(costs[j] + 1, costs[j - 1] + 1, replaceCost)
                previous = old
            }
        }
        return costs[right.length]
    }
}

object ArticleDictationEngine {
    fun fromCaptionCues(cues: List<CaptionCue>, limit: Int = 12): List<PracticePrompt> =
        cues.asSequence()
            .filter { it.english.isNotBlank() }
            .take(limit)
            .mapIndexed { index, cue ->
                PracticePrompt(
                    id = "cue-${cue.id.ifBlank { index.toString() }}",
                    expected = cue.english,
                    hint = cue.chinese.ifBlank { formatCueTime(cue.startMs) },
                    chinese = cue.chinese,
                    sourceId = cue.id,
                    sourceType = "caption"
                )
            }
            .toList()

    fun fromPhraseChunks(chunks: List<PhraseChunk>, limit: Int = 12): List<PracticePrompt> =
        chunks.take(limit).map { chunk ->
            PracticePrompt(
                id = chunk.id,
                expected = chunk.english,
                hint = chunk.chinese,
                chinese = chunk.chinese,
                sourceId = chunk.deckId,
                sourceType = chunk.useCase.name
            )
        }

    fun fromPlainText(contentId: String, title: String, text: String, limit: Int = 12): List<PracticePrompt> =
        text.split(Regex("(?<=[.!?。！？])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 8 && TextTools.hasEnglish(it) }
            .take(limit)
            .mapIndexed { index, sentence ->
                PracticePrompt(
                    id = "$contentId-$index",
                    expected = sentence,
                    hint = title,
                    sourceId = contentId,
                    sourceType = "document"
                )
            }

    private fun formatCueTime(ms: Long): String {
        val seconds = (ms / 1000L).coerceAtLeast(0)
        return "%02d:%02d".format(seconds / 60L, seconds % 60L)
    }
}

data class TypeWordsFlowPrompt(
    val stage: TypeWordsPracticeStage,
    val typeWordsMode: TypeWordsPracticeMode,
    val prompt: PracticePrompt,
    val practiceMode: PracticeMode,
    val review: Boolean
)

object TypeWordsPracticeFlowEngine {
    fun prompts(
        appMode: PracticeMode,
        newWords: List<TypeWordsWord>,
        reviewWords: List<TypeWordsWord>,
        settings: TypeWordsSettingState = TypeWordsSettingState(),
        sessionLimit: Int = 60
    ): List<TypeWordsFlowPrompt> {
        val typeWordsMode = appMode.toTypeWordsPracticeMode()
        val stages = TypeWordsPracticeRules.stageMap[typeWordsMode] ?: listOf(TypeWordsPracticeStage.SHUFFLE)
        val filteredStages = stages.filter { it != TypeWordsPracticeStage.COMPLETE }
        val newSet = newWords.distinctBy { it.normalized }.take(settings.wordDailyTarget())
        val reviewSet = reviewWords.distinctBy { it.normalized }.take((settings.wordDailyTarget() * settings.wordReviewRatio.coerceAtLeast(1)).coerceAtLeast(1))
        val base = if (typeWordsMode == TypeWordsPracticeMode.SHUFFLE) {
            (newSet + reviewSet).distinctBy { it.normalized }.sortedBy { it.normalized }
        } else {
            emptyList()
        }
        return filteredStages.flatMap { stage ->
            val words = when (stage) {
                TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
                TypeWordsPracticeStage.IDENTIFY_NEW_WORD,
                TypeWordsPracticeStage.LISTEN_NEW_WORD,
                TypeWordsPracticeStage.DICTATION_NEW_WORD -> newSet
                TypeWordsPracticeStage.FOLLOW_WRITE_REVIEW,
                TypeWordsPracticeStage.IDENTIFY_REVIEW,
                TypeWordsPracticeStage.LISTEN_REVIEW,
                TypeWordsPracticeStage.DICTATION_REVIEW -> reviewSet
                TypeWordsPracticeStage.SHUFFLE -> base
                TypeWordsPracticeStage.COMPLETE -> emptyList()
            }
            words.mapIndexed { index, word ->
                TypeWordsFlowPrompt(
                    stage = stage,
                    typeWordsMode = typeWordsMode,
                    prompt = promptFor(stage, word, index),
                    practiceMode = stage.practiceMode(),
                    review = stage.name.endsWith("_REVIEW") || typeWordsMode == TypeWordsPracticeMode.REVIEW
                )
            }
        }.take(sessionLimit)
    }

    fun toTypeWordsWord(item: LexicalItem): TypeWordsWord =
        TypeWordsWord(
            id = item.id,
            word = item.word,
            phonetic0 = item.phonetic,
            trans = listOf(TypeWordsTranslation("", item.definition)),
            sentences = item.example.takeIf { it.isNotBlank() }?.let {
                listOf(com.xingyue.english.core.source.TypeWordsSentenceExample(c = it))
            }.orEmpty(),
            phrases = item.phrases.map { com.xingyue.english.core.source.TypeWordsPhrase(c = it) }
        )

    fun toTypeWordsWord(word: LearningWord): TypeWordsWord =
        TypeWordsWord(
            id = word.id,
            word = word.word,
            phonetic0 = word.phonetic,
            trans = listOf(TypeWordsTranslation("", word.chineseDefinition)),
            sentences = word.contexts.firstOrNull()?.let {
                listOf(com.xingyue.english.core.source.TypeWordsSentenceExample(c = it.englishSentence, cn = it.chineseSentence))
            }.orEmpty()
        )

    private fun promptFor(stage: TypeWordsPracticeStage, word: TypeWordsWord, index: Int): PracticePrompt {
        val definition = word.definitionText()
        val sentence = word.sentences.firstOrNull()
        val hint = when (stage.practiceMode()) {
            PracticeMode.COPY_TYPING -> word.word
            PracticeMode.DICTATION -> word.phonetic0.ifBlank { "听发音后输入单词" }
            PracticeMode.SELF_TEST -> definition.ifBlank { sentence?.cn.orEmpty() }
            PracticeMode.CN_TO_EN -> definition.ifBlank { sentence?.cn.orEmpty() }
            else -> definition
        }
        return PracticePrompt(
            id = "${stage.name.lowercase()}-${word.normalized.ifBlank { index.toString() }}",
            expected = word.word,
            hint = hint,
            chinese = definition,
            sourceId = word.id.orEmpty(),
            sourceType = stage.name,
            normalized = word.normalized
        )
    }

    private fun TypeWordsPracticeStage.practiceMode(): PracticeMode =
        when (this) {
            TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
            TypeWordsPracticeStage.FOLLOW_WRITE_REVIEW -> PracticeMode.COPY_TYPING
            TypeWordsPracticeStage.IDENTIFY_NEW_WORD,
            TypeWordsPracticeStage.IDENTIFY_REVIEW -> PracticeMode.SELF_TEST
            TypeWordsPracticeStage.LISTEN_NEW_WORD,
            TypeWordsPracticeStage.LISTEN_REVIEW -> PracticeMode.DICTATION
            TypeWordsPracticeStage.DICTATION_NEW_WORD,
            TypeWordsPracticeStage.DICTATION_REVIEW -> PracticeMode.CN_TO_EN
            TypeWordsPracticeStage.SHUFFLE -> PracticeMode.CN_TO_EN
            TypeWordsPracticeStage.COMPLETE -> PracticeMode.SELF_TEST
        }

    private fun PracticeMode.toTypeWordsPracticeMode(): TypeWordsPracticeMode =
        when (this) {
            PracticeMode.TYPEWORDS_FREE -> TypeWordsPracticeMode.FREE
            PracticeMode.TYPEWORDS_REVIEW -> TypeWordsPracticeMode.REVIEW
            PracticeMode.TYPEWORDS_SHUFFLE -> TypeWordsPracticeMode.SHUFFLE
            else -> TypeWordsPracticeMode.SYSTEM
        }

    private fun TypeWordsSettingState.wordDailyTarget(): Int =
        TypeWordsDefaults.defaultDict().perDayStudyNumber.coerceAtLeast(1)
}

enum class AchievementType {
    FIRST_IMPORT,
    FIRST_WORD,
    STREAK_3,
    TYPING_90,
    LISTENING_30,
    WORDLE_WIN,
    MANUAL_COLLECTOR
}

data class AchievementDefinition(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String,
    val threshold: Int = 1
)

data class AchievementUnlock(
    val definition: AchievementDefinition,
    val unlockedAt: Long = System.currentTimeMillis(),
    val source: String = ""
)

object AchievementEngine {
    val defaultDefinitions: List<AchievementDefinition> = listOf(
        AchievementDefinition("first-import", AchievementType.FIRST_IMPORT, "月白档案启动", "完成第一次学习素材导入"),
        AchievementDefinition("first-word", AchievementType.FIRST_WORD, "第一枚星词", "手动保存第一个字幕生词"),
        AchievementDefinition("streak-3", AchievementType.STREAK_3, "三日月轨", "连续学习 3 天", threshold = 3),
        AchievementDefinition("typing-90", AchievementType.TYPING_90, "精准咒文", "打字/听写正确率达到 90%"),
        AchievementDefinition("listening-30", AchievementType.LISTENING_30, "三十分钟声场", "累计听力练习 30 分钟", threshold = 30),
        AchievementDefinition("wordle-win", AchievementType.WORDLE_WIN, "词阵破译", "赢得一局 Wordle"),
        AchievementDefinition("manual-collector", AchievementType.MANUAL_COLLECTOR, "手动收词者", "保存 10 个自己确认的生词", threshold = 10)
    )

    fun evaluate(
        contents: List<ImportedContent>,
        words: List<LearningWord>,
        attempts: List<StudyAttempt>,
        typingResults: List<TypingPracticeResult> = emptyList(),
        wonWordGames: Int = 0,
        unlockedIds: Set<String> = emptySet()
    ): List<AchievementUnlock> {
        val listeningMinutes = attempts
            .filter { it.taskType == StudyTaskType.LISTENING_REPEAT || it.taskType == StudyTaskType.HUNDRED_LS }
            .sumOf { (it.durationMs / 60_000L).toInt().coerceAtLeast(0) }
        val streakDays = LearningPlanEngine.streakDays(attempts)
        val checks = mapOf(
            AchievementType.FIRST_IMPORT to contents.isNotEmpty(),
            AchievementType.FIRST_WORD to words.isNotEmpty(),
            AchievementType.STREAK_3 to (streakDays >= 3),
            AchievementType.TYPING_90 to typingResults.any { it.accuracy >= 90 },
            AchievementType.LISTENING_30 to (listeningMinutes >= 30),
            AchievementType.WORDLE_WIN to (wonWordGames > 0),
            AchievementType.MANUAL_COLLECTOR to (words.size >= 10)
        )
        return defaultDefinitions
            .filter { it.id !in unlockedIds && checks[it.type] == true }
            .map { AchievementUnlock(it, source = it.type.name) }
    }
}

enum class WordleTileState {
    CORRECT,
    PRESENT,
    ABSENT
}

data class WordleTile(
    val char: Char,
    val state: WordleTileState
)

data class WordleGuessResult(
    val secret: String,
    val guess: String,
    val tiles: List<WordleTile>,
    val won: Boolean,
    val remainingAttempts: Int
)

data class GuessDescriptionQuestion(
    val word: String,
    val description: String,
    val choices: List<String>
)

object WordGameEngine {
    fun evaluateWordle(secret: String, guess: String, attemptIndex: Int = 1, maxAttempts: Int = 6): WordleGuessResult {
        val normalizedSecret = TextTools.normalizeWord(secret).take(12)
        val normalizedGuess = TextTools.normalizeWord(guess).take(normalizedSecret.length)
        val remaining = mutableMapOf<Char, Int>()
        normalizedSecret.forEachIndexed { index, char ->
            if (normalizedGuess.getOrNull(index) != char) {
                remaining[char] = remaining.getOrDefault(char, 0) + 1
            }
        }
        val states = normalizedGuess.mapIndexed { index, char ->
            when {
                normalizedSecret.getOrNull(index) == char -> WordleTile(char, WordleTileState.CORRECT)
                remaining.getOrDefault(char, 0) > 0 -> {
                    remaining[char] = remaining.getOrDefault(char, 0) - 1
                    WordleTile(char, WordleTileState.PRESENT)
                }
                else -> WordleTile(char, WordleTileState.ABSENT)
            }
        }
        return WordleGuessResult(
            secret = normalizedSecret,
            guess = normalizedGuess,
            tiles = states,
            won = normalizedSecret == normalizedGuess,
            remainingAttempts = (maxAttempts - attemptIndex).coerceAtLeast(0)
        )
    }

    fun offlineGuessQuestions(items: List<LexicalItem>, count: Int = 5): List<GuessDescriptionQuestion> {
        val candidates = items.filter { it.definition.isNotBlank() && it.word.length >= 4 }
        return candidates.take(count).mapIndexed { index, item ->
            val distractors = candidates
                .filter { it.normalized != item.normalized }
                .drop(index)
                .take(3)
                .map { it.word }
            GuessDescriptionQuestion(
                word = item.word,
                description = item.definition,
                choices = (distractors + item.word).distinct().sorted()
            )
        }
    }
}

data class StudyAnalytics(
    val todayAttempts: Int,
    val accuracy: Int,
    val dueCount: Int,
    val mistakeCount: Int,
    val listeningMinutes: Int,
    val savedWords: Int,
    val unlockedAchievements: Int
)

object StudyAnalyticsEngine {
    fun build(
        attempts: List<StudyAttempt>,
        words: List<LearningWord>,
        unlockedAchievements: Int = 0,
        now: Long = System.currentTimeMillis()
    ): StudyAnalytics {
        val today = attempts.filter { sameDay(it.completedOrClosedAt, now) && it.countsAsAttempt }
        val scored = today.filter { it.score > 0 }
        val listening = attempts
            .filter { it.taskType == StudyTaskType.LISTENING_REPEAT || it.taskType == StudyTaskType.HUNDRED_LS }
            .sumOf { (it.durationMs / 60_000L).toInt().coerceAtLeast(0) }
        return StudyAnalytics(
            todayAttempts = today.size,
            accuracy = if (scored.isEmpty()) 0 else scored.map { it.score }.average().roundToInt().coerceIn(0, 100),
            dueCount = words.count { it.status == LearningWordStatus.DUE || it.dueAt <= now },
            mistakeCount = attempts.count { it.isMistake },
            listeningMinutes = listening,
            savedWords = words.size,
            unlockedAchievements = unlockedAchievements
        )
    }

    private fun sameDay(left: Long, right: Long): Boolean =
        left / 86_400_000L == right / 86_400_000L
}

data class DeckImportItem(
    val word: String,
    val phonetic: String = "",
    val definition: String = "",
    val cefr: String = "",
    val deck: String = "",
    val tags: List<String> = emptyList(),
    val phrases: List<String> = emptyList(),
    val example: String = "",
    val licenseSource: String = "用户私有词库"
)

data class DeckImportResult(
    val items: List<DeckImportItem>,
    val errors: List<String>
)

object DeckImportNormalizer {
    private val separators = Regex("[,;|]")

    fun parseCsv(text: String): DeckImportResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return DeckImportResult(emptyList(), listOf("空文件"))
        val header = lines.first().split(',').map { it.trim() }
        val index = header.withIndex().associate { it.value to it.index }
        val required = listOf("word", "definition")
        val missing = required.filter { it !in index }
        if (missing.isNotEmpty()) return DeckImportResult(emptyList(), listOf("缺少字段: ${missing.joinToString()}"))
        val errors = mutableListOf<String>()
        val items = lines.drop(1).mapNotNull { line ->
            val cells = splitCsvLine(line)
            val word = cells.getOrNull(index.getValue("word")).orEmpty().trim()
            if (word.isBlank()) {
                errors += "空单词: $line"
                null
            } else {
                DeckImportItem(
                    word = word,
                    phonetic = cells.valueAt(index, "phonetic"),
                    definition = cells.valueAt(index, "definition"),
                    cefr = cells.valueAt(index, "cefr"),
                    deck = cells.valueAt(index, "deck"),
                    tags = cells.valueAt(index, "tags").split(separators).map { it.trim() }.filter { it.isNotBlank() },
                    phrases = cells.valueAt(index, "phrases").split('|').map { it.trim() }.filter { it.isNotBlank() },
                    example = cells.valueAt(index, "example"),
                    licenseSource = cells.valueAt(index, "licenseSource").ifBlank { "用户私有词库" }
                )
            }
        }
        return DeckImportResult(items.distinctBy { TextTools.normalizeWord(it.word) }, errors)
    }

    fun parseJson(text: String): DeckImportResult {
        val errors = mutableListOf<String>()
        val root = runCatching { JSONObject(text) }.getOrElse {
            return DeckImportResult(emptyList(), listOf("JSON 格式错误: ${it.message}"))
        }
        val array = when {
            root.has("items") -> root.optJSONArray("items") ?: JSONArray()
            root.has("words") -> root.optJSONArray("words") ?: JSONArray()
            root.has("wordList") -> root.optJSONArray("wordList") ?: JSONArray()
            else -> JSONArray().put(root)
        }
        val items = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index)
            if (item == null) {
                errors += "第 ${index + 1} 项不是对象"
                null
            } else {
                val word = item.optString("word").ifBlank { item.optString("value").ifBlank { item.optString("name") } }
                if (word.isBlank()) {
                    errors += "第 ${index + 1} 项缺少 word"
                    null
                } else {
                    DeckImportItem(
                        word = word,
                        phonetic = item.optString("phonetic").ifBlank { item.optString("usphone").ifBlank { item.optString("ukphone") } },
                        definition = item.optString("definition").ifBlank { item.optString("translation") },
                        cefr = item.optString("cefr"),
                        deck = item.optString("deck").ifBlank { root.optString("name") },
                        tags = item.optList("tags").ifEmpty {
                            item.optString("tag").split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                        },
                        phrases = item.optList("phrases"),
                        example = item.optString("example").ifBlank {
                            item.optJSONArray("captions")?.optJSONObject(0)?.optString("content").orEmpty()
                        },
                        licenseSource = item.optString("licenseSource").ifBlank { "用户私有词库" }
                    )
                }
            }
        }
        return DeckImportResult(items.distinctBy { TextTools.normalizeWord(it.word) }, errors)
    }

    fun parseTypeWordsTxt(text: String): DeckImportResult {
        val errors = mutableListOf<String>()
        val items = text.lines().mapIndexedNotNull { index, line ->
            val clean = line.trim()
            if (clean.isBlank()) return@mapIndexedNotNull null
            val parts = clean.split('\t', '|', ',', limit = 3).map { it.trim() }
            val word = parts.getOrNull(0).orEmpty()
            if (word.isBlank()) {
                errors += "第 ${index + 1} 行缺少单词"
                null
            } else {
                DeckImportItem(
                    word = word,
                    definition = parts.getOrNull(1).orEmpty(),
                    example = parts.getOrNull(2).orEmpty(),
                    licenseSource = "用户私有词库"
                )
            }
        }
        return DeckImportResult(items.distinctBy { TextTools.normalizeWord(it.word) }, errors)
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        line.forEach { char ->
            when (char) {
                '"' -> inQuote = !inQuote
                ',' -> if (inQuote) current.append(char) else {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result += current.toString()
        return result
    }

    private fun List<String>.valueAt(index: Map<String, Int>, key: String): String =
        index[key]?.let { getOrNull(it) }.orEmpty().trim()

    private fun JSONObject.optList(key: String): List<String> {
        val value = opt(key) ?: return emptyList()
        return when (value) {
            is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf { item -> item.isNotBlank() } }
            is String -> value.split(separators).map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
}
