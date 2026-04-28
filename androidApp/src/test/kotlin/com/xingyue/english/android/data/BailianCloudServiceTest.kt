package com.xingyue.english.android.data

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString.Companion.encodeUtf8
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BailianCloudServiceTest {
    @Test
    fun defaultVoiceListKeepsReadableVoiceChoices() {
        val ids = BailianTtsVoices.voices.map { it.id }

        assertEquals("longanyang", BailianTtsVoices.defaultVoice.id)
        assertTrue(ids.containsAll(listOf("longanyang", "longanhuan", "longxiaochun_v3", "longxiaoxia_v3", "longcheng_v3")))
        assertTrue(ids.containsAll(listOf("longhuhu_v3", "longpaopao_v3", "longniuniu_v3")))
        assertTrue(ids.containsAll(listOf("loongabby_v3", "loongandy_v3", "loongannie_v3")))
        assertTrue(BailianTtsVoices.voices.size >= 20)
        assertTrue(BailianTtsVoices.voices.all { it.description.isNotBlank() })
        assertFalse(BailianTtsVoices.voices.any { it.description.contains("cosyvoice", ignoreCase = true) })
        assertEquals(BailianTtsVoices.defaultVoice, BailianTtsVoices.find("missing-voice"))
    }

    @Test
    fun restTtsDownloadsAndCachesAudio() {
        MockWebServer().use { server ->
            val audioUrl = server.url("/audio.mp3").toString()
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put("output", JSONObject().put("audio", JSONObject().put("url", audioUrl)))
                        .toString()
                )
            )
            server.enqueue(MockResponse().setBody("mp3-bytes"))

            val cacheDir = Files.createTempDirectory("tts-rest").toFile()
            val service = BailianTtsService(
                apiKey = "test-key",
                region = BailianRegion.MAINLAND,
                cacheDir = cacheDir,
                restEndpoint = server.url("/api/v1/services/audio/tts/SpeechSynthesizer").toString(),
                webSocketEndpoint = server.url("/ws").toString().replace("http://", "ws://")
            )

            val first = service.synthesize("hello world", "longanyang", 1f, 1f)
            val second = service.synthesize("hello world", "longanyang", 1f, 1f)

            assertFalse(first.cached)
            assertTrue(second.cached)
            assertEquals("mp3-bytes", java.io.File(first.audioPath).readText())
            assertEquals("/api/v1/services/audio/tts/SpeechSynthesizer", server.takeRequest().path)
            assertEquals("/audio.mp3", server.takeRequest().path)
        }
    }

    @Test
    fun websocketFallbackUsesRunContinueFinishOrder() {
        MockWebServer().use { restServer ->
            MockWebServer().use { wsServer ->
                restServer.enqueue(MockResponse().setResponseCode(500).setBody("rest failed"))
                val actions = CopyOnWriteArrayList<String>()
                val finished = CountDownLatch(1)
                wsServer.enqueue(
                    MockResponse().withWebSocketUpgrade(
                        object : WebSocketListener() {
                            override fun onMessage(webSocket: WebSocket, text: String) {
                                val json = JSONObject(text)
                                val action = json.getJSONObject("header").getString("action")
                                actions += action
                                when (action) {
                                    "run-task" -> {
                                        assertEquals(0, json.getJSONObject("payload").getJSONObject("input").length())
                                        webSocket.send(JSONObject().put("header", JSONObject().put("event", "task-started")).toString())
                                    }
                                    "continue-task" -> webSocket.send("mp3".encodeUtf8())
                                    "finish-task" -> {
                                        webSocket.send(JSONObject().put("header", JSONObject().put("event", "task-finished")).toString())
                                        webSocket.close(1000, "done")
                                        finished.countDown()
                                    }
                                }
                            }
                        }
                    )
                )

                val service = BailianTtsService(
                    apiKey = "test-key",
                    region = BailianRegion.MAINLAND,
                    cacheDir = Files.createTempDirectory("tts-ws").toFile(),
                    restEndpoint = restServer.url("/tts").toString(),
                    webSocketEndpoint = wsServer.url("/ws").toString().replace("http://", "ws://")
                )

                val result = service.synthesize("hello", "longanyang", 1f, 1f)

                assertTrue(finished.await(2, TimeUnit.SECONDS))
                assertEquals(listOf("run-task", "continue-task", "finish-task"), actions.toList())
                assertEquals("mp3", java.io.File(result.audioPath).readText())
            }
        }
    }

    @Test
    fun ttsFailureDeletesEmptyCacheFile() {
        MockWebServer().use { restServer ->
            MockWebServer().use { wsServer ->
                restServer.enqueue(MockResponse().setResponseCode(500).setBody("rest failed"))
                wsServer.enqueue(
                    MockResponse().withWebSocketUpgrade(
                        object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                webSocket.send(
                                    JSONObject()
                                        .put(
                                            "header",
                                            JSONObject()
                                                .put("event", "task-failed")
                                                .put("error_message", "voice unavailable")
                                        )
                                        .toString()
                                )
                                webSocket.close(1000, "failed")
                            }
                        }
                    )
                )

                val cacheDir = Files.createTempDirectory("tts-fail").toFile()
                val service = BailianTtsService(
                    apiKey = "test-key",
                    region = BailianRegion.MAINLAND,
                    cacheDir = cacheDir,
                    restEndpoint = restServer.url("/tts").toString(),
                    webSocketEndpoint = wsServer.url("/ws").toString().replace("http://", "ws://")
                )

                assertFailsWith<IllegalStateException> {
                    service.synthesize("hello", "longanyang", 1f, 1f)
                }
                assertTrue(cacheDir.listFiles().orEmpty().none { it.length() == 0L })
            }
        }
    }

    @Test
    fun connectionTestAcceptsNonOkModelReplyWhenHttpSucceeds() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put(
                            "choices",
                            org.json.JSONArray().put(
                                JSONObject().put("message", JSONObject().put("content", "pong"))
                            )
                        )
                        .toString()
                )
            )

            val result = BailianLanguageService(
                apiKey = "test-key",
                region = BailianRegion.MAINLAND,
                compatibleChatEndpoint = server.url("/compatible-mode/v1/chat/completions").toString()
            ).testConnection()

            assertTrue(result.success)
            assertTrue(result.message.contains("云端服务可用"))
        }
    }

    @Test
    fun shortAudioAsrUsesOpenAiCompatibleInputAudioPayload() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put(
                            "choices",
                            JSONArray().put(
                                JSONObject().put(
                                    "message",
                                    JSONObject().put("content", "Practice listening every day.")
                                )
                            )
                        )
                        .toString()
                )
            )
            val audio = Files.createTempFile("asr-compatible", ".wav").toFile()
            audio.writeBytes(byteArrayOf(1, 2, 3, 4))

            val service = BailianLanguageService(
                apiKey = "test-key",
                region = BailianRegion.MAINLAND,
                compatibleChatEndpoint = server.url("/compatible-mode/v1/chat/completions").toString(),
                dashScopeBaseUrl = server.url("/api/v1").toString()
            )

            val cues = service.transcribeAudio(audio.absolutePath)

            assertEquals("Practice listening every day.", cues.first().english)
            val request = server.takeRequest()
            assertEquals("/compatible-mode/v1/chat/completions", request.path)
            val payload = JSONObject(request.body.readUtf8())
            assertEquals("qwen3-asr-flash", payload.getString("model"))
            val audioData = payload
                .getJSONArray("messages")
                .getJSONObject(0)
                .getJSONArray("content")
                .getJSONObject(0)
                .getJSONObject("input_audio")
                .getString("data")
            assertTrue(audioData.startsWith("data:audio/wav;base64,"))
            assertTrue(payload.getJSONObject("asr_options").getBoolean("enable_itn"))
        }
    }

    @Test
    fun shortAudioAsrFallsBackToDashScopeSynchronousPayload() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(400).setBody("compatible failed"))
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put(
                            "output",
                            JSONObject().put(
                                "choices",
                                JSONArray().put(
                                    JSONObject().put(
                                        "message",
                                        JSONObject().put(
                                            "content",
                                            JSONArray().put(JSONObject().put("text", "Fallback transcription works."))
                                        )
                                    )
                                )
                            )
                        )
                        .toString()
                )
            )
            val audio = Files.createTempFile("asr-dashscope", ".wav").toFile()
            audio.writeBytes(byteArrayOf(5, 6, 7, 8))

            val service = BailianLanguageService(
                apiKey = "test-key",
                region = BailianRegion.MAINLAND,
                compatibleChatEndpoint = server.url("/compatible-mode/v1/chat/completions").toString(),
                dashScopeBaseUrl = server.url("/api/v1").toString()
            )

            val cues = service.transcribeAudio(audio.absolutePath)

            assertEquals("Fallback transcription works.", cues.first().english)
            assertEquals("/compatible-mode/v1/chat/completions", server.takeRequest().path)
            val fallbackRequest = server.takeRequest()
            assertEquals("/api/v1/services/aigc/multimodal-generation/generation", fallbackRequest.path)
            val payload = JSONObject(fallbackRequest.body.readUtf8())
            val userAudio = payload
                .getJSONObject("input")
                .getJSONArray("messages")
                .getJSONObject(1)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("audio")
            assertTrue(userAudio.isNotBlank())
            assertTrue(payload.getJSONObject("parameters").getJSONObject("asr_options").getBoolean("enable_itn"))
        }
    }

    @Test
    fun longAudioAsrPollsFileTransTaskAndParsesSentences() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put("output", JSONObject().put("task_id", "task-1").put("task_status", "PENDING"))
                        .toString()
                )
            )
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put(
                            "output",
                            JSONObject()
                                .put("task_status", "SUCCEEDED")
                                .put(
                                    "result",
                                    JSONObject().put("transcription_url", server.url("/result.json").toString())
                                )
                        )
                        .toString()
                )
            )
            server.enqueue(
                MockResponse().setBody(
                    JSONObject()
                        .put(
                            "transcripts",
                            JSONArray().put(
                                JSONObject().put(
                                    "sentences",
                                    JSONArray().put(
                                        JSONObject()
                                            .put("text", "Long audio transcription works.")
                                            .put("begin_time", 100)
                                            .put("end_time", 1800)
                                    )
                                )
                            )
                        )
                        .toString()
                )
            )

            val service = BailianLanguageService(
                apiKey = "test-key",
                region = BailianRegion.MAINLAND,
                compatibleChatEndpoint = server.url("/compatible-mode/v1/chat/completions").toString(),
                dashScopeBaseUrl = server.url("/api/v1").toString(),
                fileTransPollIntervalMs = 0,
                fileTransMaxPolls = 1
            )

            val cues = service.transcribeAudio(server.url("/audio.mp3").toString())

            assertEquals("Long audio transcription works.", cues.first().english)
            assertEquals(100, cues.first().startMs)
            assertEquals(1800, cues.first().endMs)
            assertEquals("/api/v1/services/audio/asr/transcription", server.takeRequest().path)
            assertEquals("/api/v1/tasks/task-1", server.takeRequest().path)
            assertEquals("/result.json", server.takeRequest().path)
        }
    }
}
