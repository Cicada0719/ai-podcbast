package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LearningWordRepositoryTest {
    @Test
    fun savesSelectionWithCaptionContext() {
        val repository = InMemoryLearningWordRepository()

        val saved = repository.addFromSelection(
            WordSelectionContext(
                word = "Deliberate",
                sourceItemId = "content-1",
                captionStartMs = 1000L,
                captionEndMs = 3000L,
                englishSentence = "Deliberate practice matters.",
                chineseSentence = "刻意练习很重要。",
                sourceType = LearningSourceType.VIDEO,
                captionId = "caption-1",
                sourceTitle = "Demo",
                phonetic = "/dɪˈlɪbərət/",
                chineseDefinition = "刻意的"
            )
        )

        assertEquals("deliberate", saved.normalized)
        assertEquals("刻意的", saved.chineseDefinition)
        assertEquals(1, repository.wordsForCaption("caption-1").size)
        assertNotNull(repository.search("delib").singleOrNull())
    }

    @Test
    fun markMasteredAndDueKeepsWordVisible() {
        val repository = InMemoryLearningWordRepository()
        repository.addFromSelection(
            WordSelectionContext(
                word = "context",
                sourceItemId = "doc-1",
                captionStartMs = 0L,
                captionEndMs = 1000L,
                englishSentence = "Context makes words memorable.",
                chineseSentence = "",
                sourceType = LearningSourceType.DOCUMENT
            )
        )

        assertEquals(LearningWordStatus.MASTERED, repository.markMastered("context")?.status)
        assertEquals(LearningWordStatus.DUE, repository.markDue("context")?.status)
        assertTrue(repository.search(filters = LearningWordFilters(status = LearningWordStatus.DUE)).isNotEmpty())
    }

    @Test
    fun duplicateSelectionDoesNotCreateDuplicateContext() {
        val repository = InMemoryLearningWordRepository()
        val context = WordSelectionContext(
            word = "practice",
            sourceItemId = "content-1",
            captionStartMs = 0L,
            captionEndMs = 1000L,
            englishSentence = "Practice makes memory durable.",
            chineseSentence = "",
            sourceType = LearningSourceType.SUBTITLE,
            captionId = "caption-1",
            sourceTitle = "Demo"
        )

        repository.addFromSelection(context)
        val savedAgain = repository.addFromSelection(context)

        assertEquals(1, savedAgain.occurrenceCount)
        assertEquals(1, savedAgain.contexts.size)
    }
}
