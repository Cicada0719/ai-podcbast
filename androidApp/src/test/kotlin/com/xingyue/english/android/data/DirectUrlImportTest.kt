package com.xingyue.english.android.data

import com.xingyue.english.android.data.PlatformLinkResolver.SupportedPlatform
import com.xingyue.english.core.SourceType
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DirectUrlImportTest {
    @Test
    fun directUrlKindCoversTextJsonSrtAndAudio() {
        assertEquals(SourceType.DOCUMENT, ImportProcessor.directKindFor("txt", ""))
        assertEquals(SourceType.DOCUMENT, ImportProcessor.directKindFor("", "application/json"))
        assertEquals(SourceType.SUBTITLE, ImportProcessor.directKindFor("srt", ""))
        assertEquals(SourceType.AUDIO, ImportProcessor.directKindFor("", "audio/wav"))
        assertEquals(SourceType.VIDEO, ImportProcessor.directKindFor("mp4", ""))
    }

    @Test
    fun directUrlKindRejectsUnsupportedAndPickerOnlyFormats() {
        assertFailsWith<IllegalStateException> {
            ImportProcessor.directKindFor("exe", "")
        }
        assertFailsWith<IllegalStateException> {
            ImportProcessor.directKindFor("pdf", "")
        }
        assertFailsWith<IllegalStateException> {
            ImportProcessor.directKindFor("apkg", "")
        }
        assertFailsWith<IllegalStateException> {
            ImportProcessor.directKindFor("", "text/html")
        }
    }

    @Test
    fun contentTypeCanFillMissingExtension() {
        assertEquals("txt", ImportProcessor.extensionForContentType("text/plain; charset=utf-8"))
        assertEquals("json", ImportProcessor.extensionForContentType("application/json"))
        assertEquals("srt", ImportProcessor.extensionForContentType("application/x-subrip"))
        assertEquals("wav", ImportProcessor.extensionForContentType("audio/wav"))
    }

    @Test
    fun sharedTextUrlCanBeExtractedAndPlatformDetected() {
        assertEquals(
            "https://v.douyin.com/demo123/",
            PlatformLinkResolver.extractFirstHttpUrl("复制这段内容 https://v.douyin.com/demo123/，打开抖音")
        )
        assertEquals(
            "https://b23.tv/demo?share_source=copy_link",
            PlatformLinkResolver.extractFirstHttpUrl("【哔哩哔哩】 https://b23.tv/demo?share_source=copy_link】更多内容")
        )
        assertEquals(
            "https://youtu.be/abc?si=demo",
            PlatformLinkResolver.extractFirstHttpUrl("Watch later: https://youtu.be/abc?si=demo\u200B)")
        )
        assertEquals(SupportedPlatform.YOUTUBE, PlatformLinkResolver.identifyPlatform("https://youtu.be/abc"))
        assertEquals(SupportedPlatform.YOUTUBE, PlatformLinkResolver.identifyPlatform("https://www.youtube.com/watch?v=abc"))
        assertEquals(SupportedPlatform.BILIBILI, PlatformLinkResolver.identifyPlatform("https://b23.tv/abc"))
        assertEquals(SupportedPlatform.BILIBILI, PlatformLinkResolver.identifyPlatform("https://www.bilibili.com/video/BV1abc"))
        assertEquals(SupportedPlatform.DOUYIN, PlatformLinkResolver.identifyPlatform("https://v.douyin.com/demo123/"))
        assertEquals(SupportedPlatform.DOUYIN, PlatformLinkResolver.identifyPlatform("https://www.iesdouyin.com/share/video/123"))
        assertEquals(SupportedPlatform.DIRECT, PlatformLinkResolver.identifyPlatform("https://example.com/video.mp4"))
    }

    @Test
    fun directHtmlUrlFailsInsteadOfBecomingDocument() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html")
            )

            val error = assertFailsWith<IllegalStateException> {
                PlatformLinkResolver().resolve(server.url("/article").toString())
            }

            assertTrue(error.message.orEmpty().contains("网页"))
        }
    }

    @Test
    fun publicPlatformPageResolvesDirectMediaCandidate() {
        MockWebServer().use { server ->
            server.start()
            val mediaUrl = "http://cdn.example.test:${server.port}/video.mp4"
            val pageUrl = "http://www.youtube.com:${server.port}/watch?v=demo"
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html; charset=utf-8")
                    .setBody(
                        """
                        <html>
                          <head>
                            <meta property="og:title" content="Demo Video">
                            <meta property="og:url" content="https://www.youtube.com/watch?v=demo">
                            <meta property="og:video:secure_url" content="$mediaUrl">
                          </head>
                        </html>
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "video/mp4")
                    .setHeader("Content-Length", "1024")
            )

            val result = PlatformLinkResolver(loopbackClient()).resolve(pageUrl)

            assertEquals(SupportedPlatform.YOUTUBE, result.platform)
            assertEquals(SourceType.VIDEO, result.kind)
            assertEquals(mediaUrl, result.importUrl)
            assertEquals("https://www.youtube.com/watch?v=demo", result.sourceUrl)
            assertEquals("Demo Video", result.title)
        }
    }

    @Test
    fun publicPlatformPageWithoutDirectMediaFailsClearly() {
        MockWebServer().use { server ->
            server.start()
            val pageUrl = "http://www.bilibili.com:${server.port}/video/BV1demo"
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html; charset=utf-8")
                    .setBody("<html><head><title>Bilibili Demo</title></head><body>no direct media</body></html>")
            )

            val error = assertFailsWith<IllegalStateException> {
                PlatformLinkResolver(loopbackClient()).resolve(pageUrl)
            }

            assertTrue(error.message.orEmpty().contains("Bilibili"))
            assertTrue(error.message.orEmpty().contains("没有找到可直接导入的媒体"))
        }
    }

    private fun loopbackClient(): OkHttpClient =
        OkHttpClient.Builder()
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        listOf(InetAddress.getByName("127.0.0.1"))
                }
            )
            .build()
}
