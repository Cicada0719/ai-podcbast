package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningPlanEngineTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = day * 20

    @Test
    fun buildsSixTaskQueuesFromWordsContentsAndAttempts() {
        val words = listOf(
            word("spark", LearningWordStatus.NEW_WORD, createdAt = now - day),
            word("review", LearningWordStatus.LEARNING, dueAt = now - 1_000L),
            word("fragile", LearningWordStatus.LEARNING, dueAt = now + day),
            word("fallback", LearningWordStatus.DUE, dueAt = now - day)
        )
        val contents = listOf(
            content("video-1", SourceType.VIDEO, favorite = true, durationMs = 120_000L),
            content("audio-1", SourceType.AUDIO, durationMs = 60_000L)
        )
        val attempts = listOf(
            StudyAttempt(
                id = "attempt-fragile",
                taskType = StudyTaskType.SPELLING,
                wordNormalized = "fragile",
                result = "wrong",
                score = 0,
                status = StudyAttemptStatus.FAILED,
                createdAt = now - day,
                completedAt = now - day
            )
        )

        val plan = LearningPlanEngine.buildTodayPlan(words, contents, attempts, now)

        assertEquals(ScientificStudyTaskOrder, plan.taskQueues.keys.toList())
        assertEquals("spark", plan.queue(StudyTaskType.NEW_WORDS).single().wordNormalized)
        assertTrue(plan.queue(StudyTaskType.DUE_REVIEW).any { it.wordNormalized == "review" })
        assertFalse(plan.queue(StudyTaskType.DUE_REVIEW).any { it.wordNormalized == "spark" })
        assertTrue(plan.queue(StudyTaskType.MISTAKES).any { it.wordNormalized == "fragile" })
        assertTrue(plan.queue(StudyTaskType.SPELLING).any { it.wordNormalized == "fragile" })
        assertEquals("video-1", plan.queue(StudyTaskType.LISTENING_REPEAT).first().contentId)
        assertTrue(plan.queue(StudyTaskType.HUNDRED_LS).isNotEmpty())
        assertEquals(plan.taskQueues.values.sumOf { it.size }, plan.totalTaskCount)
    }

    @Test
    fun excludesSameDayAttemptsFromMatchingQueueOnly() {
        val words = listOf(
            word("spark", LearningWordStatus.NEW_WORD, createdAt = now - day),
            word("review", LearningWordStatus.LEARNING, dueAt = now - 1_000L)
        )
        val contents = listOf(content("video-1", SourceType.VIDEO))
        val attempts = listOf(
            StudyAttempt(
                id = "attempt-spark",
                taskType = StudyTaskType.NEW_WORDS,
                wordNormalized = "spark",
                result = "completed",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "attempt-video",
                taskType = StudyTaskType.LISTENING_REPEAT,
                contentId = "video-1",
                result = "completed",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "attempt-review",
                taskType = StudyTaskType.DUE_REVIEW,
                wordNormalized = "review",
                result = "GOOD",
                score = 85,
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            )
        )

        val plan = LearningPlanEngine.buildTodayPlan(words, contents, attempts, now)

        assertFalse(plan.queue(StudyTaskType.NEW_WORDS).any { it.wordNormalized == "spark" })
        assertFalse(plan.queue(StudyTaskType.DUE_REVIEW).any { it.wordNormalized == "review" })
        assertFalse(plan.queue(StudyTaskType.LISTENING_REPEAT).any { it.contentId == "video-1" })
        assertTrue(plan.queue(StudyTaskType.HUNDRED_LS).any { it.contentId == "video-1" })
    }

    @Test
    fun progressCountsUniqueCompletedAttemptsOnly() {
        val attempts = listOf(
            StudyAttempt(
                id = "completed-1",
                taskType = StudyTaskType.DUE_REVIEW,
                taskItemId = "task-review",
                wordNormalized = "review",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "completed-duplicate",
                taskType = StudyTaskType.DUE_REVIEW,
                taskItemId = "task-review",
                wordNormalized = "review",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "failed",
                taskType = StudyTaskType.SPELLING,
                taskItemId = "task-spelling",
                wordNormalized = "fragile",
                status = StudyAttemptStatus.FAILED,
                createdAt = now,
                completedAt = now
            ),
            StudyAttempt(
                id = "started",
                taskType = StudyTaskType.NEW_WORDS,
                taskItemId = "task-new",
                status = StudyAttemptStatus.STARTED,
                createdAt = now,
                completedAt = 0L
            ),
            StudyAttempt(
                id = "yesterday",
                taskType = StudyTaskType.HUNDRED_LS,
                taskItemId = "task-listen",
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now - day,
                completedAt = now - day
            )
        )

        assertEquals(1, LearningPlanEngine.completedTodayCount(attempts, now))
    }

    @Test
    fun progressTodaySubtractsCompletedAttemptsFromRemainingQueue() {
        val plan = LearningPlanEngine.buildTodayPlan(
            words = listOf(
                word("review", LearningWordStatus.LEARNING, dueAt = now - 1_000L),
                word("spark", LearningWordStatus.NEW_WORD, createdAt = now - day)
            ),
            contents = listOf(content("video-1", SourceType.VIDEO)),
            attempts = emptyList(),
            now = now
        )
        val task = plan.queue(StudyTaskType.DUE_REVIEW).first()
        val progress = LearningPlanEngine.progressToday(
            plan = plan,
            attempts = listOf(
                StudyAttempt(
                    id = "attempt-review",
                    taskType = task.taskType,
                    taskItemId = task.id,
                    wordNormalized = task.wordNormalized,
                    status = StudyAttemptStatus.COMPLETED,
                    createdAt = now,
                    completedAt = now
                )
            ),
            now = now
        )

        assertEquals(1, progress.completedCount)
        assertEquals((plan.totalTaskCount - 1).coerceAtLeast(0), progress.remainingCount)
        assertEquals(plan.totalTaskCount, progress.totalCount)
    }

    @Test
    fun streakIgnoresStartedAttempts() {
        val attempts = listOf(
            StudyAttempt(
                id = "started-today",
                taskType = StudyTaskType.NEW_WORDS,
                status = StudyAttemptStatus.STARTED,
                createdAt = now,
                completedAt = 0L
            ),
            StudyAttempt(
                id = "completed-yesterday",
                taskType = StudyTaskType.DUE_REVIEW,
                status = StudyAttemptStatus.COMPLETED,
                createdAt = now - day,
                completedAt = now - day
            )
        )

        assertEquals(0, LearningPlanEngine.streakDays(attempts, now))
        assertEquals(1, LearningPlanEngine.streakDays(attempts, now - day))
    }

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

    private fun content(
        id: String,
        kind: SourceType,
        favorite: Boolean = false,
        durationMs: Long = 0L
    ): ImportedContent =
        ImportedContent(
            id = id,
            title = id,
            kind = kind,
            durationMs = durationMs,
            status = ImportProcessingStatus.READY_TO_LEARN,
            favorite = favorite,
            createdAt = now - day
        )
}
