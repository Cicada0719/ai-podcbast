package com.xingyue.english.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.xingyue.english.core.AutoSubtitlePipeline
import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.ImportSource
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.MediaEngine
import com.xingyue.english.core.SourceType
import com.xingyue.english.core.SubtitleParser
import com.xingyue.english.core.TextTools
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

class ImportProcessor(
    private val context: Context,
    private val repository: XingYueRepository,
    private val client: OkHttpClient = BailianLanguageService.defaultClient()
) {
    fun importUri(uri: Uri): ImportedContent {
        val title = displayName(uri)
        val extension = title.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val kind = kindFor(extension, mimeType)
        val id = "content-${System.currentTimeMillis()}"
        val localFile = copyToPrivateFile(uri, id, extension.ifBlank { "bin" })
        val originalText = when (kind) {
            SourceType.SUBTITLE, SourceType.DOCUMENT -> readText(localFile, extension)
            SourceType.AUDIO, SourceType.VIDEO -> ""
        }
        val rawSourceText = if (extension == "json") decodeText(localFile.readBytes()) else originalText

        val content = ImportedContent(
            id = id,
            title = title,
            kind = kind,
            extension = extension,
            sourcePath = localFile.absolutePath,
            importSource = ImportSource.LOCAL_FILE,
            originalText = originalText,
            durationMs = durationFor(localFile, kind),
            status = ImportProcessingStatus.IMPORTED,
            statusMessage = "已导入"
        )
        repository.contentStore.save(content)
        if (extension == "apkg") {
            return ApkgPackageImporter(context, repository).importApkg(content, localFile)
        }
        if (extension == "json") {
            repository.importSourceProjectJson(content, rawSourceText)?.let { return it }
        }
        process(content.id)
        return repository.contentStore.get(content.id) ?: content
    }

    fun importDirectUrl(url: String): ImportedContent {
        val requestedUrl = PlatformLinkResolver.extractFirstHttpUrl(url) ?: url.trim()
        if (PlatformLinkResolver.identifyPlatform(requestedUrl) != PlatformLinkResolver.SupportedPlatform.DIRECT) {
            return importPlatformUrl(requestedUrl)
        }

        val resolved = PlatformLinkResolver(client).resolve(requestedUrl)
        val cleanUrl = resolved.importUrl
        val extension = resolved.extension
        val kind = resolved.kind
        val title = resolved.title
        val id = "content-${System.currentTimeMillis()}"
        val sourcePath: String
        val durationMs: Long
        val originalText: String
        when (kind) {
            SourceType.SUBTITLE, SourceType.DOCUMENT -> {
                sourcePath = cleanUrl
                durationMs = 0L
                originalText = fetchText(cleanUrl)
            }
            SourceType.AUDIO -> {
                if (resolved.contentLength > SHORT_DIRECT_AUDIO_BYTES) {
                    error("直链音频超过 10MB，请改用公开长音频 URL 或压缩后导入")
                }
                val localFile = downloadDirectFile(cleanUrl, id, extension.ifBlank { "audio" }, SHORT_DIRECT_AUDIO_BYTES)
                sourcePath = localFile.absolutePath
                durationMs = durationFor(localFile, kind)
                originalText = ""
            }
            SourceType.VIDEO -> {
                sourcePath = cleanUrl
                durationMs = 0L
                originalText = ""
            }
        }
        val content = ImportedContent(
            id = id,
            title = title,
            kind = kind,
            extension = extension,
            sourcePath = sourcePath,
            sourceUrl = resolved.sourceUrl.ifBlank { cleanUrl },
            importSource = ImportSource.DIRECT_URL,
            originalText = originalText,
            durationMs = durationMs,
            status = ImportProcessingStatus.IMPORTED,
            statusMessage = if (resolved.requestedUrl.startsWith("http://") && resolved.platform == PlatformLinkResolver.SupportedPlatform.DIRECT) {
                "链接已导入。失败时可改用系统文件导入。"
            } else {
                resolved.statusMessage
            }
        )
        repository.contentStore.save(content)
        repository.savePlatformMetadata(content.id, resolved)
        if (extension == "json") {
            repository.importSourceProjectJson(content, originalText)?.let { return it }
        }
        process(content.id)
        return repository.contentStore.get(content.id) ?: content
    }

    private fun importPlatformUrl(url: String): ImportedContent {
        val id = "content-${System.currentTimeMillis()}"
        val result = PlatformLinkResolver(client).resolvePlatformMedia(
            input = url,
            extractor = YtDlpPlatformExtractor(AndroidYtDlpRunner(context))
        )

        return when (result) {
            is PlatformImportResult.Failure -> {
                val content = ImportedContent(
                    id = id,
                    title = "${result.platform.displayName} 链接导入失败",
                    kind = SourceType.VIDEO,
                    extension = "",
                    sourcePath = "",
                    sourceUrl = result.originalUrl,
                    importSource = ImportSource.DIRECT_URL,
                    originalText = "",
                    durationMs = 0L,
                    status = ImportProcessingStatus.FAILED,
                    statusMessage = result.message,
                    progress = 0
                )
                repository.contentStore.save(content)
                repository.savePlatformMetadata(content.id, result)
                content
            }
            is PlatformImportResult.Success -> importResolvedPlatformMedia(id, result)
        }
    }

    private fun importResolvedPlatformMedia(id: String, result: PlatformImportResult.Success): ImportedContent {
        val info = result.mediaInfo
        val selectedMedia = result.selectedMedia
        val kind = when {
            result.nextStep == PlatformImportNextStep.TRANSCRIBE_AUDIO -> SourceType.AUDIO
            selectedMedia?.hasVideo == true -> SourceType.VIDEO
            selectedMedia?.hasAudio == true -> SourceType.AUDIO
            else -> SourceType.VIDEO
        }
        val extension = selectedMedia?.extension
            ?: result.selectedSubtitle?.extension
            ?: if (kind == SourceType.AUDIO) "m4a" else "mp4"
        val content = ImportedContent(
            id = id,
            title = info.title,
            kind = kind,
            extension = extension,
            sourcePath = selectedMedia?.url.orEmpty(),
            sourceUrl = info.canonicalUrl.ifBlank { info.originalUrl },
            importSource = ImportSource.DIRECT_URL,
            originalText = "",
            durationMs = info.durationMs,
            coverPath = info.thumbnailUrl,
            status = ImportProcessingStatus.IMPORTED,
            statusMessage = result.message
        )
        repository.contentStore.save(content)
        repository.savePlatformMetadata(content.id, result)

        return when (result.nextStep) {
            PlatformImportNextStep.IMPORT_SUBTITLE -> importPlatformSubtitle(content, result)
            PlatformImportNextStep.TRANSCRIBE_AUDIO -> {
                process(content.id)
                repository.contentStore.get(content.id) ?: content
            }
        }
    }

    private fun importPlatformSubtitle(
        content: ImportedContent,
        result: PlatformImportResult.Success
    ): ImportedContent {
        val track = result.selectedSubtitle
        if (track == null) {
            repository.contentStore.update(
                content.copy(
                    status = ImportProcessingStatus.FAILED,
                    statusMessage = "没有读取到字幕轨。可重新导入，或配置云端转写。",
                    progress = 0
                )
            )
            return repository.contentStore.get(content.id) ?: content
        }

        repository.contentStore.update(
            content.copy(
                status = ImportProcessingStatus.EXTRACTING_SUBTITLE,
                statusMessage = "下载 ${result.mediaInfo.platform.displayName} 字幕轨",
                progress = 35
            )
        )
        val subtitleText = runCatching { fetchText(track.url) }.getOrElse { error ->
            val failed = content.copy(
                status = ImportProcessingStatus.FAILED,
                statusMessage = "平台字幕下载失败：${error.message ?: "请稍后重试"}",
                progress = 0
            )
            repository.contentStore.update(failed)
            return failed
        }
        val parsed = SubtitleParser.parse("platform.${track.extension.ifBlank { "vtt" }}", subtitleText)
        if (parsed.isEmpty()) {
            val latest = repository.contentStore.get(content.id) ?: content
            repository.contentStore.update(
                latest.copy(
                    status = ImportProcessingStatus.FAILED,
                    statusMessage = "平台字幕为空或格式暂不支持：${track.extension.ifBlank { "unknown" }}",
                    progress = 0
                )
            )
            return repository.contentStore.get(content.id) ?: latest
        }

        val aligned = alignPlatformCues(content, parsed)
        val translated = translateMissingChinese(content, aligned)
        val captionId = "cap-${content.id}"
        repository.captionRepository.save(
            BilingualCaption(
                id = captionId,
                sourceItemId = content.id,
                cues = translated,
                source = CaptionSource.IMPORTED_SUBTITLE
            )
        )
        val wordCount = TextTools.candidateWords(translated).size
        val latest = repository.contentStore.get(content.id) ?: content
        val ready = latest.copy(
            status = ImportProcessingStatus.READY_TO_LEARN,
            statusMessage = if (translated.any { it.chinese.isBlank() }) {
                "字幕已导入。可点选字幕词加入词库。"
            } else {
                "双语字幕已生成。可点选字幕词加入词库。"
            },
            progress = 100,
            captionId = captionId,
            wordCount = wordCount,
            durationMs = content.durationMs,
            coverPath = content.coverPath
        )
        repository.contentStore.update(ready)
        return ready
    }

    fun process(contentId: String): ImportedContent? {
        val pipeline = AutoSubtitlePipeline(
            contentStore = repository.contentStore,
            captionRepository = repository.captionRepository,
            learningWordRepository = repository.learningWordRepository,
            mediaEngine = AndroidMediaEngine,
            languageService = repository.languageService()
        )
        val result = runCatching { pipeline.processImportedItem(contentId) }
        if (result.isFailure) {
            val latest = repository.contentStore.get(contentId)
            if (latest != null && latest.status !in setOf(
                    ImportProcessingStatus.FAILED,
                    ImportProcessingStatus.NEEDS_CLOUD_KEY,
                    ImportProcessingStatus.NEEDS_MEDIA_ENGINE
                )
            ) {
                repository.contentStore.update(
                    latest.copy(
                        status = ImportProcessingStatus.FAILED,
                        statusMessage = "处理失败：${result.exceptionOrNull()?.message ?: "请重试或改用本地文件导入"}",
                        progress = 0
                    )
                )
            }
        }
        return repository.contentStore.get(contentId)
    }

    private fun translateMissingChinese(item: ImportedContent, cues: List<CaptionCue>): List<CaptionCue> {
        val languageService = repository.languageService()
        if (!languageService.hasCloudKey || cues.all { it.chinese.isNotBlank() }) return cues
        val latest = repository.contentStore.get(item.id) ?: item
        repository.contentStore.update(
            latest.copy(
                status = ImportProcessingStatus.TRANSLATING,
                statusMessage = "生成中文字幕",
                progress = 68
            )
        )
        return cues.map { cue ->
            if (cue.chinese.isNotBlank() || cue.english.isBlank() || !TextTools.hasEnglish(cue.english)) {
                cue
            } else {
                cue.copy(chinese = languageService.translateToChinese(cue.english))
            }
        }
    }

    private fun alignPlatformCues(item: ImportedContent, cues: List<CaptionCue>): List<CaptionCue> {
        val duration = item.durationMs
        if (duration <= 1L || cues.isEmpty()) return cues
        val lastEnd = cues.maxOf { it.endMs }
        if (lastEnd <= duration) return cues
        val shouldScale = lastEnd - duration >= TIMELINE_OVERFLOW_GRACE_MS ||
            lastEnd >= (duration.toDouble() * TIMELINE_OVERFLOW_RATIO).roundToLong()
        val ratio = if (shouldScale) duration.toDouble() / lastEnd.toDouble() else 1.0
        return cues.map { cue ->
            val start = (cue.startMs * ratio).roundToLong().coerceAtLeast(0L).coerceAtMost(duration - 1L)
            val end = (cue.endMs * ratio).roundToLong().coerceAtLeast(0L).coerceIn(start + 1L, duration)
            cue.copy(startMs = start, endMs = end)
        }
    }

    private fun copyToPrivateFile(uri: Uri, id: String, extension: String): File {
        val importsDir = File(context.filesDir, "imports").apply { mkdirs() }
        val target = File(importsDir, "$id.$extension")
        val inputStream = runCatching { context.contentResolver.openInputStream(uri) }
            .getOrElse {
                error(if (uri.scheme == "file") "无法读取 file:// 文件，请用系统文件选择器导入" else "无法读取导入文件：请重新授权文件访问")
            }
        inputStream?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error(if (uri.scheme == "file") "无法读取 file:// 文件，请用系统文件选择器导入" else "无法读取导入文件")
        return target
    }

    private fun kindFor(extension: String, mimeType: String): SourceType =
        when {
            extension in setOf("srt", "vtt", "ass", "ssa") -> SourceType.SUBTITLE
            extension in setOf("txt", "pdf", "json", "apkg") || mimeType == "application/pdf" || mimeType.startsWith("text/") -> SourceType.DOCUMENT
            extension in setOf("wav", "pcm", "mp3", "m4a", "aac", "flac") || mimeType.startsWith("audio/") -> SourceType.AUDIO
            else -> SourceType.VIDEO
        }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "导入内容"
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("读取直链失败：${response.code}")
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) error("读取直链失败：文本内容为空")
            return text
        }
    }

    private fun downloadDirectFile(url: String, id: String, extension: String, maxBytes: Long): File {
        val importsDir = File(context.filesDir, "imports").apply { mkdirs() }
        val target = File(importsDir, "$id.$extension")
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载直链音频失败：${response.code}")
            val body = response.body ?: error("下载直链音频失败：响应为空")
            var copied = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > maxBytes) {
                            target.delete()
                            error("直链音频超过 10MB，请改用公开长音频 URL 或压缩后导入")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
        if (target.length() == 0L) {
            target.delete()
            error("下载直链音频失败：文件为空")
        }
        return target
    }

    private fun readText(file: File, extension: String): String =
        when (extension) {
            "pdf" -> extractPdfText(file)
            "json" -> extractJsonText(file.readBytes())
            "apkg" -> extractApkgText(file)
            else -> decodeText(file.readBytes())
        }

    private fun extractPdfText(file: File): String {
        val parsed = runCatching {
            PDFBoxResourceLoader.init(context)
            PDDocument.load(file).use { document ->
                PDFTextStripper()
                    .getText(document)
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
        }.getOrDefault("")
        return parsed.ifBlank { extractBasicPdfText(file.readBytes()) }
    }

    private fun decodeText(bytes: ByteArray): String {
        val utf8 = runCatching { bytes.toString(Charsets.UTF_8) }.getOrDefault("")
        return if ('\uFFFD' in utf8) bytes.toString(Charset.forName("GBK")) else utf8
    }

    private fun extractBasicPdfText(bytes: ByteArray): String {
        val raw = bytes.toString(Charsets.ISO_8859_1)
        val literalText = Regex("\\(([^()]{3,})\\)").findAll(raw)
            .map { it.groupValues[1] }
            .map { it.replace("\\n", " ").replace("\\r", " ") }
            .filter { TextTools.hasEnglish(it) || TextTools.containsCjk(it) }
            .take(160)
            .joinToString(" ")
        return literalText.ifBlank { "PDF 正文需要可提取文本层。请导入带文本层的 PDF，或先导出为 TXT。" }
    }

    private fun extractJsonText(bytes: ByteArray): String {
        val text = decodeText(bytes)
        return runCatching {
            val trimmed = text.trim()
            if (trimmed.startsWith("[")) {
                jsonArrayToText(JSONArray(trimmed))
            } else {
                val json = JSONObject(trimmed)
                when {
                    json.has("cues") -> jsonArrayToText(json.getJSONArray("cues"))
                    json.has("words") -> jsonArrayToText(json.getJSONArray("words"))
                    json.has("items") -> jsonArrayToText(json.getJSONArray("items"))
                    else -> json.keys().asSequence().joinToString("\n") { key -> "${key}: ${json.optString(key)}" }
                }
            }
        }.getOrElse { text }
    }

    private fun jsonArrayToText(array: JSONArray): String =
        (0 until array.length()).joinToString("\n\n") { index ->
            val item = array.opt(index)
            when (item) {
                is JSONObject -> listOf(
                    item.optString("english"),
                    item.optString("text"),
                    item.optString("word"),
                    item.optString("definition"),
                    item.optString("chinese")
                ).filter { it.isNotBlank() }.joinToString("\n")
                else -> item?.toString().orEmpty()
            }
        }

    private fun extractApkgText(file: File): String =
        runCatching {
            val collection = extractApkgCollection(file) ?: return@runCatching ""
            val database = SQLiteDatabase.openDatabase(collection.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            database.use { db ->
                db.rawQuery("SELECT flds FROM notes LIMIT 1000", emptyArray()).use { cursor ->
                    val rows = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        rows += cursor.getString(0)
                            .split('\u001f')
                            .joinToString("\n")
                            .replace(Regex("<[^>]+>"), " ")
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .trim()
                    }
                    rows.filter { it.isNotBlank() }.joinToString("\n\n")
                }
            }
        }.getOrDefault("").ifBlank { "APKG 未读取到可学习文本。" }

    private fun extractApkgCollection(file: File): File? {
        val target = File(context.cacheDir, "${file.nameWithoutExtension}-collection.anki2")
        ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "collection.anki2" || entry.name == "collection.anki21") {
                    target.outputStream().use { output -> zip.copyTo(output) }
                    return target
                }
            }
        }
        return null
    }

    private fun durationFor(file: File, kind: SourceType): Long {
        if (kind != SourceType.AUDIO && kind != SourceType.VIDEO) return 0L
        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        }.getOrDefault(0L)
    }

    companion object {
        private const val SHORT_DIRECT_AUDIO_BYTES = 10L * 1024L * 1024L
        private const val TIMELINE_OVERFLOW_GRACE_MS = 15_000L
        private const val TIMELINE_OVERFLOW_RATIO = 1.10

        fun extensionForContentType(contentType: String): String =
            when (contentType.substringBefore(';').trim().lowercase(Locale.ROOT)) {
                "text/plain" -> "txt"
                "application/json", "text/json" -> "json"
                "application/x-subrip" -> "srt"
                "text/vtt" -> "vtt"
                "audio/wav", "audio/x-wav" -> "wav"
                "audio/mpeg", "audio/mp3" -> "mp3"
                "audio/mp4", "audio/aac" -> "m4a"
                "video/mp4" -> "mp4"
                "application/vnd.apple.mpegurl", "application/x-mpegurl", "audio/mpegurl" -> "m3u8"
                "video/x-matroska" -> "mkv"
                else -> ""
            }

        fun directKindFor(extension: String, contentType: String = ""): SourceType {
            val normalizedExtension = extension.lowercase(Locale.ROOT)
            val normalizedType = contentType.substringBefore(';').trim().lowercase(Locale.ROOT)
            return when {
                PlatformLinkResolver.isHtmlContentType(normalizedType) -> error("该链接返回网页（text/html），不能当作文本直链导入")
                normalizedExtension in setOf("srt", "vtt", "ass", "ssa") || normalizedType in setOf("application/x-subrip", "text/vtt") -> SourceType.SUBTITLE
                normalizedExtension in setOf("txt", "json") || normalizedType in setOf("text/plain", "text/markdown", "text/csv") || normalizedType == "application/json" -> SourceType.DOCUMENT
                normalizedExtension in setOf("wav", "pcm", "mp3", "m4a", "aac", "flac") || normalizedType.startsWith("audio/") -> SourceType.AUDIO
                normalizedExtension in setOf("mp4", "mkv", "mov", "webm", "m4v", "m3u8") || normalizedType.startsWith("video/") || normalizedType in setOf("application/vnd.apple.mpegurl", "application/x-mpegurl") -> SourceType.VIDEO
                normalizedExtension in setOf("pdf", "apkg") -> error("${normalizedExtension.uppercase(Locale.ROOT)} 直链暂不支持，请用系统文件选择器导入以保留文件权限和解析边界")
                else -> error("不支持的直链格式，请提供 SRT/TXT/JSON、10MB 内音频或常见视频直链")
            }
        }
    }
}

