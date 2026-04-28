package com.xingyue.english.android.data

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal interface PlatformMediaExtractor {
    fun extract(url: String): PlatformImportResult
}

internal interface YtDlpRunner {
    fun dumpSingleJson(url: String): YtDlpRunResult
}

internal data class YtDlpRunResult(
    val exitCode: Int,
    val output: String = "",
    val errorOutput: String = ""
)

internal class AndroidYtDlpRunner(context: Context) : YtDlpRunner {
    private val appContext = context.applicationContext

    override fun dumpSingleJson(url: String): YtDlpRunResult {
        return runCatching {
            YoutubeDL.init(appContext)
            FFmpeg.init(appContext)
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--no-playlist")
                addOption("--skip-download")
                addOption("--no-warnings")
                addOption("--socket-timeout", 20)
            }
            val response = YoutubeDL.getInstance().execute(
                request = request,
                processId = null,
                redirectErrorStream = false,
                callback = null
            )
            YtDlpRunResult(
                exitCode = response.exitCode,
                output = response.out,
                errorOutput = response.err
            )
        }.getOrElse { error ->
            YtDlpRunResult(
                exitCode = 1,
                errorOutput = error.message ?: "平台解析失败"
            )
        }
    }
}

internal class YtDlpPlatformExtractor(
    private val runner: YtDlpRunner
) : PlatformMediaExtractor {
    override fun extract(url: String): PlatformImportResult {
        val platform = PlatformLinkResolver.identifyPlatform(url)
        if (platform == PlatformLinkResolver.SupportedPlatform.DIRECT) {
            return PlatformImportResult.Failure(
                platform = platform,
                originalUrl = url,
                reason = PlatformImportFailure.UNSUPPORTED_URL,
                message = "该链接不是支持的视频平台链接，请走直链导入。"
            )
        }

        val run = runCatching { runner.dumpSingleJson(url) }.getOrElse { error ->
            return PlatformImportResult.Failure(
                platform = platform,
                originalUrl = url,
                reason = PlatformImportFailure.UNKNOWN,
                message = "平台解析启动失败：${error.message ?: "请重试"}"
            )
        }
        if (run.exitCode != 0) {
            return mapYtDlpFailure(platform, url, run.output, run.errorOutput)
        }
        if (run.output.isBlank()) {
            return PlatformImportResult.Failure(
                platform = platform,
                originalUrl = url,
                reason = PlatformImportFailure.UNKNOWN,
                message = "平台没有返回可导入信息。"
            )
        }

        val mediaInfo = runCatching { parseMediaInfo(url, platform, JSONObject(run.output)) }
            .getOrElse { error ->
                return PlatformImportResult.Failure(
                    platform = platform,
                    originalUrl = url,
                    reason = PlatformImportFailure.UNKNOWN,
                    message = "平台内容解析失败：${error.message ?: "格式不兼容"}"
                )
            }

        val subtitle = selectSubtitle(mediaInfo.subtitles)
        val asrMedia = selectAsrMedia(mediaInfo.formats)
        return when {
            subtitle != null -> PlatformImportResult.Success(
                mediaInfo = mediaInfo,
                selectedSubtitle = subtitle,
                selectedMedia = asrMedia ?: selectPlayableMedia(mediaInfo.formats),
                nextStep = PlatformImportNextStep.IMPORT_SUBTITLE,
                message = "已解析 ${platform.displayName} 字幕：${subtitle.languageCode.ifBlank { subtitle.name }}"
            )
            asrMedia != null -> PlatformImportResult.Success(
                mediaInfo = mediaInfo,
                selectedSubtitle = null,
                selectedMedia = asrMedia,
                nextStep = PlatformImportNextStep.TRANSCRIBE_AUDIO,
                message = "未发现可用字幕，已选择音频用于转写。"
            )
            mediaInfo.formats.none { it.url.isNotBlank() } -> PlatformImportResult.Failure(
                platform = platform,
                originalUrl = url,
                reason = PlatformImportFailure.NO_MEDIA_FORMATS,
                message = "平台没有返回可导入的媒体。"
            )
            else -> PlatformImportResult.Failure(
                platform = platform,
                originalUrl = url,
                reason = PlatformImportFailure.NO_SUBTITLES_OR_AUDIO,
                message = "平台没有返回字幕，也没有可转写的音频。"
            )
        }
    }

    private fun parseMediaInfo(
        originalUrl: String,
        platform: PlatformLinkResolver.SupportedPlatform,
        json: JSONObject
    ): PlatformMediaInfo =
        PlatformMediaInfo(
            platform = platform,
            originalUrl = originalUrl,
            canonicalUrl = json.optCleanString("webpage_url")
                .ifBlank { json.optCleanString("original_url") }
                .ifBlank { originalUrl },
            title = json.optCleanString("title").ifBlank { "${platform.displayName} 学习材料" },
            durationMs = json.optDurationMs(),
            thumbnailUrl = json.optCleanString("thumbnail").ifBlank { firstThumbnail(json.optJSONArray("thumbnails")) },
            subtitles = parseSubtitleObject(json.optJSONObject("subtitles"), PlatformSubtitleSource.MANUAL) +
                parseSubtitleObject(json.optJSONObject("automatic_captions"), PlatformSubtitleSource.AUTOMATIC),
            formats = parseFormats(json.optJSONArray("formats")),
            extractorMessage = json.optCleanString("extractor").ifBlank { json.optCleanString("extractor_key") }
        )

    private fun parseSubtitleObject(
        subtitles: JSONObject?,
        source: PlatformSubtitleSource
    ): List<PlatformSubtitleTrack> {
        if (subtitles == null) return emptyList()
        val tracks = mutableListOf<PlatformSubtitleTrack>()
        val keys = subtitles.keys()
        while (keys.hasNext()) {
            val language = keys.next()
            val entries = subtitles.optJSONArray(language) ?: continue
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val url = item.optCleanString("url")
                if (url.isBlank()) continue
                tracks += PlatformSubtitleTrack(
                    languageCode = language,
                    name = item.optCleanString("name").ifBlank { item.optCleanString("language") },
                    url = url,
                    extension = item.optCleanString("ext").ifBlank { PlatformLinkResolver.extensionFromUrl(url) },
                    source = source
                )
            }
        }
        return tracks
    }

    private fun parseFormats(formats: JSONArray?): List<PlatformFormatCandidate> {
        if (formats == null) return emptyList()
        val candidates = mutableListOf<PlatformFormatCandidate>()
        for (index in 0 until formats.length()) {
            val item = formats.optJSONObject(index) ?: continue
            val url = item.optCleanString("url")
            if (url.isBlank()) continue
            val audioCodec = item.optCleanString("acodec")
            val videoCodec = item.optCleanString("vcodec")
            val hasAudio = audioCodec.isNotBlank() && !audioCodec.equals("none", ignoreCase = true)
            val hasVideo = videoCodec.isNotBlank() && !videoCodec.equals("none", ignoreCase = true)
            candidates += PlatformFormatCandidate(
                formatId = item.optCleanString("format_id"),
                url = url,
                extension = item.optCleanString("ext").ifBlank { PlatformLinkResolver.extensionFromUrl(url) },
                mimeType = item.optCleanString("mime_type"),
                protocol = item.optCleanString("protocol"),
                audioCodec = audioCodec,
                videoCodec = videoCodec,
                hasAudio = hasAudio,
                hasVideo = hasVideo,
                width = item.optNullableInt("width"),
                height = item.optNullableInt("height"),
                audioBitrate = item.optNullableDouble("abr"),
                videoBitrate = item.optNullableDouble("vbr"),
                filesize = item.optNullableLong("filesize") ?: item.optNullableLong("filesize_approx")
            )
        }
        return candidates
    }

    private fun selectSubtitle(tracks: List<PlatformSubtitleTrack>): PlatformSubtitleTrack? =
        tracks
            .filter { it.url.isNotBlank() }
            .sortedWith(
                compareBy<PlatformSubtitleTrack> { track ->
                    when {
                        track.source == PlatformSubtitleSource.MANUAL && track.isEnglish -> 0
                        track.source == PlatformSubtitleSource.AUTOMATIC && track.isEnglish -> 1
                        track.isChinese -> 2
                        else -> 3
                    }
                }.thenBy { if (it.source == PlatformSubtitleSource.MANUAL) 0 else 1 }
                    .thenBy { subtitleExtensionRank(it.extension) }
            )
            .firstOrNull()

    private fun selectAsrMedia(formats: List<PlatformFormatCandidate>): PlatformFormatCandidate? =
        formats
            .filter { it.url.isNotBlank() && it.hasAudio }
            .maxWithOrNull(
                compareBy<PlatformFormatCandidate> { if (it.audioOnly) 2 else 1 }
                    .thenBy { audioExtensionRank(it.extension) }
                    .thenBy { it.audioBitrate ?: 0.0 }
            )

    private fun selectPlayableMedia(formats: List<PlatformFormatCandidate>): PlatformFormatCandidate? =
        formats
            .filter { it.url.isNotBlank() }
            .maxWithOrNull(
                compareBy<PlatformFormatCandidate> { if (it.hasVideo) 1 else 0 }
                    .thenBy { it.height ?: 0 }
                    .thenBy { it.audioBitrate ?: 0.0 }
            )

    private fun mapYtDlpFailure(
        platform: PlatformLinkResolver.SupportedPlatform,
        url: String,
        output: String,
        errorOutput: String
    ): PlatformImportResult.Failure {
        val raw = "$output\n$errorOutput".trim()
        val normalized = raw.lowercase(Locale.ROOT)
        val reason = when {
            normalized.contains("unsupported url") || normalized.contains("unsupportedurl") ->
                PlatformImportFailure.UNSUPPORTED_URL
            normalized.contains("no video formats found") ||
                normalized.contains("no formats found") ||
                normalized.contains("requested format is not available") ||
                normalized.contains("format not available") ->
                PlatformImportFailure.NO_MEDIA_FORMATS
            normalized.contains("403") ||
                normalized.contains("429") ||
                normalized.contains("timed out") ||
                normalized.contains("timeout") ||
                normalized.contains("temporarily unavailable") ->
                PlatformImportFailure.NETWORK_OR_RATE_LIMITED
            normalized.contains("login") ||
                normalized.contains("sign in") ||
                normalized.contains("cookie") ||
                normalized.contains("private") ||
                normalized.contains("age-restricted") ||
                normalized.contains("age restricted") ||
                normalized.contains("not available in your country") ||
                normalized.contains("geo-restricted") ||
                normalized.contains("geo restricted") ||
                normalized.contains("copyright") ||
                normalized.contains("会员") ||
                normalized.contains("付费") ||
                normalized.contains("drm") ||
                normalized.contains("premium") ||
                normalized.contains("payment") ->
                PlatformImportFailure.PLATFORM_RESTRICTED
            else -> PlatformImportFailure.UNKNOWN
        }
        return PlatformImportResult.Failure(
            platform = platform,
            originalUrl = url,
            reason = reason,
            message = when (reason) {
                PlatformImportFailure.PLATFORM_RESTRICTED -> "平台限制：该内容暂时不能直接导入，可改用本地文件或字幕。"
                PlatformImportFailure.NETWORK_OR_RATE_LIMITED -> "平台解析失败：网络受限，请稍后重试。"
                PlatformImportFailure.UNSUPPORTED_URL -> "平台解析失败：暂不支持该链接。"
                PlatformImportFailure.NO_MEDIA_FORMATS -> "平台没有返回可导入的媒体。"
                else -> "平台解析失败：${raw.ifBlank { "未知错误" }.take(240)}"
            }
        )
    }

    private fun firstThumbnail(thumbnails: JSONArray?): String {
        if (thumbnails == null) return ""
        for (index in 0 until thumbnails.length()) {
            val url = thumbnails.optJSONObject(index)?.optCleanString("url").orEmpty()
            if (url.isNotBlank()) return url
        }
        return ""
    }

    private fun subtitleExtensionRank(extension: String): Int =
        when (extension.lowercase(Locale.ROOT)) {
            "vtt" -> 0
            "srt" -> 1
            "json3" -> 2
            "srv3", "srv2", "srv1" -> 3
            "ttml" -> 4
            else -> 9
        }

    private fun audioExtensionRank(extension: String): Int =
        when (extension.lowercase(Locale.ROOT)) {
            "m4a", "mp3" -> 4
            "webm" -> 3
            "mp4" -> 2
            "m3u8" -> 1
            else -> 0
        }
}

