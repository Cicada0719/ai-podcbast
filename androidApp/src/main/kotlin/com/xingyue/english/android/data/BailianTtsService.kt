package com.xingyue.english.android.data

import com.xingyue.english.core.SpeechSynthesisResult
import com.xingyue.english.core.SpeechSynthesisService
import com.xingyue.english.core.TtsVoice
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object BailianTtsVoices {
    val voices = listOf(
        TtsVoice("longanyang", "龙安洋", "阳光男声，中英可用"),
        TtsVoice("longanhuan", "龙安欢", "元气女声，中英可用"),
        TtsVoice("longhuhu_v3", "龙呼呼", "清亮童声，中英可用"),
        TtsVoice("longpaopao_v3", "龙泡泡", "童话朗读声，中英可用"),
        TtsVoice("longjielidou_v3", "龙杰力豆", "明亮童声，中英可用"),
        TtsVoice("longxian_v3", "龙仙", "活泼女声，中英可用"),
        TtsVoice("longling_v3", "龙铃", "柔和童声，中英可用"),
        TtsVoice("longshanshan_v3", "龙闪闪", "戏剧化童声，中英可用"),
        TtsVoice("longniuniu_v3", "龙牛牛", "阳光童声，中英可用"),
        TtsVoice("longxiaochun_v3", "龙小淳", "知性女声，中英可用"),
        TtsVoice("longxiaoxia_v3", "龙小夏", "沉稳女声，中英可用"),
        TtsVoice("longcheng_v3", "龙橙", "青年男声，中英可用"),
        TtsVoice("longze_v3", "龙泽", "温暖男声，中英可用"),
        TtsVoice("longyan_v3", "龙颜", "温柔女声，中英可用"),
        TtsVoice("longxing_v3", "龙星", "邻家女声，中英可用"),
        TtsVoice("longshuo_v3", "龙硕", "干练男声，中英可用"),
        TtsVoice("longshu_v3", "龙书", "沉稳男声，中英可用"),
        TtsVoice("longwanjun_v3", "龙婉君", "细腻女声，中英可用"),
        TtsVoice("longyichen_v3", "龙逸尘", "活力男声，中英可用"),
        TtsVoice("loongabby_v3", "Abby", "美式英文女声"),
        TtsVoice("loongandy_v3", "Andy", "美式英文男声"),
        TtsVoice("loongannie_v3", "Annie", "美式英文女声"),
        TtsVoice("loongava_v3", "Ava", "美式英文女声"),
        TtsVoice("loongbeth_v3", "Beth", "美式英文女声")
    )

    val defaultVoice: TtsVoice = voices.first()

    fun find(id: String): TtsVoice =
        voices.firstOrNull { it.id == id } ?: defaultVoice
}

