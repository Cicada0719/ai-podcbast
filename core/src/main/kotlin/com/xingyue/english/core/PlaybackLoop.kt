package com.xingyue.english.core

enum class AbLoopPhase {
    EMPTY,
    WAITING_FOR_B,
    ACTIVE
}

data class AbLoopState(
    val startMs: Long = -1L,
    val endMs: Long = -1L
) {
    val phase: AbLoopPhase
        get() = when {
            startMs < 0L -> AbLoopPhase.EMPTY
            endMs <= startMs -> AbLoopPhase.WAITING_FOR_B
            else -> AbLoopPhase.ACTIVE
        }
}

data class PlaybackLoopRange(
    val startMs: Long,
    val endMs: Long,
    val repeat: Boolean
)

data class AbLoopUpdate(
    val state: AbLoopState,
    val message: String,
    val seekToStart: Boolean = false
)

object PlaybackLoopEngine {
    const val MIN_AB_DURATION_MS: Long = 500L

    fun onAbMarkerTap(
        state: AbLoopState,
        positionMs: Long,
        minDurationMs: Long = MIN_AB_DURATION_MS
    ): AbLoopUpdate {
        val position = positionMs.coerceAtLeast(0L)
        return when (state.phase) {
            AbLoopPhase.EMPTY -> AbLoopUpdate(
                state = AbLoopState(startMs = position, endMs = -1L),
                message = "已设置 A 点 ${formatShort(position)}，继续播放后点击设置 B 点。"
            )

            AbLoopPhase.WAITING_FOR_B -> {
                val minEnd = state.startMs + minDurationMs
                if (position < minEnd) {
                    AbLoopUpdate(
                        state = state,
                        message = "B 点需要比 A 点至少晚 ${minDurationMs}ms。"
                    )
                } else {
                    AbLoopUpdate(
                        state = state.copy(endMs = position),
                        message = "A-B 循环已开启：${formatShort(state.startMs)} - ${formatShort(position)}。",
                        seekToStart = true
                    )
                }
            }

            AbLoopPhase.ACTIVE -> AbLoopUpdate(
                state = AbLoopState(),
                message = "A-B 循环已清除。"
            )
        }
    }

    fun activeRange(state: AbLoopState): PlaybackLoopRange? =
        if (state.phase == AbLoopPhase.ACTIVE) PlaybackLoopRange(state.startMs, state.endMs, repeat = true) else null

    fun shouldRestartAtStart(positionMs: Long, range: PlaybackLoopRange?): Boolean =
        range != null && positionMs >= range.endMs

    private fun formatShort(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
