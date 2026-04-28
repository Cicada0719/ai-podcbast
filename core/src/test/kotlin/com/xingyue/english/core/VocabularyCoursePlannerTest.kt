package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VocabularyCoursePlannerTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = day * 90

    @Test
    fun sessionSizeDefaultsToTenAndAllowsFiveTenTwenty() {
        assertEquals(5, VocabularyCoursePlanner.normalizeSessionSize(5))
        assertEquals(10, VocabularyCoursePlanner.normalizeSessionSize(10))
        assertEquals(20, VocabularyCoursePlanner.normalizeSessionSize(20))
        assertEquals(10, VocabularyCoursePlanner.normalizeSessionSize(7))
    }

    @Test
    fun dashboardStartsWithDueReviewBeforeMistakesAndNewWords() {
        val words = listOf(
            word("review", LearningWordStatus.LEARNING, dueAt = now - 1_000L),
            word("mistake", LearningWordStatus.LEARNING, dueAt = now + day),
            word("fresh", LearningWordStatus.NEW_WORD, dueAt = now + day)
        )
        val attempts = listOf(
            StudyAttempt(
                id = "wrong",
                taskType = StudyTaskType.SPELLING,
                wordNormalized = "mistake",
                result = "wrong",
                score = 20,
                createdAt = now - 1_000L,
                completedAt = now - 1_000L
            )
        )
        val plan = LearningPlanEngine.buildTodayPlan(words, emptyList(), attempts, now)

        val dashboard = VocabularyCoursePlanner.buildDashboard(
            goalMode = LearningGoalMode.GENERAL,
            plan = plan,
            words = words,
            attempts = attempts,
            now = now
        )

        assertEquals(StudyTaskType.DUE_REVIEW, dashboard.nextQueueType)
        assertEquals("开始复习", dashboard.primaryActionLabel)
        assertEquals(listOf(StudyTaskType.DUE_REVIEW, StudyTaskType.MISTAKES, StudyTaskType.SPELLING, StudyTaskType.NEW_WORDS), VocabularyCoursePlanner.orderedReviewQueue(plan))
    }

    @Test
    fun fourFeedbackLabelsMatchUserLanguageAndMistakeReturn() {
        val again = VocabularyCoursePlanner.feedbackUi(ReviewRating.AGAIN)
        val hard = VocabularyCoursePlanner.feedbackUi(ReviewRating.HARD)
        val good = VocabularyCoursePlanner.feedbackUi(ReviewRating.GOOD)
        val easy = VocabularyCoursePlanner.feedbackUi(ReviewRating.EASY)

        assertEquals("不会", again.label)
        assertEquals("模糊", hard.label)
        assertEquals("认识", good.label)
        assertEquals("太简单", easy.label)
        assertTrue(again.returnsToMistakesToday)
        assertTrue(hard.returnsToMistakesToday)
        assertFalse(good.returnsToMistakesToday)
        assertFalse(easy.returnsToMistakesToday)
    }

    @Test
    fun personalSubtitleWordsKeepSourceExampleAndJumpTarget() {
        val personal = word(
            normalized = "struggle",
            status = LearningWordStatus.NEW_WORD,
            context = SourceContext(
                sourceItemId = "video-1",
                sourceTitle = "TED short",
                captionId = "cue-1",
                captionStartMs = 5_000L,
                captionEndMs = 9_000L,
                englishSentence = "Some learners struggle for years.",
                chineseSentence = "有些学习者会挣扎多年。",
                sourceType = LearningSourceType.VIDEO,
                createdAt = now
            )
        )

        val session = VocabularyCoursePlanner.buildSession(
            goalMode = LearningGoalMode.GENERAL,
            words = listOf(personal),
            preferredSessionSize = 10
        )

        val card = session.cards.first()
        assertEquals("struggle", card.normalized)
        assertTrue(card.fromPersonalSubtitle)
        assertEquals("TED short", card.examples.first().sourceTitle)
        assertEquals(5_000L, card.examples.first().jumpTarget?.startMs)
        assertTrue(session.phases.contains(WordLearningPhase.SPELLING_RECALL))
    }

    @Test
    fun examModesPullDifferentBuiltInCards() {
        val ielts = VocabularyCoursePlanner.buildWordCards(LearningGoalMode.IELTS, emptyList(), limit = 20)
        val toefl = VocabularyCoursePlanner.buildWordCards(LearningGoalMode.TOEFL, emptyList(), limit = 20)

        assertTrue(ielts.any { it.normalized == "urbanization" || it.normalized == "curriculum" })
        assertTrue(toefl.any { it.normalized == "hypothesis" || it.normalized == "artifact" })
    }

    @Test
    fun sessionUsesImportedLexicalItemsInsteadOfOnlyBuiltInExamples() {
        val names = listOf("sourcealpha", "sourcebravo", "sourcecharlie", "sourcedelta", "sourceecho", "sourcefoxtrot", "sourcegolf", "sourcehotel", "sourceindia", "sourcejuliet", "sourcekilo", "sourcelima")
        val imported = names.mapIndexed { index, word ->
            LexicalItem(
                id = "typewords-${index + 1}",
                word = word,
                definition = "imported definition ${index + 1}",
                deckId = "typewords",
                stage = VocabularyDeckStage.CORE_3500,
                difficulty = index + 1
            )
        }

        val session = VocabularyCoursePlanner.buildSession(
            goalMode = LearningGoalMode.GENERAL,
            words = emptyList(),
            preferredSessionSize = 10,
            sourceItems = imported
        )

        assertEquals(10, session.cards.size)
        assertEquals("sourcealpha", session.cards.first().normalized)
        assertTrue(session.cards.none { it.normalized == "benefit" })
    }

    private fun word(
        normalized: String,
        status: LearningWordStatus,
        dueAt: Long = now,
        context: SourceContext = SourceContext(
            sourceItemId = "content-$normalized",
            sourceTitle = "Demo",
            captionId = "caption-$normalized",
            captionStartMs = 0L,
            captionEndMs = 1000L,
            englishSentence = "$normalized in context.",
            chineseSentence = "",
            sourceType = LearningSourceType.VIDEO,
            createdAt = now
        )
    ): LearningWord =
        LearningWord(
            id = "lw-$normalized",
            word = normalized,
            normalized = normalized,
            chineseDefinition = "$normalized definition",
            status = status,
            contexts = listOf(context),
            createdAt = now,
            updatedAt = now,
            dueAt = dueAt
        )
}
