package com.xingyue.english.android.ui

import androidx.compose.ui.graphics.Color

internal enum class HoloMotionLevel {
    Standard,
    Reduced
}

internal data class HoloThemeTokens(
    val background: Color = Color(0xFF070A0F),
    val panel: Color = Color(0xFF101722),
    val panelBright: Color = Color(0xFF172231),
    val stroke: Color = Color(0xFF24384F),
    val primaryGlow: Color = Color(0xFF4C8DFF),
    val secondaryGlow: Color = Color(0xFF3DB7A6),
    val successGlow: Color = Color(0xFF6EDB8F),
    val accentGold: Color = Color(0xFFF2B84B),
    val textMain: Color = Color(0xFFF4F7FB),
    val textSoft: Color = Color(0xFFA8B2C0)
)

internal data class PlaybackJumpTarget(
    val contentId: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val autoplay: Boolean = true,
    val loop: Boolean = true
)

internal data class TimelineCueUiState(
    val cueId: String,
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val english: String,
    val chinese: String,
    val active: Boolean,
    val savedTokenCount: Int,
    val totalTokenCount: Int
)

internal data class ImportStageUiState(
    val stageLabel: String,
    val progress: Int,
    val userMessage: String,
    val nextAction: String = ""
)

internal data class LearningTaskUiState(
    val taskId: String,
    val title: String,
    val subtitle: String,
    val step: Int,
    val remainingCount: Int,
    val completedCount: Int,
    val estimatedMinutes: Int
)
