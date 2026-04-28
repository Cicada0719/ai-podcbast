package com.xingyue.english.core

enum class WordLearningPhase {
    RECOGNIZE,
    LISTEN,
    EXAMPLE,
    SPELLING_RECALL,
    FEEDBACK
}

data class TodayVocabularyDashboardUiState(
    val goalMode: LearningGoalMode,
    val newWordTarget: Int,
    val dueReviewCount: Int,
    val mistakeCount: Int,
    val subtitleWordCount: Int,
    val streakDays: Int,
    val primaryActionLabel: String,
    val nextQueueType: StudyTaskType,
    val message: String
)

data class SourceExampleUiState(
    val sentence: String,
    val translation: String = "",
    val sourceTitle: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val jumpTarget: SourceJumpTarget? = null
)

data class WordCardUiState(
    val normalized: String,
    val word: String,
    val phonetic: String,
    val chineseDefinition: String,
    val englishDefinition: String,
    val rootAffixHint: String,
    val collocations: List<String>,
    val examples: List<SourceExampleUiState>,
    val fromPersonalSubtitle: Boolean,
    val ttsStatusLabel: String
)

data class VocabularySessionUiState(
    val goalMode: LearningGoalMode,
    val sessionSize: Int,
    val currentIndex: Int,
    val cards: List<WordCardUiState>,
    val phases: List<WordLearningPhase> = defaultWordLearningPhases,
    val completedCount: Int = 0,
    val correctCount: Int = 0
) {
    val totalCount: Int
        get() = cards.size

    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount).coerceIn(0f, 1f)

    val completed: Boolean
        get() = totalCount > 0 && completedCount >= totalCount
}

data class ReviewFeedbackUiState(
    val rating: ReviewRating,
    val label: String,
    val description: String,
    val nextReviewHint: String,
    val returnsToMistakesToday: Boolean
)

private val defaultWordLearningPhases = listOf(
    WordLearningPhase.RECOGNIZE,
    WordLearningPhase.LISTEN,
    WordLearningPhase.EXAMPLE,
    WordLearningPhase.SPELLING_RECALL,
    WordLearningPhase.FEEDBACK
)

object VocabularyCoursePlanner {
    val allowedSessionSizes: Set<Int> = setOf(5, 10, 20)

    fun normalizeSessionSize(requested: Int): Int =
        if (requested in allowedSessionSizes) requested else 10

    fun buildDashboard(
        goalMode: LearningGoalMode,
        plan: DailyLearningPlan,
        words: List<LearningWord>,
        attempts: List<StudyAttempt>,
        preferredSessionSize: Int = 10,
        now: Long = System.currentTimeMillis()
    ): TodayVocabularyDashboardUiState {
        val sessionSize = normalizeSessionSize(preferredSessionSize)
        val nextType = when {
            plan.dueReviews.isNotEmpty() -> StudyTaskType.DUE_REVIEW
            plan.mistakes.isNotEmpty() -> StudyTaskType.MISTAKES
            else -> StudyTaskType.NEW_WORDS
        }
        val action = when (nextType) {
            StudyTaskType.DUE_REVIEW -> "开始复习"
            StudyTaskType.MISTAKES -> "处理错词"
            else -> "背 $sessionSize 个新词"
        }
        val subtitleWords = words.count { word ->
            word.contexts.any { it.sourceType in setOf(LearningSourceType.VIDEO, LearningSourceType.AUDIO, LearningSourceType.SUBTITLE, LearningSourceType.DOCUMENT) }
        }
        return TodayVocabularyDashboardUiState(
            goalMode = goalMode,
            newWordTarget = sessionSize,
            dueReviewCount = plan.dueReviews.size,
            mistakeCount = plan.mistakes.size,
            subtitleWordCount = subtitleWords,
            streakDays = LearningPlanEngine.streakDays(attempts),
            primaryActionLabel = action,
            nextQueueType = nextType,
            message = when (nextType) {
                StudyTaskType.DUE_REVIEW -> "先清到期复习，避免遗忘曲线堆积。"
                StudyTaskType.MISTAKES -> "先把昨天和本组错词拉回来，正确后再回普通队列。"
                else -> "今天先背一小组，再做拼写回忆和熟悉度反馈。"
            }
        )
    }

    fun buildSession(
        goalMode: LearningGoalMode,
        words: List<LearningWord>,
        preferredSessionSize: Int = 10,
        sourceItems: List<LexicalItem> = BuiltInStudyLexicon.lexicalItems
    ): VocabularySessionUiState {
        val size = normalizeSessionSize(preferredSessionSize)
        val cards = buildWordCards(goalMode, words, size, sourceItems)
        return VocabularySessionUiState(
            goalMode = goalMode,
            sessionSize = size,
            currentIndex = 0,
            cards = cards
        )
    }

    fun buildWordCards(
        goalMode: LearningGoalMode,
        words: List<LearningWord>,
        limit: Int = 10,
        sourceItems: List<LexicalItem> = BuiltInStudyLexicon.lexicalItems
    ): List<WordCardUiState> {
        val allItems = sourceItems.ifEmpty { BuiltInStudyLexicon.lexicalItems }
        val savedCards = words
            .filter { it.status != LearningWordStatus.IGNORED }
            .sortedWith(compareBy<LearningWord> { it.dueAt }.thenByDescending { it.contexts.size }.thenBy { it.normalized })
            .map { it.toWordCard() }
        val savedKeys = savedCards.map { it.normalized }.toSet()
        val sourceCards = allItems
            .filter { item ->
                item.normalized !in savedKeys &&
                    (item.stage in StudyPathEngine.goalDeckStages(goalMode) || item.stage == VocabularyDeckStage.CORE_3500)
            }
            .sortedWith(compareBy<LexicalItem> { it.stage.ordinal }.thenBy { it.difficulty }.thenBy { it.word })
            .map { it.toWordCard() }
        return (savedCards + sourceCards).take(limit.coerceAtLeast(1))
    }

