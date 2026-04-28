package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals

class StudyQueuePlannerTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = day * 120

    @Test
    fun lexicalQueueAdvancesAfterCompletedCoreSessions() {
        val names = (0 until 30).map { index ->
            "term${('a'.code + index / 26).toChar()}${('a'.code + index % 26).toChar()}"
        }
        val items = names.mapIndexed { index, name ->
            LexicalItem(
                id = "core-$index",
                word = name,
                definition = "definition $name",
                deckId = "core-3500",
                stage = VocabularyDeckStage.CORE_3500,
                difficulty = index
            )
        }
        val attempts = listOf(
            pathAttempt("a1", StudyPathStage.CORE_VOCAB),
            pathAttempt("a2", StudyPathStage.CORE_VOCAB)
        )

        val next = StudyQueuePlanner.nextLexicalQueue(
            goalMode = LearningGoalMode.GENERAL,
            deckStage = VocabularyDeckStage.CORE_3500,
            sourceItems = items,
            attempts = attempts,
            sessionSize = 10
        )

        assertEquals(names.drop(20), next.map { it.word })
    }

    @Test
    fun phraseQueueAdvancesAndRollsOverWhenPhrasesAreExhausted() {
        val chunks = (1..12).map { index ->
            PhraseChunk(
                id = "daily-$index",
                english = "Phrase $index",
                chinese = "短句 $index",
                useCase = PhraseUseCase.DAILY,
                deckId = "core-phrases-1200",
                keywords = listOf("phrase")
            )
        }
        val attempts = listOf(
            pathAttempt("p1", StudyPathStage.PHRASE_INPUT),
            pathAttempt("p2", StudyPathStage.PHRASE_INPUT)
        )

        val next = StudyQueuePlanner.nextPhraseQueue(LearningGoalMode.GENERAL, chunks, attempts, sessionSize = 5)
        val exhausted = StudyQueuePlanner.nextPhraseQueue(
            LearningGoalMode.GENERAL,
            chunks,
            attempts + pathAttempt("p3", StudyPathStage.PHRASE_INPUT),
            sessionSize = 5
        )

        assertEquals(listOf("Phrase 11", "Phrase 12"), next.map { it.english })
        assertEquals(listOf("Phrase 4", "Phrase 5", "Phrase 6", "Phrase 7", "Phrase 8"), exhausted.map { it.english })
    }

    @Test
    fun completedStageSessionsCanBeScopedToTodayForPathCompletion() {
        val attempts = listOf(
            pathAttempt("old", StudyPathStage.CORE_VOCAB, completedAt = now - day),
            pathAttempt("today", StudyPathStage.CORE_VOCAB, completedAt = now)
        )

        assertEquals(2, StudyQueuePlanner.completedStageSessions(attempts, StudyPathStage.CORE_VOCAB, LearningGoalMode.GENERAL))
        assertEquals(1, StudyQueuePlanner.completedStageSessions(attempts, StudyPathStage.CORE_VOCAB, LearningGoalMode.GENERAL, now))
    }

    private fun pathAttempt(
        id: String,
        stage: StudyPathStage,
        completedAt: Long = now
    ): StudyAttempt =
        StudyAttempt(
            id = id,
            taskType = if (stage == StudyPathStage.PHRASE_INPUT) StudyTaskType.SPELLING else StudyTaskType.NEW_WORDS,
            taskItemId = "path-${LearningGoalMode.GENERAL.name.lowercase()}-$id",
            result = "path:${stage.name}",
            status = StudyAttemptStatus.COMPLETED,
            createdAt = completedAt,
            completedAt = completedAt
        )
}
