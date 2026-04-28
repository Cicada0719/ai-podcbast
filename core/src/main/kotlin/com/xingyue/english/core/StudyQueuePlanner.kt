package com.xingyue.english.core

object StudyQueuePlanner {
    fun lexicalItemsFor(
        goalMode: LearningGoalMode,
        deckStage: VocabularyDeckStage? = null,
        sourceItems: List<LexicalItem> = BuiltInStudyLexicon.lexicalItems
    ): List<LexicalItem> {
        val allItems = sourceItems.ifEmpty { BuiltInStudyLexicon.lexicalItems }
        val stages = when (goalMode) {
            LearningGoalMode.GENERAL -> listOf(VocabularyDeckStage.CORE_3500, VocabularyDeckStage.ADVANCED_2000)
            LearningGoalMode.IELTS -> listOf(VocabularyDeckStage.IELTS_TOPICS, VocabularyDeckStage.AWL_570, VocabularyDeckStage.CORE_3500)
            LearningGoalMode.TOEFL -> listOf(VocabularyDeckStage.TOEFL_ACADEMIC, VocabularyDeckStage.AWL_570, VocabularyDeckStage.CORE_3500)
        }
        val preferred = deckStage?.let { stage -> allItems.filter { it.stage == stage } }.orEmpty()
        return (preferred + allItems.filter { it.stage in stages }).distinctBy { it.normalized }
    }

    fun phraseChunksFor(
        goalMode: LearningGoalMode,
        sourceChunks: List<PhraseChunk> = BuiltInStudyLexicon.phraseChunks
    ): List<PhraseChunk> {
        val allChunks = sourceChunks.ifEmpty { BuiltInStudyLexicon.phraseChunks }
        val orderedCases = when (goalMode) {
            LearningGoalMode.GENERAL -> listOf(PhraseUseCase.DAILY)
            LearningGoalMode.IELTS -> listOf(PhraseUseCase.IELTS_SPEAKING, PhraseUseCase.IELTS_WRITING, PhraseUseCase.DAILY)
            LearningGoalMode.TOEFL -> listOf(PhraseUseCase.TOEFL_LECTURE, PhraseUseCase.TOEFL_CAMPUS, PhraseUseCase.DAILY)
        }
        return orderedCases.flatMap { useCase -> allChunks.filter { it.useCase == useCase } }
    }

    fun nextLexicalQueue(
        goalMode: LearningGoalMode,
        deckStage: VocabularyDeckStage? = null,
        sourceItems: List<LexicalItem> = BuiltInStudyLexicon.lexicalItems,
        attempts: List<StudyAttempt> = emptyList(),
        sessionSize: Int = 10
    ): List<LexicalItem> {
        val normalizedSize = sessionSize.coerceAtLeast(1)
        val pool = lexicalItemsFor(goalMode, deckStage, sourceItems)
        val offset = completedStageSessions(attempts, StudyPathStage.CORE_VOCAB, goalMode) * normalizedSize
        return rollingWindow(pool, offset, normalizedSize)
    }

    fun nextPhraseQueue(
        goalMode: LearningGoalMode,
        sourceChunks: List<PhraseChunk> = BuiltInStudyLexicon.phraseChunks,
        attempts: List<StudyAttempt> = emptyList(),
        sessionSize: Int = 5
    ): List<PhraseChunk> {
        val normalizedSize = sessionSize.coerceAtLeast(1)
        val pool = phraseChunksFor(goalMode, sourceChunks)
        val offset = completedStageSessions(attempts, StudyPathStage.PHRASE_INPUT, goalMode) * normalizedSize
        return rollingWindow(pool, offset, normalizedSize)
    }

    fun completedStageSessions(
        attempts: List<StudyAttempt>,
        stage: StudyPathStage,
        goalMode: LearningGoalMode? = null,
        now: Long? = null
    ): Int =
        attempts.count { attempt ->
            attempt.countsAsCompleted &&
                attempt.result == "path:${stage.name}" &&
                (now == null || sameDay(attempt.completedOrClosedAt, now)) &&
                (goalMode == null || attempt.taskItemId.contains(goalMode.name.lowercase(), ignoreCase = true) || attempt.taskItemId.isBlank())
        }

    fun completedPracticeTargetCount(attempts: List<StudyAttempt>, mode: PracticeMode): Int =
        attempts
            .filter { it.countsAsCompleted && it.result.startsWith("practice-session:${mode.name}:") }
            .sumOf { attempt ->
                attempt.result.substringAfterLast('/').toIntOrNull()?.coerceAtLeast(1) ?: 1
            }

    private fun sameDay(left: Long, right: Long): Boolean =
        left / 86_400_000L == right / 86_400_000L

    private fun <T> rollingWindow(pool: List<T>, offset: Int, size: Int): List<T> {
        if (pool.isEmpty()) return emptyList()
        if (offset < pool.size) return pool.drop(offset).take(size)
        val start = offset.mod(pool.size)
        val ordered = pool.drop(start) + pool.take(start)
        return ordered.take(size.coerceAtMost(pool.size))
    }
}
