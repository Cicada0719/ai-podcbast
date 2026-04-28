package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeEnginesTest {
    @Test
    fun typingPracticeScoresCaseAndPunctuationTolerantly() {
        val prompt = PracticePrompt(id = "p1", expected = "Evidence-based method")

        val result = TypingPracticeEngine.evaluate(prompt, "evidence based method!", elapsedMs = 30_000)

        assertTrue(result.correct)
        assertEquals(100, result.accuracy)
        assertTrue(result.wpm >= 6)
    }

    @Test
    fun typingPracticeReportsTypos() {
        val prompt = PracticePrompt(id = "p1", expected = "sustainable")

        val result = TypingPracticeEngine.evaluate(prompt, "sustainble", elapsedMs = 20_000)

        assertFalse(result.correct)
        assertTrue(result.accuracy in 80..99)
        assertEquals(1, result.typoCount)
    }

    @Test
    fun practiceSessionSummaryTracksProgressAndNextPrompt() {
        val prompts = listOf(
            PracticePrompt(id = "p1", expected = "benefit"),
            PracticePrompt(id = "p2", expected = "method"),
            PracticePrompt(id = "p3", expected = "evidence")
        )
        val first = TypingPracticeEngine.evaluate(prompts[0], "benefit", elapsedMs = 15_000)
        val third = TypingPracticeEngine.evaluate(prompts[2], "evidenc", elapsedMs = 18_000)

        val summary = PracticeSessionEngine.summarize(prompts, listOf(first, third))

        assertEquals(3, summary.targetCount)
        assertEquals(2, summary.completedCount)
        assertEquals(1, summary.correctCount)
        assertFalse(summary.completed)
        assertEquals(1, PracticeSessionEngine.nextIndex(0, prompts, listOf(first, third)))
    }

    @Test
    fun practiceSessionCompletesOnlyWhenEveryPromptHasResult() {
        val prompts = listOf(
            PracticePrompt(id = "p1", expected = "benefit"),
            PracticePrompt(id = "p2", expected = "method")
        )
        val results = prompts.map { TypingPracticeEngine.evaluate(it, it.expected, elapsedMs = 12_000) }

        val summary = PracticeSessionEngine.summarize(prompts, results)

        assertTrue(summary.completed)
        assertEquals(2, summary.correctCount)
        assertEquals(100, summary.averageAccuracy)
        assertEquals(null, PracticeSessionEngine.nextIndex(1, prompts, results))
    }

    @Test
    fun articleDictationBuildsPromptsFromCaptionAndPlainText() {
        val cues = listOf(
            CaptionCue("c1", 0, 2_000, "Learning works when input is repeated.", "反复输入才有效。"),
            CaptionCue("c2", 2_000, 4_000, "Short sessions are easier to maintain.", "短课节奏更容易坚持。")
        )

        val fromCues = ArticleDictationEngine.fromCaptionCues(cues)
        val fromText = ArticleDictationEngine.fromPlainText("doc", "demo", "This is a useful sentence. 第二句中文。Another English sentence!")

        assertEquals(2, fromCues.size)
        assertEquals("反复输入才有效。", fromCues.first().hint)
        assertEquals(2, fromText.size)
    }

    @Test
    fun wordleHandlesRepeatedLetters() {
        val result = WordGameEngine.evaluateWordle(secret = "level", guess = "lever")

        assertEquals(listOf(
            WordleTileState.CORRECT,
            WordleTileState.CORRECT,
            WordleTileState.CORRECT,
            WordleTileState.CORRECT,
            WordleTileState.ABSENT
        ), result.tiles.map { it.state })
        assertFalse(result.won)
    }

    @Test
    fun achievementsUnlockOnceFromAttempts() {
        val now = System.currentTimeMillis()
        val attempts = listOf(
            StudyAttempt(
                id = "a1",
                taskType = StudyTaskType.HUNDRED_LS,
                durationMs = 30L * 60L * 1000L,
                createdAt = now,
                completedAt = now
            )
        )

        val unlocked = AchievementEngine.evaluate(
            contents = listOf(ImportedContent("c1", "demo", SourceType.DOCUMENT)),
            words = listOf(LearningWord("w1", "method", "method")),
            attempts = attempts,
            typingResults = listOf(TypingPracticeResult("p", "method", "method", true, 100, 10, 0, "method", "method")),
            wonWordGames = 1,
            unlockedIds = setOf("first-import")
        )

        assertTrue(unlocked.none { it.definition.id == "first-import" })
        assertTrue(unlocked.any { it.definition.type == AchievementType.FIRST_WORD })
        assertTrue(unlocked.any { it.definition.type == AchievementType.LISTENING_30 })
        assertTrue(unlocked.any { it.definition.type == AchievementType.WORDLE_WIN })
    }

    @Test
    fun analyticsSummarizesRealAttemptData() {
        val now = System.currentTimeMillis()
        val attempts = listOf(
            StudyAttempt("a1", StudyTaskType.SPELLING, score = 80, createdAt = now, completedAt = now),
            StudyAttempt("a2", StudyTaskType.LISTENING_REPEAT, score = 100, durationMs = 5L * 60L * 1000L, createdAt = now, completedAt = now),
            StudyAttempt("a3", StudyTaskType.MISTAKES, result = "wrong", score = 30, createdAt = now, completedAt = now)
        )
        val words = listOf(
            LearningWord("w1", "method", "method", status = LearningWordStatus.DUE, dueAt = now - 1_000),
            LearningWord("w2", "benefit", "benefit", status = LearningWordStatus.LEARNING, dueAt = now + 1_000)
        )

        val analytics = StudyAnalyticsEngine.build(attempts, words, unlockedAchievements = 2, now = now)

        assertEquals(3, analytics.todayAttempts)
        assertEquals(70, analytics.accuracy)
        assertEquals(1, analytics.dueCount)
        assertEquals(1, analytics.mistakeCount)
        assertEquals(5, analytics.listeningMinutes)
        assertEquals(2, analytics.unlockedAchievements)
    }

    @Test
    fun deckImportSupportsCsvJsonAndTxt() {
        val csv = """
            word,phonetic,definition,cefr,deck,tags,phrases,example,licenseSource
            method,/ˈmeθəd/,方法,B1,core,study|core,practical method|method works,A method helps,GPL-compatible sample
        """.trimIndent()
        val json = """{"items":[{"word":"analyze","definition":"分析","tags":["awl"],"phrases":["analyze data"]}]}"""
        val txt = "benefit\t好处\tLearners benefit from repeated input."

        assertEquals("method", DeckImportNormalizer.parseCsv(csv).items.single().word)
        assertEquals(listOf("awl"), DeckImportNormalizer.parseJson(json).items.single().tags)
        assertEquals("benefit", DeckImportNormalizer.parseTypeWordsTxt(txt).items.single().word)
    }

    @Test
    fun deckImportSupportsMujingVocabularyJson() {
        val mujing = """
            {
              "name": "四级",
              "wordList": [
                {
                  "value": "cancel",
                  "usphone": "'kænsl",
                  "translation": "取消；撤销",
                  "tag": "cet4 cet6",
                  "captions": [{"content": "They had to cancel the meeting."}]
                }
              ]
            }
        """.trimIndent()

        val item = DeckImportNormalizer.parseJson(mujing).items.single()

        assertEquals("cancel", item.word)
        assertEquals("四级", item.deck)
        assertEquals(listOf("cet4", "cet6"), item.tags)
        assertEquals("They had to cancel the meeting.", item.example)
    }
}