internal object AndroidMediaEngine : MediaEngine {
    override val available: Boolean = true
    override val version: String = "FFmpegKit 6.0.1"

    override fun extractSubtitleTrack(input: String, output: String): String? {
        val out = resolvedOutput(input, output)
        out.parentFile?.mkdirs()
        out.delete()
        val ok = execute(SUBTITLE_COMMAND_TIMEOUT_SECONDS, "-y", "-nostdin", "-hide_banner", "-loglevel", "error", "-i", input, "-map", "0:s:0", "-c:s", "srt", out.absolutePath)
        return if (ok && out.exists() && out.length() > 0) out.readText() else null
    }

    override fun extractAudioForAsr(input: String, output: String): String? {
        val out = resolvedOutput(input, output)
        out.parentFile?.mkdirs()
        val ok = execute(MEDIA_COMMAND_TIMEOUT_SECONDS, "-y", "-nostdin", "-hide_banner", "-loglevel", "error", "-i", input, "-t", "600", "-vn", "-ac", "1", "-ar", "16000", "-f", "wav", out.absolutePath)
        return if (ok && out.exists() && out.length() > 0) out.absolutePath else null
    }

    private fun execute(@Suppress("UNUSED_PARAMETER") timeoutSeconds: Long, vararg args: String): Boolean {
        val completed = CountDownLatch(1)
        val success = AtomicReference(false)
        val session = FFmpegKit.executeAsync(args.joinToString(" ") { it.ffmpegQuote() }) { finished ->
            success.set(ReturnCode.isSuccess(finished.returnCode))
            completed.countDown()
        }
        return runCatching {
            if (!completed.await(timeoutSeconds.coerceAtLeast(1L), TimeUnit.SECONDS)) {
                FFmpegKit.cancel(session.sessionId)
                false
            } else {
                success.get()
            }
        }.getOrDefault(false)
    }

    private fun resolvedOutput(input: String, output: String): File {
        val outputFile = File(output)
        if (outputFile.isAbsolute) return outputFile
        val sourceDir = File(input).parentFile ?: File(".")
        return File(sourceDir, output)
    }

    private fun String.ffmpegQuote(): String =
        if (any { it.isWhitespace() || it == '"' }) "\"${replace("\"", "\\\"")}\"" else this

    private const val SUBTITLE_COMMAND_TIMEOUT_SECONDS = 30L
    private const val MEDIA_COMMAND_TIMEOUT_SECONDS = 90L
}
