package com.xingyue.english.android.data

import com.xingyue.english.android.data.PlatformLinkResolver.SupportedPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PlatformMediaExtractorTest {
    @Test
    fun youtubeJsonPrefersManualEnglishSubtitleOverAutomaticAndChinese() {
        val result = extractorFor(
            """
            {
              "extractor_key": "Youtube",
              "webpage_url": "https://www.youtube.com/watch?v=demo",
              "title": "YouTube Demo",
              "duration": 42.5,
              "thumbnail": "https://i.ytimg.com/demo.jpg",
              "subtitles": {
                "zh-Hans": [{"ext": "vtt", "url": "https://caption.example/zh.vtt", "name": "Chinese"}],
                "en": [{"ext": "srt", "url": "https://caption.example/en.srt", "name": "English"}]
              },
              "automatic_captions": {
                "en": [{"ext": "vtt", "url": "https://caption.example/en-auto.vtt", "name": "English auto"}]
              },
              "formats": [
                {"format_id": "18", "url": "https://media.example/video.mp4", "ext": "mp4", "acodec": "mp4a", "vcodec": "avc1", "height": 360, "abr": 96},
                {"format_id": "140", "url": "https://media.example/audio.m4a", "ext": "m4a", "acodec": "mp4a", "vcodec": "none", "abr": 128}
              ]
            }
            """.trimIndent()
        ).extract("https://youtu.be/demo")

        val success = assertIs<PlatformImportResult.Success>(result)
        assertEquals(SupportedPlatform.YOUTUBE, success.mediaInfo.platform)
        assertEquals("YouTube Demo", success.mediaInfo.title)
        assertEquals(42_500L, success.mediaInfo.durationMs)
        assertEquals(PlatformImportNextStep.IMPORT_SUBTITLE, success.nextStep)
        assertEquals("en", success.selectedSubtitle?.languageCode)
        assertEquals(PlatformSubtitleSource.MANUAL, success.selectedSubtitle?.source)
        assertEquals("https://media.example/audio.m4a", success.selectedMedia?.url)
    }

    @Test
    fun bilibiliJsonFallsBackToChineseSubtitleWhenNoEnglishSubtitleExists() {
        val result = extractorFor(
            """
            {
              "extractor_key": "BiliBili",
              "webpage_url": "https://www.bilibili.com/video/BV1demo",
              "title": "Bilibili Demo",
              "duration": 120,
              "subtitles": {
                "zh-CN": [{"ext": "vtt", "url": "https://caption.example/bili-zh.vtt", "name": "中文"}]
              },
              "formats": [
                {"format_id": "video", "url": "https://media.example/bili.mp4", "ext": "mp4", "acodec": "mp4a", "vcodec": "avc1", "height": 720, "abr": 96}
              ]
            }
            """.trimIndent()
        ).extract("https://www.bilibili.com/video/BV1demo")

        val success = assertIs<PlatformImportResult.Success>(result)
        assertEquals(SupportedPlatform.BILIBILI, success.mediaInfo.platform)
        assertEquals(PlatformImportNextStep.IMPORT_SUBTITLE, success.nextStep)
        assertEquals("zh-CN", success.selectedSubtitle?.languageCode)
        assertEquals("https://caption.example/bili-zh.vtt", success.selectedSubtitle?.url)
    }

    @Test
    fun douyinJsonWithoutSubtitlesSelectsAudioForAsr() {
        val result = extractorFor(
            """
            {
              "extractor_key": "Douyin",
              "webpage_url": "https://www.douyin.com/video/123",
              "title": "Douyin Demo",
              "thumbnail": "https://img.example/douyin.jpg",
              "formats": [
                {"format_id": "dash-video", "url": "https://media.example/douyin-video.mp4", "ext": "mp4", "acodec": "none", "vcodec": "h264", "height": 720},
                {"format_id": "dash-audio", "url": "https://media.example/douyin-audio.m4a", "ext": "m4a", "acodec": "aac", "vcodec": "none", "abr": 96}
              ]
            }
            """.trimIndent()
        ).extract("https://v.douyin.com/demo123/")

        val success = assertIs<PlatformImportResult.Success>(result)
        assertEquals(SupportedPlatform.DOUYIN, success.mediaInfo.platform)
        assertEquals(PlatformImportNextStep.TRANSCRIBE_AUDIO, success.nextStep)
        assertEquals(null, success.selectedSubtitle)
        assertEquals("https://media.example/douyin-audio.m4a", success.selectedMedia?.url)
        assertEquals(true, success.selectedMedia?.audioOnly)
    }

    @Test
    fun runnerErrorsAreMappedToActionableFailures() {
        val loginFailure = failingExtractor("ERROR: This video requires login cookies").extract("https://www.youtube.com/watch?v=private")
        assertEquals(PlatformImportFailure.PLATFORM_RESTRICTED, assertIs<PlatformImportResult.Failure>(loginFailure).reason)

        val rateLimitFailure = failingExtractor("ERROR: HTTP Error 429: Too Many Requests").extract("https://www.bilibili.com/video/BV1demo")
        assertEquals(PlatformImportFailure.NETWORK_OR_RATE_LIMITED, assertIs<PlatformImportResult.Failure>(rateLimitFailure).reason)

        val unsupportedFailure = failingExtractor("ERROR: Unsupported URL: https://example.com/page").extract("https://v.douyin.com/demo123/")
        assertEquals(PlatformImportFailure.UNSUPPORTED_URL, assertIs<PlatformImportResult.Failure>(unsupportedFailure).reason)

        val ageFailure = failingExtractor("ERROR: Sign in to confirm your age").extract("https://www.youtube.com/watch?v=age")
        assertEquals(PlatformImportFailure.PLATFORM_RESTRICTED, assertIs<PlatformImportResult.Failure>(ageFailure).reason)

        val geoFailure = failingExtractor("ERROR: This video is not available in your country").extract("https://www.youtube.com/watch?v=geo")
        assertEquals(PlatformImportFailure.PLATFORM_RESTRICTED, assertIs<PlatformImportResult.Failure>(geoFailure).reason)

        val noFormatsFailure = failingExtractor("ERROR: No video formats found").extract("https://www.bilibili.com/video/BV1demo")
        assertEquals(PlatformImportFailure.NO_MEDIA_FORMATS, assertIs<PlatformImportResult.Failure>(noFormatsFailure).reason)
    }

    @Test
    fun missingFormatsAndSubtitlesFailsClearly() {
        val result = extractorFor(
            """
            {
              "extractor_key": "Youtube",
              "webpage_url": "https://www.youtube.com/watch?v=empty",
              "title": "No Public Media",
              "formats": [],
              "subtitles": {},
              "automatic_captions": {}
            }
            """.trimIndent()
        ).extract("https://www.youtube.com/watch?v=empty")

        val failure = assertIs<PlatformImportResult.Failure>(result)
        assertEquals(PlatformImportFailure.NO_MEDIA_FORMATS, failure.reason)
        assertEquals(SupportedPlatform.YOUTUBE, failure.platform)
    }

    @Test
    fun videoOnlyWithoutSubtitlesFailsBecauseAsrNeedsAudio() {
        val result = extractorFor(
            """
            {
              "extractor_key": "Douyin",
              "webpage_url": "https://www.douyin.com/video/123",
              "title": "Video Only",
              "formats": [
                {"format_id": "video", "url": "https://media.example/video-only.mp4", "ext": "mp4", "acodec": "none", "vcodec": "h264", "height": 720}
              ]
            }
            """.trimIndent()
        ).extract("https://v.douyin.com/demo123/")

        val failure = assertIs<PlatformImportResult.Failure>(result)
        assertEquals(PlatformImportFailure.NO_SUBTITLES_OR_AUDIO, failure.reason)
    }

    @Test
    fun directUrlIsRejectedByPlatformExtractor() {
        val result = extractorFor("{}").extract("https://example.com/video.mp4")

        val failure = assertIs<PlatformImportResult.Failure>(result)
        assertEquals(SupportedPlatform.DIRECT, failure.platform)
        assertEquals(PlatformImportFailure.UNSUPPORTED_URL, failure.reason)
    }

    @Test
    fun resolverCanRouteSharedTextToPlatformExtractor() {
        val result = PlatformLinkResolver().resolvePlatformMedia(
            input = "复制这段内容 https://v.douyin.com/demo123/ 打开抖音",
            extractor = extractorFor(
                """
                {
                  "extractor_key": "Douyin",
                  "webpage_url": "https://www.douyin.com/video/123",
                  "title": "Shared Douyin",
                  "formats": [
                    {"format_id": "audio", "url": "https://media.example/audio.m4a", "ext": "m4a", "acodec": "aac", "vcodec": "none", "abr": 96}
                  ]
                }
                """.trimIndent()
            )
        )

        val success = assertIs<PlatformImportResult.Success>(result)
        assertEquals(SupportedPlatform.DOUYIN, success.mediaInfo.platform)
        assertEquals("Shared Douyin", success.mediaInfo.title)
    }

    private fun extractorFor(json: String): YtDlpPlatformExtractor =
        YtDlpPlatformExtractor(
            object : YtDlpRunner {
                override fun dumpSingleJson(url: String): YtDlpRunResult =
                    YtDlpRunResult(exitCode = 0, output = json)
            }
        )

    private fun failingExtractor(error: String): YtDlpPlatformExtractor =
        YtDlpPlatformExtractor(
            object : YtDlpRunner {
                override fun dumpSingleJson(url: String): YtDlpRunResult =
                    YtDlpRunResult(exitCode = 1, errorOutput = error)
            }
        )
}
