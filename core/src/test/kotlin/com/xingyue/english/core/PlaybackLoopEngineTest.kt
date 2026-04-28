package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackLoopEngineTest {
    @Test
    fun abLoopMovesFromASetToActiveAndThenClears() {
        val a = PlaybackLoopEngine.onAbMarkerTap(AbLoopState(), 1_200L)

        assertEquals(AbLoopPhase.WAITING_FOR_B, a.state.phase)
        assertTrue(a.message.contains("A 点"))
        assertNull(PlaybackLoopEngine.activeRange(a.state))

        val b = PlaybackLoopEngine.onAbMarkerTap(a.state, 3_200L)

        assertEquals(AbLoopPhase.ACTIVE, b.state.phase)
        assertTrue(b.seekToStart)
        val range = assertNotNull(PlaybackLoopEngine.activeRange(b.state))
        assertEquals(1_200L, range.startMs)
        assertEquals(3_200L, range.endMs)
        assertTrue(range.repeat)

        val cleared = PlaybackLoopEngine.onAbMarkerTap(b.state, 3_600L)

        assertEquals(AbLoopPhase.EMPTY, cleared.state.phase)
        assertFalse(cleared.seekToStart)
    }

    @Test
    fun abLoopRejectsTooShortBPointAndKeepsA() {
        val a = PlaybackLoopEngine.onAbMarkerTap(AbLoopState(), 5_000L)

        val invalid = PlaybackLoopEngine.onAbMarkerTap(a.state, 5_200L)

        assertEquals(AbLoopPhase.WAITING_FOR_B, invalid.state.phase)
        assertEquals(5_000L, invalid.state.startMs)
        assertEquals(-1L, invalid.state.endMs)
        assertTrue(invalid.message.contains("至少"))
        assertFalse(invalid.seekToStart)
    }

    @Test
    fun abLoopOnlyRestartsWhenPositionReachesEnd() {
        val range = PlaybackLoopEngine.activeRange(AbLoopState(1_000L, 2_000L))

        assertFalse(PlaybackLoopEngine.shouldRestartAtStart(1_999L, range))
        assertTrue(PlaybackLoopEngine.shouldRestartAtStart(2_000L, range))
        assertFalse(PlaybackLoopEngine.shouldRestartAtStart(10_000L, null))
    }
}
