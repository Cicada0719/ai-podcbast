package com.xingyue.english.android.data

import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CloudLanguageService
import com.xingyue.english.core.SubtitleParser
import com.xingyue.english.core.WordDefinition
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class BailianLanguageService(
    private val apiKey: String,
    private val region: BailianRegion,
    private val client: OkHttpClient = defaultClient(),
    private val compatibleChatEndpoint: String = region.compatibleChatEndpoint,
    private val dashScopeBaseUrl: String = region.dashScopeBaseUrl,
    private val fileTransPollIntervalMs: Long = 2_000L,
    private val fileTransMaxPolls: Int = 30
) : CloudLanguageService {
    override val hasCloudKey: Boolean = apiKey.isNotBlank()

    override fun transcribeAudio(audioPath: String): List<CaptionCue> {
        require(hasCloudKey) { "需要先配置云端转写" }
        require(region.asrSupported) { "${region.label} 当前未启用本地音频识别，请切换中国内地或国际区域" }

        return if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
            transcribeByFileTrans(audioPath)
        } else {
            val file = File(audioPath)
            require(file.exists()) { "音频文件不存在" }
            require(file.length() <= SHORT_AUDIO_LIMIT_BYTES) {
                "超过 10MB 的本地音频需要先上传到可访问地址后使用长音频识别"
            }
            transcribeShortAudio(file)
        }
    }

    override fun translateToChinese(english: String): String {
        if (!hasCloudKey || english.isBlank()) return ""
        return runCatching {
            chat(
                system = "你是专业字幕翻译引擎。必须只输出自然、简洁、适合字幕显示的简体中文译文；不要英文原文，不要解释，不要 Markdown。",
                user = "Translate to Simplified Chinese only:\n${english.take(4000)}"
            )
        }.getOrDefault("")
    }

    override fun lookup(word: String): WordDefinition {
        if (!hasCloudKey) return WordDefinition(word = word, chinese = fallbackDefinition(word))
        return runCatching {
            val raw = chat(
                system = "你是严格 JSON API。只输出 JSON 对象，不要 Markdown，不要解释。schema: {\"word\":\"\",\"phonetic\":\"\",\"chinese\":\"\"}。chinese 使用简短简体中文释义。",
                user = "word=$word",
                jsonObject = true
            )
            val json = JSONObject(extractJsonObject(raw))
            WordDefinition(
                word = json.optString("word", word),
                phonetic = json.optString("phonetic"),
                chinese = json.optString("chinese", fallbackDefinition(word))
            )
        }.getOrElse {
            WordDefinition(word = word, chinese = fallbackDefinition(word))
        }
    }

    fun testConnection(): CloudCheckResult {
        if (!hasCloudKey) {
            return CloudCheckResult(false, "未填写访问密钥")
        }
        return runCatching {
            val reply = chat(system = "只返回 OK。", user = "ping")
            val message = if (reply.contains("OK", ignoreCase = true)) {
                "连接正常：云端转写可用"
            } else {
                "连接正常：云端服务可用"
            }
            CloudCheckResult(
                success = true,
                message = message,
                checkedCapabilities = listOf("qwen-plus", region.label)
            )
        }.getOrElse { error ->
            CloudCheckResult(
                success = false,
                message = "连接失败：${error.message ?: "请检查密钥、网络或区域"}",
                checkedCapabilities = listOf(region.label)
            )
        }
    }

    private fun transcribeShortAudio(file: File): List<CaptionCue> {
        val base64Audio = Base64.getEncoder().encodeToString(file.readBytes())
        val dataUrl = "data:${mimeFor(file.name)};base64,$base64Audio"
        return runCatching { transcribeShortAudioCompatible(dataUrl) }
            .getOrElse { compatibleError ->
                runCatching { transcribeShortAudioDashScope(base64Audio) }
                    .getOrElse { dashScopeError ->
                        val message = dashScopeError.message ?: compatibleError.message ?: "请稍后重试"
                        error(message)
                    }
            }
    }

    private fun transcribeShortAudioCompatible(dataUrl: String): List<CaptionCue> {
        val payload = JSONObject()
            .put("model", asrModelName())
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray().put(
                                JSONObject()
                                    .put("type", "input_audio")
                                    .put("input_audio", JSONObject().put("data", dataUrl))
                            )
                        )
                )
            )
            .put("stream", false)
            .put("asr_options", JSONObject().put("language", "en").put("enable_itn", true))

        val body = postJson(compatibleChatEndpoint, payload)
        val text = JSONObject(body)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content")
        return textToCues(text)
    }

    private fun transcribeShortAudioDashScope(base64Audio: String): List<CaptionCue> {
        val payload = JSONObject()
            .put("model", asrModelName())
            .put(
                "input",
                JSONObject().put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("role", "system")
                                .put("content", JSONArray().put(JSONObject().put("text", "")))
                        )
                        .put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", JSONArray().put(JSONObject().put("audio", base64Audio)))
                        )
                )
            )
            .put("parameters", JSONObject().put("asr_options", JSONObject().put("language", "en").put("enable_itn", true)))

        val body = postJson("$dashScopeBaseUrl/services/aigc/multimodal-generation/generation", payload)
        val content = JSONObject(body)
            .getJSONObject("output")
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getJSONArray("content")

        val text = buildString {
            for (index in 0 until content.length()) {
                val value = content.getJSONObject(index).optString("text")
                if (value.isNotBlank()) {
                    if (isNotEmpty()) append('\n')
                    append(value)
                }
            }
        }
        return textToCues(text)
    }

    private fun transcribeByFileTrans(fileUrl: String): List<CaptionCue> {
        val submitPayload = JSONObject()
            .put("model", "qwen3-asr-flash-filetrans")
            .put("input", JSONObject().put("file_url", fileUrl))
            .put("parameters", JSONObject().put("channel_id", JSONArray().put(0)).put("language", "en").put("enable_itn", true))
        val submit = JSONObject(
            postJson(
                url = "$dashScopeBaseUrl/services/audio/asr/transcription",
                payload = submitPayload,
                async = true
            )
        )
        val taskId = submit.getJSONObject("output").getString("task_id")
        repeat(fileTransMaxPolls.coerceAtLeast(1)) {
            if (fileTransPollIntervalMs > 0) Thread.sleep(fileTransPollIntervalMs)
            val task = JSONObject(getJson("$dashScopeBaseUrl/tasks/$taskId", async = true))
            val output = task.getJSONObject("output")
            when (output.optString("task_status")) {
                "SUCCEEDED" -> {
                    val resultUrl = output.getJSONObject("result").getString("transcription_url")
                    return parseTranscriptionResult(getJson(resultUrl, authorized = false))
                }
                "FAILED" -> error(output.optString("message", "语音识别失败"))
            }
        }
        error("语音识别超时")
    }

    private fun parseTranscriptionResult(rawJson: String): List<CaptionCue> {
        val root = JSONObject(rawJson)
        val transcripts = root.optJSONArray("transcripts") ?: return emptyList()
        val cues = mutableListOf<CaptionCue>()
        for (trackIndex in 0 until transcripts.length()) {
            val sentences = transcripts.getJSONObject(trackIndex).optJSONArray("sentences") ?: continue
            for (sentenceIndex in 0 until sentences.length()) {
                val sentence = sentences.getJSONObject(sentenceIndex)
                val text = sentence.optString("text")
                if (text.isNotBlank()) {
                    cues += CaptionCue(
                        id = "asr-$trackIndex-$sentenceIndex",
                        startMs = sentence.optLong("begin_time"),
                        endMs = sentence.optLong("end_time"),
                        english = text
                    )
                }
            }
        }
        return cues
    }

    private fun textToCues(text: String): List<CaptionCue> =
        runCatching { SubtitleParser.parse("asr.srt", text) }
            .getOrDefault(emptyList())
            .ifEmpty { SubtitleParser.fromPlainText(text) }

    private fun asrModelName(): String =
        if (region == BailianRegion.US) "qwen3-asr-flash-us" else "qwen3-asr-flash"

    private fun chat(system: String, user: String, jsonObject: Boolean = false): String {
        val payload = JSONObject()
            .put("model", "qwen-plus")
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", user))
            )
        if (jsonObject) {
            payload.put("response_format", JSONObject().put("type", "json_object"))
        }
        return JSONObject(postJson(compatibleChatEndpoint, payload))
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content")
            .trim()
    }

    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "词典返回不是 JSON" }
        return raw.substring(start, end + 1)
    }

    private fun postJson(url: String, payload: JSONObject, async: Boolean = false): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
        if (async) requestBuilder.addHeader("X-DashScope-Async", "enable")
        val request = requestBuilder.build()
        var lastError: IOException? = null
        repeat(2) { attempt ->
            try {
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("云端请求失败：${response.code} $text")
                    return text
                }
            } catch (error: IOException) {
                lastError = error
                if (attempt == 0) Thread.sleep(600)
            }
        }
        throw IOException("网络连接中断，请稍后重试", lastError)
    }

    private fun getJson(url: String, async: Boolean = false, authorized: Boolean = true): String {
        val requestBuilder = Request.Builder().url(url).get()
        if (authorized) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        if (async) requestBuilder.addHeader("X-DashScope-Async", "enable")
        client.newCall(requestBuilder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("云端请求失败：${response.code} $text")
            return text
        }
    }

    private fun mimeFor(name: String): String =
        when (name.substringAfterLast('.', "").lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "pcm" -> "audio/L16"
            else -> "application/octet-stream"
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val SHORT_AUDIO_LIMIT_BYTES = 10L * 1024L * 1024L

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

        fun fallbackDefinition(word: String): String =
            when (word.lowercase()) {
                "context" -> "语境；背景"
                "practice" -> "练习；实践"
                "memory" -> "记忆"
                "listen", "listening" -> "听；听力"
                "repeat", "repetition" -> "重复；复现"
                "durable" -> "持久的"
                "curiosity" -> "好奇心"
                "reflection" -> "复盘；反思"
                "subtitle", "subtitles" -> "字幕"
                "learning" -> "学习"
                else -> "待补全释义"
            }
    }
}
