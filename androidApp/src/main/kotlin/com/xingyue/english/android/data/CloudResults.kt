package com.xingyue.english.android.data

data class CloudCheckResult(
    val success: Boolean,
    val message: String,
    val checkedCapabilities: List<String> = emptyList()
)

sealed class SpeechPlaybackState(open val message: String) {
    data object Idle : SpeechPlaybackState("")
    data object Loading : SpeechPlaybackState("正在生成云端发音...")
    data class Cloud(val voiceId: String) : SpeechPlaybackState("云端发音已播放")
    data class Cached(val voiceId: String) : SpeechPlaybackState("已播放缓存发音")
    data class SystemFallback(val reason: String) : SpeechPlaybackState("已切换系统发音：$reason")
    data class Failed(val reason: String) : SpeechPlaybackState("发音失败：$reason")
}

data class ImportUiEvent(
    val message: String,
    val severity: ImportUiSeverity = ImportUiSeverity.INFO,
    val actionLabel: String? = null
)

enum class ImportUiSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}