class BailianTtsService(
    private val apiKey: String,
    private val region: BailianRegion,
    private val cacheDir: File,
    private val client: OkHttpClient = BailianLanguageService.defaultClient(),
    private val restEndpoint: String = region.ttsRestEndpoint,
    private val webSocketEndpoint: String = region.ttsWebSocketEndpoint
) : SpeechSynthesisService {
    override val available: Boolean = apiKey.isNotBlank()

    override fun synthesize(text: String, voiceId: String, speed: Float, volume: Float): SpeechSynthesisResult {
        require(available) { "需要先配置云端发音" }
        require(text.isNotBlank()) { "发音文本为空" }

        cacheDir.mkdirs()
        val voice = BailianTtsVoices.find(voiceId)
        val target = cacheFile(cacheDir, text, voice.id, speed, volume)
        if (target.exists() && target.length() > 0) {
            return SpeechSynthesisResult(text, voice.id, target.absolutePath, cached = true)
        }

        val restFailure = runCatching {
            synthesizeByRest(text, voice.id, speed, volume, target)
            return SpeechSynthesisResult(text, voice.id, target.absolutePath, cached = false)
        }.exceptionOrNull()
        target.delete()

        val wsFailure = runCatching {
            synthesizeByWebSocket(text, voice.id, speed, volume, target)
            return SpeechSynthesisResult(text, voice.id, target.absolutePath, cached = false)
        }.exceptionOrNull()
        target.delete()

        val detail = listOfNotNull(restFailure?.message, wsFailure?.message)
            .joinToString("；")
            .take(220)
            .ifBlank { "请稍后重试" }
        throw IllegalStateException("云端发音失败：$detail")
    }

    private fun synthesizeByRest(text: String, voiceId: String, speed: Float, volume: Float, target: File) {
        val payload = JSONObject()
            .put("model", MODEL)
            .put(
                "input",
                JSONObject()
                    .put("text", text)
                    .put("voice", voiceId)
                    .put("format", "mp3")
                    .put("sample_rate", SAMPLE_RATE)
                    .put("volume", volumeValue(volume))
                    .put("rate", rateValue(speed))
                    .put("language_hints", JSONArray().put("en"))
            )

        val request = Request.Builder()
            .url(restEndpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        val body = client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("服务返回 ${response.code}：${raw.take(120)}")
            raw
        }

        val audio = JSONObject(body)
            .optJSONObject("output")
            ?.optJSONObject("audio")
            ?: error("没有返回音频")
        val inlineData = audio.optString("data")
        val audioUrl = audio.optString("url")
        when {
            inlineData.isNotBlank() -> {
                target.parentFile?.mkdirs()
                target.writeBytes(Base64.getDecoder().decode(inlineData))
            }
            audioUrl.isNotBlank() -> downloadAudio(audioUrl, target)
            else -> error("没有返回音频地址")
        }
        require(target.exists() && target.length() > 0) { "没有生成有效音频" }
    }

    private fun downloadAudio(audioUrl: String, target: File) {
        val request = Request.Builder().url(audioUrl).get().build()
        target.parentFile?.mkdirs()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载音频失败：${response.code}")
            val body = response.body ?: error("下载音频失败：响应为空")
            target.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
    }

    private fun synthesizeByWebSocket(text: String, voiceId: String, speed: Float, volume: Float, target: File) {
        target.parentFile?.mkdirs()
        val taskId = UUID.randomUUID().toString()
        val latch = CountDownLatch(1)
        var failure: Throwable? = null
        var socket: WebSocket? = null

        target.outputStream().use { output ->
            val request = Request.Builder()
                .url(webSocketEndpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("X-DashScope-DataInspection", "enable")
                .build()
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(runTaskPayload(taskId, voiceId, speed, volume).toString())
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    output.write(bytes.toByteArray())
                }

                override fun onMessage(webSocket: WebSocket, textMessage: String) {
                    val json = runCatching { JSONObject(textMessage) }.getOrNull()
                    val header = json?.optJSONObject("header")
                    val event = header?.optString("event").orEmpty()
                    val code = header?.optString("error_code").orEmpty()
                    if (code.isNotBlank()) {
                        failure = IllegalStateException(header?.optString("error_message").orEmpty().ifBlank { code })
                        webSocket.close(1000, "error")
                        latch.countDown()
                        return
                    }
                    when (event) {
                        "task-started" -> {
                            webSocket.send(continueTaskPayload(taskId, text).toString())
                            webSocket.send(finishTaskPayload(taskId).toString())
                        }
                        "task-failed", "error" -> {
                            failure = IllegalStateException(header?.optString("error_message").orEmpty().ifBlank { "语音合成失败" })
                            webSocket.close(1000, "failed")
                            latch.countDown()
                        }
                        "task-finished" -> {
                            webSocket.close(1000, "done")
                            latch.countDown()
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    failure = t
                    latch.countDown()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    latch.countDown()
                }
            }
            socket = client.newWebSocket(request, listener)
            if (!latch.await(45, TimeUnit.SECONDS)) {
                socket?.cancel()
                failure = IOException("语音合成超时")
            }
        }

        failure?.let { throw it }
        require(target.exists() && target.length() > 0) { "没有生成有效音频" }
    }

    private fun runTaskPayload(taskId: String, voiceId: String, speed: Float, volume: Float): JSONObject =
        JSONObject()
            .put("header", JSONObject().put("action", "run-task").put("task_id", taskId).put("streaming", "duplex"))
            .put(
                "payload",
                JSONObject()
                    .put("task_group", "audio")
                    .put("task", "tts")
                    .put("function", "SpeechSynthesizer")
                    .put("model", MODEL)
                    .put(
                        "parameters",
                        JSONObject()
                            .put("text_type", "PlainText")
                            .put("voice", voiceId)
                            .put("format", "mp3")
                            .put("sample_rate", SAMPLE_RATE)
                            .put("rate", rateValue(speed))
                            .put("volume", volumeValue(volume))
                            .put("pitch", 1.0)
                            .put("enable_ssml", false)
                    )
                    .put("input", JSONObject())
            )

    private fun continueTaskPayload(taskId: String, text: String): JSONObject =
        JSONObject()
            .put("header", JSONObject().put("action", "continue-task").put("task_id", taskId).put("streaming", "duplex"))
            .put("payload", JSONObject().put("input", JSONObject().put("text", text)))

    private fun finishTaskPayload(taskId: String): JSONObject =
        JSONObject()
            .put("header", JSONObject().put("action", "finish-task").put("task_id", taskId).put("streaming", "duplex"))
            .put("payload", JSONObject().put("input", JSONObject()))

    private fun rateValue(speed: Float): Float =
        speed.coerceIn(0.5f, 2.0f)

    private fun volumeValue(volume: Float): Int =
        (volume.coerceIn(0.1f, 2.0f) * 50).toInt().coerceIn(1, 100)

    companion object {
        private const val MODEL = "cosyvoice-v3-flash"
        private const val SAMPLE_RATE = 24000
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun cacheFile(cacheDir: File, text: String, voiceId: String, speed: Float, volume: Float): File =
            File(cacheDir, "${cacheKey(text, voiceId, speed, volume)}.mp3")

        fun cacheKey(text: String, voiceId: String, speed: Float, volume: Float): String {
            val raw = "${BailianTtsVoices.find(voiceId).id}|${speed.coerceIn(0.5f, 2.0f)}|${volume.coerceIn(0.1f, 2.0f)}|$text"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }
    }
}

val BailianRegion.ttsRestEndpoint: String
    get() = when (this) {
        BailianRegion.MAINLAND -> "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer"
        else -> "$dashScopeBaseUrl/services/audio/tts/SpeechSynthesizer"
    }

val BailianRegion.ttsWebSocketEndpoint: String
    get() = when (this) {
        BailianRegion.MAINLAND -> "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
        BailianRegion.SINGAPORE -> "wss://dashscope-intl.aliyuncs.com/api-ws/v1/inference"
        BailianRegion.US -> "wss://dashscope-us.aliyuncs.com/api-ws/v1/inference"
        BailianRegion.HONG_KONG -> "wss://cn-hongkong.dashscope.aliyuncs.com/api-ws/v1/inference"
    }
