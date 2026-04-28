package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudyPathEngineTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = day * 42

    @Test
    fun generalPathUsesLearningScriptInsteadOfSixFlatTasks() {
        val plan = basePlan(contents = listOf(content("video-1", SourceType.VIDEO)))

        val path = StudyPathEngine.buildTodayPath(
            goalMode = LearningGoalMode.GENERAL,
            plan = plan,
            contents = listOf(content("video-1", SourceType.VIDEO)),
            words = listOf(word("spark", LearningWordStatus.NEW_WORD)),
            attempts = emptyList(),
            now = now
        )

        assertEquals(7, path.steps.size)
        assertEquals(StudyPathStage.VOCAB_WARMUP, path.steps.first().stage)
        assertTrue(path.deckStages.contains(VocabularyDeckStage.CORE_3500))
        assertTrue(path.deckStages.contains(VocabularyDeckStage.CORE_PHRASES_1200))
        assertFalse(path.deckStages.contains(VocabularyDeckStage.IELTS_TOPICS))
        assertFalse(path.deckStages.contains(VocabularyDeckStage.TOEFL_ACADEMIC))
        assertNotNull(path.steps.firstOrNull { it.stage == StudyPathStage.MATERIAL_INTENSIVE_LISTENING }?.contentId)
    }

    @Test
    fun ieltsPathPrioritizesTopicPacksAndOutputPhrases() {
        val path = StudyPathEngine.buildTodayPath(
            goalMode = LearningGoalMode.IELTS,
            plan = basePlan(contents = listOf(content("video-1", SourceType.VIDEO))),
            contents = listOf(content("video-1", SourceType.VIDEO)),
            words = emptyList(),
            attempts = emptyList(),
            now = now
        )

        assertTrue(path.deckStages.contains(VocabularyDeckStage.AWL_570))
        assertTrue(path.deckStages.contains(VocabularyDeckStage.IELTS_TOPICS))
        assertFalse(path.deckStages.contains(VocabularyDeckStage.TOEFL_ACADEMIC))
        val phraseStep = path.steps.single { it.stage == StudyPathStage.PHRASE_INPUT }
        assertTrue(phraseStep.highlights.any { it.contains("perspective") || it.contains("extent") })
    }

    @Test
    fun toeflPathPrioritizesAcademicPacksAndLecturePhrases() {
        val path = StudyPathEngine.buildTodayPath(
            goalMode = LearningGoalMode.TOEFL,
            plan = basePlan(contents = listOf(content("audio-1", SourceType.AUDIO))),
            contents = listOf(content("audio-1", SourceType.AUDIO)),
            words = emptyList(),
            attempts = emptyList(),
            now = now
        )

        assertTrue(path.deckStages.contains(VocabularyDeckStage.AWL_570))
        assertTrue(path.deckStages.contains(VocabularyDeckStage.TOEFL_ACADEMIC))
        assertFalse(path.deckStages.contains(VocabularyDeckStage.IELTS_TOPICS))
        val phraseStep = path.steps.single { it.stage == StudyPathStage.PHRASE_INPUT }
        assertTrue(phraseStep.highlights.any { it.contains("professor") || it.contains("example") })
    }

    @Test
    fun subtitleWordsStayManualAndAreNotCreatedFromImportedContent() {
        val path = StudyPathEngine.buildTodayPath(
            goalMode = LearningGoalMode.GENERAL,
            plan = basePlan(contents = listOf(content("doc-1", SourceType.DOCUMENT))),
            contents = listOf(
                content("doc-1", SourceType.DOCUMENT).copy(
                    originalText = "This document contains several words that should not auto-save."
                )
            ),
            words = emptyList(),
            attempts = emptyList(),
            now = now
        )

        val captureStep = path.steps.single { it.stage == StudyPathStage.MANUAL_WORD_CAPTURE }
        assertEquals(0, captureStep.progressValue)
        assertFalse(captureStep.completed)
        assertTrue(captureStep.subtitle.contains("点选字幕词"))
    }

    @Test
    fun completedPathAttemptRemovesStepFromNextAction() {
        val attempts = listOf(
            StudyAttempt(
                id = "path-core",
                taskType = StudyTaskType.NEW_WORDS,
                result = "path:${StudyPathStage.CORE_VOCAB.name}",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "path-phrases",
                taskType = StudyTaskType.SPELLING,
                result = "path:${StudyPathStage.PHRASE_INPUT.name}",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            )
        )

        val path = StudyPathEngine.buildTodayPath(
            goalMode = LearningGoalMode.IELTS,
            plan = basePlan(contents = listOf(content("video-1", SourceType.VIDEO))),
            contents = listOf(content("video-1", SourceType.VIDEO)),
            words = emptyList(),
            attempts = attempts,
            now = now
        )

        assertTrue(path.steps.single { it.stage == StudyPathStage.CORE_VOCAB }.completed)
        assertTrue(path.steps.single { it.stage == StudyPathStage.PHRASE_INPUT }.completed)
        assertFalse(path.nextStep?.stage == StudyPathStage.CORE_VOCAB)
    }

    private fun basePlan(contents: List<ImportedContent>): DailyLearningPlan =
        LearningPlanEngine.buildTodayPlan(
            words = listOf(
                word("review", LearningWordStatus.LEARNING, dueAt = now - 1_000L),
                word("spark", LearningWordStatus.NEW_WORD, createdAt = now - day)
            ),
            contents = contents,
            attempts = emptyList(),
            now = now
        )

    private fun word(
        normalized: String,
        status: LearningWordStatus,
        createdAt: Long = now,
        dueAt: Long = now
    ): LearningWord =
        LearningWord(
            id = "lw-$normalized",
            word = normalized,
            normalized = normalized,
            chineseDefinition = "$normalized definition",
            status = status,
            contexts = listOf(
                SourceContext(
                    sourceItemId = "content-$normalized",
                    sourceTitle = "Demo",
                    captionId = "caption-$normalized",
                    captionStartMs = 0L,
                    captionEndMs = 1000L,
                    englishSentence = "$normalized in context.",
                    chineseSentence = "",
                    sourceType = LearningSourceType.VIDEO,
                    createdAt = createdAt
                )
            ),
            createdAt = createdAt,
            updatedAt = createdAt,
            dueAt = dueAt
        )

    private fun content(id: String, kind: SourceType): ImportedContent =
        ImportedContent(
            id = id,
            title = id,
            kind = kind,
            durationMs = 120_000L,
            status = ImportProcessingStatus.READY_TO_LEARN,
            wordCount = 120,
            createdAt = now - day
        )
}
