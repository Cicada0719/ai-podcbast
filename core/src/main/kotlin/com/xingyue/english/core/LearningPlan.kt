package com.xingyue.english.core

val ScientificStudyTaskOrder: List<StudyTaskType> = listOf(
    StudyTaskType.DUE_REVIEW,
    StudyTaskType.MISTAKES,
    StudyTaskType.SPELLING,
    StudyTaskType.NEW_WORDS,
    StudyTaskType.LISTENING_REPEAT,
    StudyTaskType.HUNDRED_LS
)

data class TodayStudyProgress(
    val completedCount: Int,
    val remainingCount: Int
) {
    val totalCount: Int
        get() = completedCount + remainingCount

    val fraction: Float
        get() = if (totalCount == 0) 1f else (completedCount.toFloat() / totalCount).coerceIn(0f, 1f)
}

data class DailyLearningPlan(
    val generatedAt: Long,
    val newWords: List<LearningWord>,
    val dueReviews: List<LearningWord>,
    val mistakes: List<LearningWord>,
    val spellingWords: List<LearningWord>,
    val listeningItems: List<ImportedContent>,
    val hundredLsItems: List<ImportedContent>,
    val newWordTasks: List<StudyTaskItem>,
    val dueReviewTasks: List<StudyTaskItem>,
    val spellingTasks: List<StudyTaskItem>,
    val listeningRepeatTasks: List<StudyTaskItem>,
    val hundredLsTasks: List<StudyTaskItem>,
    val mistakeTasks: List<StudyTaskItem>,
    val totalMinutesTarget: Int = 30
) {
    val taskQueues: Map<StudyTaskType, List<StudyTaskItem>>
        get() = linkedMapOf(
            StudyTaskType.DUE_REVIEW to dueReviewTasks,
            StudyTaskType.MISTAKES to mistakeTasks,
            StudyTaskType.SPELLING to spellingTasks,
            StudyTaskType.NEW_WORDS to newWordTasks,
            StudyTaskType.LISTENING_REPEAT to listeningRepeatTasks,
            StudyTaskType.HUNDRED_LS to hundredLsTasks
        )

    val allTasks: List<StudyTaskItem>
        get() = taskQueues.values.flatten()

    val totalTaskCount: Int
        get() = allTasks.size

    fun queue(type: StudyTaskType): List<StudyTaskItem> =
        taskQueues[type].orEmpty()
}

typealias LearningPlan = DailyLearningPlan

object LearningPlanEngine {
    fun buildTodayPlan(
        words: List<LearningWord>,
        contents: List<ImportedContent>,
        attempts: List<StudyAttempt>,
        now: Long = System.currentTimeMillis()
    ): DailyLearningPlan {
        val attemptedWordsToday = StudyTaskType.entries.associateWith { taskType ->
            attempts.wordsAttemptedToday(taskType, now)
        }
        val attemptedContentsToday = StudyTaskType.entries.associateWith { taskType ->
            attempts.contentsAttemptedToday(taskType, now)
        }

        val due = words
            .filter { it.status != LearningWordStatus.NEW_WORD && it.status != LearningWordStatus.IGNORED && it.dueAt <= now }
            .filter { it.normalized !in attemptedWordsToday.getValue(StudyTaskType.DUE_REVIEW) }
            .sortedWith(compareBy<LearningWord> { it.dueAt }.thenByDescending { it.occurrenceCount })
            .take(30)
        val dueKeys = due.map { it.normalized }.toSet()

        val newWords = words
            .filter { it.status == LearningWordStatus.NEW_WORD }
            .filter { it.normalized !in attemptedWordsToday.getValue(StudyTaskType.NEW_WORDS) }
            .sortedByDescending { it.createdAt }
            .take(18)

        val mistakes = mistakeWords(words, attempts)
            .filter { it.status != LearningWordStatus.IGNORED }
            .filter { it.normalized !in dueKeys }
            .filter { it.normalized !in attemptedWordsToday.getValue(StudyTaskType.MISTAKES) }
            .take(12)

        val spellingWords = (mistakes + due)
            .distinctBy { it.normalized }
            .filter { it.normalized !in attemptedWordsToday.getValue(StudyTaskType.SPELLING) }
            .take(20)

        val listenable = contents
            .filter { it.kind == SourceType.VIDEO || it.kind == SourceType.AUDIO }
            .filter { it.status == ImportProcessingStatus.READY_TO_LEARN }

        val listening = listenable
            .filter { it.id !in attemptedContentsToday.getValue(StudyTaskType.LISTENING_REPEAT) }
            .sortedWith(compareByDescending<ImportedContent> { it.favorite }.thenByDescending { it.createdAt })
            .take(3)

        val hundredLs = listenable
            .filter { it.id !in attemptedContentsToday.getValue(StudyTaskType.HUNDRED_LS) }
            .sortedWith(compareByDescending<ImportedContent> { it.favorite }.thenByDescending { it.durationMs }.thenByDescending { it.createdAt })
            .take(2)

        return DailyLearningPlan(
            generatedAt = now,
            newWords = newWords,
            dueReviews = due,
            mistakes = mistakes,
            spellingWords = spellingWords,
            listeningItems = listening,
            hundredLsItems = hundredLs,
            newWordTasks = newWords.mapIndexed { index, word -> word.toTaskItem(StudyTaskType.NEW_WORDS, index, now) },
            dueReviewTasks = due.mapIndexed { index, word -> word.toTaskItem(StudyTaskType.DUE_REVIEW, index, now) },
            spellingTasks = spellingWords.mapIndexed { index, word -> word.toTaskItem(StudyTaskType.SPELLING, index, now) },
            listeningRepeatTasks = listening.mapIndexed { index, content -> content.toTaskItem(StudyTaskType.LISTENING_REPEAT, index, now) },
            hundredLsTasks = hundredLs.mapIndexed { index, content -> content.toTaskItem(StudyTaskType.HUNDRED_LS, index, now) },
            mistakeTasks = mistakes.mapIndexed { index, word -> word.toTaskItem(StudyTaskType.MISTAKES, index, now) }
        )
    }

