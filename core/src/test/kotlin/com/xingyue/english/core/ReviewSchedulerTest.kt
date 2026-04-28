package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewSchedulerTest {
    @Test
    fun fourRatingsCreateDifferentDueIntervals() {
        val word = LearningWord(id = "lw-wonder", word = "wonder", normalized = "wonder", createdAt = 1_000L, dueAt = 1_000L)
        val now = 10_000L

        val again = ReviewScheduler.review(word, rating = ReviewRating.AGAIN, now = now)
        val hard = ReviewScheduler.review(word, rating = ReviewRating.HARD, now = now)
        val good = ReviewScheduler.review(word, rating = ReviewRating.GOOD, now = now)
        val easy = ReviewScheduler.review(word, rating = ReviewRating.EASY, now = now)

        assertEquals(LearningWordStatus.DUE, again.word.status)
        assertEquals(LearningWordStatus.LEARNING, hard.word.status)
        assertEquals(LearningWordStatus.FAMILIAR, good.word.status)
        assertEquals(LearningWordStatus.MASTERED, easy.word.status)
        assertTrue(again.cardState.scheduledDays < hard.cardState.scheduledDays)
        assertTrue(hard.cardState.scheduledDays < good.cardState.scheduledDays)
        assertTrue(good.cardState.scheduledDays < easy.cardState.scheduledDays)
    }

    @Test
    fun repeatAgainIncrementsLapsesAndKeepsCardDue() {
        val day = 24L * 60L * 60L * 1000L
        val word = LearningWord(id = "lw-context", word = "context", normalized = "context", createdAt = 0L, dueAt = 0L)
        val first = ReviewScheduler.review(word, rating = ReviewRating.GOOD, now = day)

        val second = ReviewScheduler.review(first.word, previousState = first.cardState, rating = ReviewRating.AGAIN, now = day * 5)

        assertEquals(2, second.cardState.reviewCount)
        assertEquals(1, second.cardState.lapses)
        assertEquals(LearningWordStatus.DUE, second.word.status)
        assertEquals(second.review.nextDueAt, second.cardState.dueAt)
    }
}