internal data class PlatformMediaInfo(
    val platform: PlatformLinkResolver.SupportedPlatform,
    val originalUrl: String,
    val canonicalUrl: String,
    val title: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val subtitles: List<PlatformSubtitleTrack>,
    val formats: List<PlatformFormatCandidate>,
    val extractorMessage: String = ""
)

internal data class PlatformSubtitleTrack(
    val languageCode: String,
    val name: String,
    val url: String,
    val extension: String,
    val source: PlatformSubtitleSource
) {
    val isEnglish: Boolean
        get() = languageCode.lowercase(Locale.ROOT).startsWith("en")

    val isChinese: Boolean
        get() {
            val language = languageCode.lowercase(Locale.ROOT)
            return language.startsWith("zh") || language.startsWith("cmn")
        }
}

internal data class PlatformFormatCandidate(
    val formatId: String,
    val url: String,
    val extension: String,
    val mimeType: String,
    val protocol: String,
    val audioCodec: String,
    val videoCodec: String,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val width: Int? = null,
    val height: Int? = null,
    val audioBitrate: Double? = null,
    val videoBitrate: Double? = null,
    val filesize: Long? = null
) {
    val audioOnly: Boolean
        get() = hasAudio && !hasVideo
}

internal sealed class PlatformImportResult {
    data class Success(
        val mediaInfo: PlatformMediaInfo,
        val selectedSubtitle: PlatformSubtitleTrack?,
        val selectedMedia: PlatformFormatCandidate?,
        val nextStep: PlatformImportNextStep,
        val message: String
    ) : PlatformImportResult()