    fun orderedReviewQueue(plan: DailyLearningPlan): List<StudyTaskType> =
        buildList {
            if (plan.dueReviews.isNotEmpty()) add(StudyTaskType.DUE_REVIEW)
            if (plan.mistakes.isNotEmpty()) add(StudyTaskType.MISTAKES)
            if (plan.spellingWords.isNotEmpty()) add(StudyTaskType.SPELLING)
            if (plan.newWords.isNotEmpty()) add(StudyTaskType.NEW_WORDS)
        }

    fun feedbackUi(rating: ReviewRating): ReviewFeedbackUiState =
        when (rating) {
            ReviewRating.AGAIN -> ReviewFeedbackUiState(
                rating = rating,
                label = "不会",
                description = "今天错词回流里再见一次。",
                nextReviewHint = "今天再次复习",
                returnsToMistakesToday = true
            )
            ReviewRating.HARD -> ReviewFeedbackUiState(
                rating = rating,
                label = "模糊",
                description = "能认出一点，但还需要短间隔巩固。",
                nextReviewHint = "短间隔复习",
                returnsToMistakesToday = true
            )
            ReviewRating.GOOD -> ReviewFeedbackUiState(
                rating = rating,
                label = "认识",
                description = "进入正常复习曲线。",
                nextReviewHint = "按 FSRS 计算",
                returnsToMistakesToday = false
            )
            ReviewRating.EASY -> ReviewFeedbackUiState(
                rating = rating,
                label = "太简单",
                description = "延后复习，避免浪费今日注意力。",
                nextReviewHint = "更长间隔后复习",
                returnsToMistakesToday = false
            )
        }

    fun nextPhase(current: WordLearningPhase): WordLearningPhase? {
        val index = defaultWordLearningPhases.indexOf(current)
        return defaultWordLearningPhases.getOrNull(index + 1)
    }

    private fun LearningWord.toWordCard(): WordCardUiState {
        val examples = contexts
            .filter { it.englishSentence.isNotBlank() || it.chineseSentence.isNotBlank() }
            .take(3)
            .map {
                SourceExampleUiState(
                    sentence = it.englishSentence,
                    translation = it.chineseSentence,
                    sourceTitle = it.sourceTitle,
                    startMs = it.captionStartMs,
                    endMs = it.captionEndMs,
                    jumpTarget = it.jumpTarget()
                )
            }
        return WordCardUiState(
            normalized = normalized,
            word = word,
            phonetic = phonetic,
            chineseDefinition = chineseDefinition.ifBlank { "待补充释义" },
            englishDefinition = "Personal word collected from your materials.",
            rootAffixHint = notes.takeIf { it.isNotBlank() } ?: inferRootAffix(word),
            collocations = examples.mapNotNull { it.sentence.extractCollocation(word) }.distinct().take(3),
            examples = examples.ifEmpty {
                listOf(SourceExampleUiState(sentence = "Try to use $word in one sentence today."))
            },
            fromPersonalSubtitle = contexts.isNotEmpty(),
            ttsStatusLabel = "发音：云端/缓存优先"
        )
    }

    private fun LexicalItem.toWordCard(): WordCardUiState =
        WordCardUiState(
            normalized = normalized,
            word = word,
            phonetic = phonetic,
            chineseDefinition = definition,
            englishDefinition = tags.joinToString(prefix = "Deck: ", separator = " / ").ifBlank { "Deck word" },
            rootAffixHint = inferRootAffix(word),
            collocations = phrases,
            examples = listOf(SourceExampleUiState(sentence = example.ifBlank { "Use $word in a real sentence." })),
            fromPersonalSubtitle = false,
            ttsStatusLabel = "发音：云端/缓存优先"
        )

    private fun inferRootAffix(word: String): String {
        val lower = word.lowercase()
        return when {
            lower.endsWith("tion") -> "-tion: 名词后缀，常表示动作或结果"
            lower.endsWith("able") -> "-able: 形容词后缀，表示可以被..."
            lower.endsWith("ive") -> "-ive: 形容词后缀，表示具有...性质"
            lower.startsWith("un") -> "un-: 否定前缀"
            lower.startsWith("re") -> "re-: 重复、再次"
            else -> "从词形、搭配和例句一起记，不孤立背中文。"
        }
    }

    private fun String.extractCollocation(word: String): String? {
        if (isBlank()) return null
        val tokens = split(Regex("\\s+")).map { it.trim(',', '.', '!', '?', ';', ':', '"', '\'') }
        val index = tokens.indexOfFirst { it.equals(word, ignoreCase = true) }
        if (index < 0) return null
        val from = (index - 1).coerceAtLeast(0)
        val to = (index + 2).coerceAtMost(tokens.size)
        return tokens.subList(from, to).joinToString(" ").takeIf { it.isNotBlank() }
    }
}