    fun progressToday(
        plan: DailyLearningPlan,
        attempts: List<StudyAttempt>,
        now: Long = System.currentTimeMillis()
    ): TodayStudyProgress {
        val completed = completedTodayCount(attempts, now)
        return TodayStudyProgress(
            completedCount = completed,
            remainingCount = (plan.totalTaskCount - completed).coerceAtLeast(0)
        )
    }

    fun completedTodayCount(
        attempts: List<StudyAttempt>,
        now: Long = System.currentTimeMillis()
    ): Int =
        attempts.asSequence()
            .filter { it.countsAsCompleted && isSameDay(it.completedOrClosedAt, now) }
            .distinctBy { it.progressIdentity() }
            .count()

    fun streakDays(attempts: List<StudyAttempt>, now: Long = System.currentTimeMillis()): Int {
        val days = attempts.asSequence()
            .filter { it.countsAsAttempt }
            .map { dayIndex(it.completedOrClosedAt) }
            .toSet()
        if (days.isEmpty()) return 0

        var cursor = dayIndex(now)
        var streak = 0
        while (cursor in days) {
            streak += 1
            cursor -= 1
        }
        return streak
    }

    private fun mistakeWords(words: List<LearningWord>, attempts: List<StudyAttempt>): List<LearningWord> {
        val wordsByNormalized = words.associateBy { it.normalized }
        val mistakeKeys = attempts.asSequence()
            .filter { it.countsAsAttempt && it.isMistake && it.wordNormalized.isNotBlank() }
            .sortedByDescending { it.completedOrClosedAt }
            .map { it.wordNormalized }
            .distinct()
            .toList()
        val fromAttempts = mistakeKeys.mapNotNull { wordsByNormalized[it] }
        val dueFallback = words
            .filter { it.status == LearningWordStatus.DUE }
            .sortedByDescending { it.updatedAt }
        return (fromAttempts + dueFallback).distinctBy { it.normalized }
    }

    private fun List<StudyAttempt>.wordsAttemptedToday(taskType: StudyTaskType, now: Long): Set<String> =
        asSequence()
            .filter { it.taskType == taskType && it.countsAsAttempt && isSameDay(it.completedOrClosedAt, now) }
            .map { it.wordNormalized }
            .filter { it.isNotBlank() }
            .toSet()

    private fun List<StudyAttempt>.contentsAttemptedToday(taskType: StudyTaskType, now: Long): Set<String> =
        asSequence()
            .filter { it.taskType == taskType && it.countsAsAttempt && isSameDay(it.completedOrClosedAt, now) }
            .map { it.contentId }
            .filter { it.isNotBlank() }
            .toSet()

    private fun StudyAttempt.progressIdentity(): String =
        when {
            taskItemId.isNotBlank() -> "task:$taskItemId"
            wordNormalized.isNotBlank() -> "word:${taskType.name}:$wordNormalized"
            contentId.isNotBlank() -> "content:${taskType.name}:$contentId"
            else -> id
        }

    private fun LearningWord.toTaskItem(taskType: StudyTaskType, index: Int, now: Long): StudyTaskItem {
        val context = contexts.maxByOrNull { it.createdAt }
        return StudyTaskItem(
            id = "daily-${dayIndex(now)}-${taskType.name.lowercase()}-$normalized",
            taskType = taskType,
            title = word,
            subtitle = chineseDefinition.ifBlank { context?.englishSentence.orEmpty() },
            contentId = context?.sourceItemId.orEmpty(),
            wordNormalized = normalized,
            sourceType = context?.sourceType,
            priority = index + 1,
            dueAt = dueAt,
            estimatedMinutes = when (taskType) {
                StudyTaskType.SPELLING -> 2
                StudyTaskType.LISTENING_REPEAT,
                StudyTaskType.HUNDRED_LS -> 5
                else -> 1
            },
            createdAt = now
        )
    }

    private fun ImportedContent.toTaskItem(taskType: StudyTaskType, index: Int, now: Long): StudyTaskItem =
        StudyTaskItem(
            id = "daily-${dayIndex(now)}-${taskType.name.lowercase()}-$id",
            taskType = taskType,
            title = title,
            subtitle = statusMessage,
            contentId = id,
            sourceType = kind.toLearningSourceType(),
            priority = index + 1,
            dueAt = now,
            estimatedMinutes = if (taskType == StudyTaskType.HUNDRED_LS) 15 else 6,
            createdAt = now
        )

    private fun SourceType.toLearningSourceType(): LearningSourceType =
        when (this) {
            SourceType.VIDEO -> LearningSourceType.VIDEO
            SourceType.AUDIO -> LearningSourceType.AUDIO
            SourceType.SUBTITLE -> LearningSourceType.SUBTITLE
            SourceType.DOCUMENT -> LearningSourceType.DOCUMENT
        }

    private fun isSameDay(left: Long, right: Long): Boolean =
        dayIndex(left) == dayIndex(right)

    private fun dayIndex(timestamp: Long): Long =
        timestamp / (24L * 60L * 60L * 1000L)
}
