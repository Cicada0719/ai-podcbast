package com.xingyue.english.android.data

import com.xingyue.english.core.SourceType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.Locale

class PlatformLinkResolver(
    private val client: OkHttpClient = BailianLanguageService.defaultClient()
) {
    internal fun resolvePlatformMedia(input: String, extractor: PlatformMediaExtractor): PlatformImportResult {
        val requestedUrl = extractFirstHttpUrl(input) ?: input.trim()
        val platform = identifyPlatform(requestedUrl)
        if (platform == SupportedPlatform.DIRECT) {
            return PlatformImportResult.Failure(
                platform = platform,
                originalUrl = requestedUrl,
                reason = PlatformImportFailure.UNSUPPORTED_URL,
                message = "该链接不是支持的视频平台链接，请走直链导入。"
            )
        }
        return extractor.extract(requestedUrl)
    }

    fun resolve(input: String): LinkResolution {
        val requestedUrl = extractFirstHttpUrl(input) ?: input.trim()
        val uri = parseHttpUri(requestedUrl)
        val platform = identifyPlatform(requestedUrl)
        val probe = runCatching { probe(requestedUrl, strict = platform == SupportedPlatform.DIRECT) }
            .getOrElse {
                if (platform == SupportedPlatform.DIRECT) throw it else UrlProbe()
            }

        directResolution(requestedUrl, requestedUrl, platform, probe)?.let { return it }

        if (platform != SupportedPlatform.DIRECT) {
            return resolvePublicPlatformPage(requestedUrl, platform)
        }

        if (isHtmlContentType(probe.contentType)) {
            error("该链接是网页，不是文件直链。请提供音视频、字幕或文本文件链接。")
        }

        val host = uri.host.orEmpty()
        error("不支持的链接格式：$host 未返回可识别的音频、视频、字幕或文本文件。")
    }

    private fun resolvePublicPlatformPage(url: String, platform: SupportedPlatform): LinkResolution {
        val html = fetchPublicHtml(url, platform)
        val metadata = parsePublicPageMetadata(url, html)
        val sourceUrl = metadata.canonicalUrl.ifBlank { url }
        val title = metadata.title.ifBlank { "${platform.displayName} 视频" }

        metadata.mediaCandidates.forEach { candidate ->
            val candidateProbe = probe(candidate, strict = false)
            directResolution(
                requestedUrl = candidate,
                sourceUrl = sourceUrl,
                platform = platform,
                probe = candidateProbe,
                titleOverride = title,
                statusMessage = "已识别 ${platform.displayName} 链接"
            )?.let { return it }
        }

        error("已识别 ${platform.displayName} 链接，但没有找到可直接导入的媒体。可下载后通过系统文件导入。")
    }

    private fun directResolution(
        requestedUrl: String,
        sourceUrl: String,
        platform: SupportedPlatform,
        probe: UrlProbe,
        titleOverride: String? = null,
        statusMessage: String? = null
    ): LinkResolution? {
        if (isHtmlContentType(probe.contentType)) return null

        val extension = extensionFromUrl(requestedUrl).ifBlank {
            ImportProcessor.extensionForContentType(probe.contentType)
        }
        val kind = runCatching { ImportProcessor.directKindFor(extension, probe.contentType) }.getOrNull()
            ?: return null
        return LinkResolution(
            requestedUrl = requestedUrl,
            importUrl = requestedUrl,
            sourceUrl = sourceUrl,
            platform = platform,
            title = titleOverride?.takeIf { it.isNotBlank() } ?: titleFromUrl(requestedUrl, extension, platform),
            extension = extension,
            contentType = probe.contentType,
            contentLength = probe.contentLength,
            kind = kind,
            statusMessage = statusMessage ?: if (platform == SupportedPlatform.DIRECT) "已导入直链" else "已导入 ${platform.displayName} 直链"
        )
    }

    private fun fetchPublicHtml(url: String, platform: SupportedPlatform): String {
        val request = browserRequest(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("读取 ${platform.displayName} 链接失败：${response.code}")
            }
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) error("读取 ${platform.displayName} 链接失败：页面为空")
            return text
        }
    }

    private fun probe(url: String, strict: Boolean): UrlProbe {
        val request = browserRequest(url).head().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (!strict && response.code in setOf(403, 405)) {
                        return@use UrlProbe(statusCode = response.code)
                    }
                    error("链接预检失败：${response.code}")
                }
                UrlProbe(
                    contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT),
                    contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L,
                    statusCode = response.code
                )
            }
        }.getOrElse { error ->
            if (strict) throw error else UrlProbe()
        }
    }

    private fun browserRequest(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,video/*;q=0.8,audio/*;q=0.8,*/*;q=0.7")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

    data class LinkResolution(
        val requestedUrl: String,
        val importUrl: String,
        val sourceUrl: String,
        val platform: SupportedPlatform,
        val title: String,
        val extension: String,
        val contentType: String,
        val contentLength: Long,
        val kind: SourceType,
        val statusMessage: String
    )

    data class PublicPageMetadata(
        val title: String = "",
        val canonicalUrl: String = "",
        val mediaCandidates: List<String> = emptyList()
    )

    private data class UrlProbe(
        val contentType: String = "",
        val contentLength: Long = -1L,
        val statusCode: Int = 0
    )

    enum class SupportedPlatform(val displayName: String) {
        DIRECT("直链"),
        YOUTUBE("YouTube"),
        BILIBILI("Bilibili"),
        DOUYIN("Douyin")
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; XingYueEnglish) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
        private val URL_REGEX = Regex("""https?://[^\s<>"'，。！？；：、）】]+""", RegexOption.IGNORE_CASE)
        private val META_TAG_REGEX = Regex("""<meta\b[^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val LINK_TAG_REGEX = Regex("""<link\b[^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val TITLE_REGEX = Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val ATTR_REGEX = Regex("""([a-zA-Z_:.-]+)\s*=\s*(['"])(.*?)\2""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val EMBEDDED_URL_REGEX = Regex("""https?(?::|\\u003a)(?:\\?/\\?/|//)[^"'<>\s]+""", RegexOption.IGNORE_CASE)
        private val MEDIA_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv", "m3u8", "mp3", "m4a", "aac", "wav", "flac", "srt", "vtt")
        private val MEDIA_META_KEYS = setOf(
            "og:video",
            "og:video:url",
            "og:video:secure_url",
            "og:audio",
            "og:audio:url",
            "og:audio:secure_url",
            "twitter:player:stream"
        )

        fun extractFirstHttpUrl(input: String): String? =
            URL_REGEX.find(input.removeInvisibleUrlChars().trim())?.value?.let(::sanitizeUrl)

        fun identifyPlatform(url: String): SupportedPlatform {
            val host = runCatching { URI(url).host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
                .removePrefix("www.")
                .removePrefix("m.")
            return when {
                host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") || host.endsWith(".youtube-nocookie.com") -> SupportedPlatform.YOUTUBE
                host == "b23.tv" || host == "bilibili.com" || host.endsWith(".bilibili.com") -> SupportedPlatform.BILIBILI
                host == "douyin.com" || host.endsWith(".douyin.com") || host == "iesdouyin.com" || host.endsWith(".iesdouyin.com") -> SupportedPlatform.DOUYIN
                else -> SupportedPlatform.DIRECT
            }
        }

        fun isHtmlContentType(contentType: String): Boolean {
            val normalized = contentType.substringBefore(';').trim().lowercase(Locale.ROOT)
            return normalized == "text/html" || normalized == "application/xhtml+xml"
        }

        fun parsePublicPageMetadata(pageUrl: String, html: String): PublicPageMetadata {
            val metas = META_TAG_REGEX.findAll(html)
                .map { attributesOf(it.value) }
                .toList()
            val metaByKey = metas.mapNotNull { attrs ->
                val key = attrs["property"] ?: attrs["name"] ?: attrs["itemprop"] ?: return@mapNotNull null
                val content = attrs["content"] ?: return@mapNotNull null
                key.lowercase(Locale.ROOT) to htmlDecode(content)
            }

            val title = firstMeta(metaByKey, "og:title", "twitter:title", "title")
                .ifBlank {
                    TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)?.let(::htmlDecode).orEmpty()
                }
                .normalizeWhitespace()
            val canonicalUrl = firstMeta(metaByKey, "og:url")
                .ifBlank { canonicalLink(html) }
                .let { makeAbsoluteUrl(pageUrl, it).orEmpty() }

            val metaCandidates = metaByKey
                .filter { it.first in MEDIA_META_KEYS }
                .mapNotNull { makeAbsoluteUrl(pageUrl, decodeEscapedUrl(it.second)) }
            val embeddedCandidates = EMBEDDED_URL_REGEX.findAll(html)
                .map { decodeEscapedUrl(it.value) }
                .mapNotNull { makeAbsoluteUrl(pageUrl, it) }
                .filter { isProbablyMediaUrl(it) }

            return PublicPageMetadata(
                title = title,
                canonicalUrl = canonicalUrl,
                mediaCandidates = (metaCandidates + embeddedCandidates)
                    .map(::sanitizeUrl)
                    .filter { isProbablyMediaUrl(it) }
                    .distinct()
            )
        }

        fun extensionFromUrl(url: String): String {
            val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
            val name = path.substringAfterLast('/')
            val extension = name.substringAfterLast('.', "")
            return extension.lowercase(Locale.ROOT).takeIf { it != name.lowercase(Locale.ROOT) }.orEmpty()
        }

        private fun parseHttpUri(url: String): URI {
            val uri = runCatching { URI(url) }.getOrElse { error("请输入有效链接") }
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            require(scheme == "http" || scheme == "https") { "请输入 http:// 或 https:// 开头的链接" }
            require(!uri.host.isNullOrBlank()) { "链接需要包含有效主机名" }
            return uri
        }

        private fun titleFromUrl(url: String, extension: String, platform: SupportedPlatform): String {
            val rawName = runCatching { URI(url).path.orEmpty().substringAfterLast('/') }.getOrDefault("")
            val decoded = runCatching { URLDecoder.decode(rawName, Charsets.UTF_8.name()) }.getOrDefault(rawName)
            val fallback = if (platform == SupportedPlatform.DIRECT) "外链学习材料" else "${platform.displayName} 学习材料"
            if (decoded.isBlank()) return fallback
            return if (decoded.contains('.') || extension.isBlank()) decoded else "$decoded.$extension"
        }

        private fun sanitizeUrl(url: String): String =
            htmlDecode(url)
                .removeInvisibleUrlChars()
                .trim()
                .trimEnd(
                '.', ',', ';', ':', '!', '?',
                '。', '，', '；', '：', '！', '？', '、',
                ')', '）', ']', '】', '}', '>'
            )

        private fun firstMeta(values: List<Pair<String, String>>, vararg keys: String): String =
            keys.firstNotNullOfOrNull { key ->
                values.firstOrNull { it.first == key }?.second?.takeIf { value -> value.isNotBlank() }
            }.orEmpty()

        private fun canonicalLink(html: String): String =
            LINK_TAG_REGEX.findAll(html)
                .map { attributesOf(it.value) }
                .firstOrNull { attrs -> attrs["rel"]?.contains("canonical", ignoreCase = true) == true }
                ?.get("href")
                ?.let(::htmlDecode)
                .orEmpty()

        private fun attributesOf(tag: String): Map<String, String> =
            ATTR_REGEX.findAll(tag).associate { match ->
                match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[3]
            }

        private fun htmlDecode(value: String): String =
            value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

        private fun decodeEscapedUrl(value: String): String =
            htmlDecode(value)
                .replace("\\/", "/")
                .replace("\\u0026", "&", ignoreCase = true)
                .replace("\\u003d", "=", ignoreCase = true)
                .replace("\\u003f", "?", ignoreCase = true)
                .replace("\\u003a", ":", ignoreCase = true)

        private fun makeAbsoluteUrl(baseUrl: String, candidate: String): String? {
            val clean = candidate.trim()
            if (clean.isBlank()) return null
            val base = runCatching { URI(baseUrl) }.getOrNull() ?: return null
            return when {
                clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true) -> clean
                clean.startsWith("//") -> "${base.scheme}:$clean"
                clean.startsWith("/") -> "${base.scheme}://${base.host}$clean"
                else -> null
            }
        }

        private fun isProbablyMediaUrl(url: String): Boolean =
            extensionFromUrl(url) in MEDIA_EXTENSIONS

        private fun String.normalizeWhitespace(): String =
            replace(Regex("\\s+"), " ").trim()

        private fun String.removeInvisibleUrlChars(): String =
            replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
    }
}