    data class Failure(
        val platform: PlatformLinkResolver.SupportedPlatform,
        val originalUrl: String,
        val reason: PlatformImportFailure,
        val message: String
    ) : PlatformImportResult()
}

internal enum class PlatformSubtitleSource {
    MANUAL,
    AUTOMATIC
}

internal enum class PlatformImportNextStep {
    IMPORT_SUBTITLE,
    TRANSCRIBE_AUDIO
}

internal enum class PlatformImportFailure {
    PLATFORM_RESTRICTED,
    NETWORK_OR_RATE_LIMITED,
    UNSUPPORTED_URL,
    NO_MEDIA_FORMATS,
    NO_SUBTITLES_OR_AUDIO,
    UNKNOWN
}

private fun JSONObject.optCleanString(name: String): String =
    if (has(name) && !isNull(name)) optString(name).trim() else ""

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) runCatching { getLong(name) }.getOrNull() else null

private fun JSONObject.optNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) runCatching { getInt(name) }.getOrNull() else null

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (has(name) && !isNull(name)) runCatching { getDouble(name) }.getOrNull() else null

private fun JSONObject.optDurationMs(): Long {
    val seconds = optNullableDouble("duration") ?: return 0L
    return (seconds * 1000.0).toLong().coerceAtLeast(0L)
}
