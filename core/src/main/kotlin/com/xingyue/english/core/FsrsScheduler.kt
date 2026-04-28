package com.xingyue.english.core

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ReviewScheduler {
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0

    // FSRS-inspired defaults. They keep the first review close to common
    // language-learning expectations while preserving stability/difficulty.
    private val weights = doubleArrayOf(
        0.4, 1.2, 3.5, 7.0,
        5.0, 1.2, 0.8, 0.001,
        1.6, 0.15, 1.0, 2.0,
        0.08, 0.35, 1.4, 0.2,
        2.4, 0.5, 0.6
    )

    fun review(
        word: LearningWord,
        previousState: FsrsCardState? = null,
        rating: ReviewRating,
        now: Long = System.currentTimeMillis()
    ): ReviewSessionResult {
        val normalized = word.normalized
        val state = previousState?.takeIf { it.reviewCount > 0 } ?: initialState(normalized, word)
        val elapsedDays = elapsedDays(state, word, now)
        val nextState = if (state.reviewCount == 0) {
            firstReview(normalized, rating, now)
        } else {
            repeatReview(state, rating, elapsedDays, now)
        }

        val nextStatus = when (rating) {
            ReviewRating.AGAIN -> LearningWordStatus.DUE
            ReviewRating.HARD -> LearningWordStatus.LEARNING
            ReviewRating.GOOD -> LearningWordStatus.FAMILIAR
            ReviewRating.EASY -> LearningWordStatus.MASTERED
        }
        val updatedWord = word.copy(
            status = nextStatus,
            updatedAt = now,
            dueAt = nextState.dueAt
        )
        val review = LearningReview(
            id = "review-${normalized}-$now",
            normalized = normalized,
            rating = rating,
            reviewedAt = now,
            nextDueAt = nextState.dueAt,
            stability = nextState.stability,
            difficulty = nextState.difficulty,
            elapsedDays = elapsedDays,
            scheduledDays = nextState.scheduledDays,
            reviewCount = nextState.reviewCount,
            lapses = nextState.lapses
        )
        return ReviewSessionResult(updatedWord, nextState, review)
    }

    fun isDue(state: FsrsCardState?, word: LearningWord, now: Long = System.currentTimeMillis()): Boolean =
        (state?.dueAt ?: word.dueAt) <= now

    private fun initialState(normalized: String, word: LearningWord): FsrsCardState =
        FsrsCardState(
            normalized = normalized,
            stability = 0.0,
            difficulty = 0.0,
            reviewCount = 0,
            lapses = 0,
            lastReviewAt = word.createdAt,
            scheduledDays = 0,
            dueAt = word.dueAt
        )

    private fun firstReview(normalized: String, rating: ReviewRating, now: Long): FsrsCardState {
        val grade = grade(rating)
        val stability = max(0.1, weights[grade - 1])
        val difficulty = constrainDifficulty(weights[4] - exp((grade - 1) * weights[5]) + 1.0)
        val scheduledDays = when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> 1
            ReviewRating.GOOD -> 3
            ReviewRating.EASY -> 7
        }
        return FsrsCardState(
            normalized = normalized,
            stability = stability,
            difficulty = difficulty,
            reviewCount = 1,
            lapses = if (rating == ReviewRating.AGAIN) 1 else 0,
            lastReviewAt = now,
            scheduledDays = scheduledDays,
            dueAt = now + scheduledDays * DAY_MS
        )
    }

    private fun repeatReview(
        state: FsrsCardState,
        rating: ReviewRating,
        elapsedDays: Int,
        now: Long
    ): FsrsCardState {
        val retrievability = retrievability(elapsedDays, state.stability)
        val nextDifficulty = nextDifficulty(state.difficulty, rating)
        val nextStability = when (rating) {
            ReviewRating.AGAIN -> nextForgetStability(nextDifficulty, state.stability, retrievability)
            ReviewRating.HARD -> nextRecallStability(nextDifficulty, state.stability, retrievability, hardPenalty = weights[15], easyBonus = 1.0)
            ReviewRating.GOOD -> nextRecallStability(nextDifficulty, state.stability, retrievability, hardPenalty = 1.0, easyBonus = 1.0)
            ReviewRating.EASY -> nextRecallStability(nextDifficulty, state.stability, retrievability, hardPenalty = 1.0, easyBonus = weights[16])
        }
        val scheduledDays = intervalFor(rating, nextStability)
        return state.copy(
            stability = nextStability,
            difficulty = nextDifficulty,
            reviewCount = state.reviewCount + 1,
            lapses = state.lapses + if (rating == ReviewRating.AGAIN) 1 else 0,
            lastReviewAt = now,
            scheduledDays = scheduledDays,
            dueAt = now + scheduledDays * DAY_MS
        )
    }

    private fun elapsedDays(state: FsrsCardState, word: LearningWord, now: Long): Int {
        val last = max(state.lastReviewAt, word.updatedAt.takeIf { state.reviewCount == 0 } ?: 0L)
        return max(0, ((now - last) / DAY_MS).toInt())
    }

    private fun retrievability(elapsedDays: Int, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)
    }

    private fun nextDifficulty(previous: Double, rating: ReviewRating): Double {
        val grade = grade(rating)
        val delta = -weights[6] * (grade - 3)
        val meanReversion = weights[7] * initialEasyDifficulty() + (1 - weights[7]) * (previous + delta)
        return constrainDifficulty(meanReversion)
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        hardPenalty: Double,
        easyBonus: Double
    ): Double {
        val growth = exp(weights[8]) *
            (11.0 - difficulty) *
            stability.pow(-weights[9]) *
            (exp((1.0 - retrievability) * weights[10]) - 1.0) *
            hardPenalty *
            easyBonus
        return max(0.1, stability * (1.0 + growth))
    }

    private fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        val next = weights[11] *
            difficulty.pow(-weights[12]) *
            ((stability + 1.0).pow(weights[13]) - 1.0) *
            exp((1.0 - retrievability) * weights[14])
        return max(0.1, min(next, stability))
    }

    private fun intervalFor(rating: ReviewRating, stability: Double): Int =
        when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> max(1, ceil(stability * 0.6).toInt())
            ReviewRating.GOOD -> max(1, ceil(stability).toInt())
            ReviewRating.EASY -> max(2, ceil(stability * 1.4).toInt())
        }

    private fun initialEasyDifficulty(): Double =
        constrainDifficulty(weights[4] - exp(3 * weights[5]) + 1.0)

    private fun grade(rating: ReviewRating): Int =
        when (rating) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> 2
            ReviewRating.GOOD -> 3
            ReviewRating.EASY -> 4
        }

    private fun constrainDifficulty(value: Double): Double = min(10.0, max(1.0, value))
}

object FsrsScheduler {
    fun review(word: LearningWord, rating: ReviewRating, now: Long = System.currentTimeMillis()): Pair<LearningWord, LearningReview> {
        val result = ReviewScheduler.review(word = word, rating = rating, now = now)
        return result.word to result.review
    }
}
