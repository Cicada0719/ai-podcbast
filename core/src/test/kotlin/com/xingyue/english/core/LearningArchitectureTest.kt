package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearningArchitectureTest {
    @Test
    fun requiredCapabilitiesCoverEssentialEnglishLearningNeeds() {
        val requiredNeeds = EnglishLearningArchitecture.requiredCapabilities().map { it.need }.toSet()

        assertTrue(requiredNeeds.contains(LearningNeed.MATERIAL_INPUT))
        assertTrue(requiredNeeds.contains(LearningNeed.COMPREHENSIBLE_LISTENING))
        assertTrue(requiredNeeds.contains(LearningNeed.LEXICAL_RETRIEVAL))
        assertTrue(requiredNeeds.contains(LearningNeed.SPACED_REVIEW))
        assertTrue(requiredNeeds.contains(LearningNeed.OUTPUT_PRACTICE))
        assertTrue(EnglishLearningArchitecture.validate().isEmpty())
    }

    @Test
    fun gamesAndAnalyticsDoNotBlockMainStudyPath() {
        val optionalGames = EnglishLearningArchitecture.capabilities.single { it.id == "light-games" }
        val analytics = EnglishLearningArchitecture.capabilities.single { it.id == "study-analytics" }

        assertEquals(CapabilityPriority.OPTIONAL, optionalGames.priority)
        assertEquals(CapabilityPriority.SUPPORTING, analytics.priority)
        assertTrue(EnglishLearningArchitecture.requiredCapabilities().none { it.id == optionalGames.id })
    }

    @Test
    fun nextRequiredSurfaceStartsFromMaterialInput() {
        assertEquals(
            LearningSurface.IMPORT,
            EnglishLearningArchitecture.nextRequiredSurface(emptySet())
        )
        assertEquals(
            LearningSurface.PLAYER,
            EnglishLearningArchitecture.nextRequiredSurface(setOf("material-import"))
        )
    }
}
