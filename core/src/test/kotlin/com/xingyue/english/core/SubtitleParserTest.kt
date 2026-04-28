package com.xingyue.english.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleParserTest {
    @Test
    fun parsesSrtAndKeepsBilingualLines() {
        val cues = SubtitleParser.parse(
            "lesson.srt",
            """
            1
            00:00:01,000 --> 00:00:03,500
            We should preserve the original context.
            我们应该保留原始语境。

            2
            00:00:04,000 --> 00:00:06,000
            Vocabulary grows through repetition.
            """.trimIndent()
        )

        assertEquals(2, cues.size)
        assertEquals(1000L, cues.first().startMs)
        assertEquals("We should preserve the original context.", cues.first().english)
        assertEquals("我们应该保留原始语境。", cues.first().chinese)
        assertTrue(cues.first().tokens.any { it.normalized == "preserve" })
    }

    @Test
    fun parsesVtt() {
        val cues = SubtitleParser.parse(
            "clip.vtt",
            """
            WEBVTT

            00:00:02.000 --> 00:00:04.000
            Listening practice should feel natural.
            """.trimIndent()
        )

        assertEquals(1, cues.size)
        assertEquals(2000L, cues.single().startMs)
        assertEquals(4000L, cues.single().endMs)
    }

    @Test
    fun parsesAssDialogue() {
        val cues = SubtitleParser.parse(
            "movie.ass",
            """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:07.10,0:00:09.20,Default,,0,0,0,,This line contains deliberate practice.
            """.trimIndent()
        )

        assertEquals(1, cues.size)
        assertEquals(7100L, cues.single().startMs)
        assertEquals("This line contains deliberate practice.", cues.single().english)
    }

    @Test
    fun stripsAssStyleMarkup() {
        val cues = SubtitleParser.parse(
            "styled.ass",
            """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,{\i1}Markup should disappear.{\i0}
            """.trimIndent()
        )

        assertEquals("Markup should disappear.", cues.single().english)
    }

    @Test
    fun parsesJsonCaptionsAndKeepsMissingChineseEmpty() {
        val cues = SubtitleParser.parse(
            "captions.json",
            """
            {
              "cues": [
                {
                  "id": "a",
                  "startMs": 1000,
                  "endMs": 2500,
                  "english": "English-only captions should be translated later.",
                  "chinese": ""
                },
                {
                  "start": 3.0,
                  "end": 5.5,
                  "text": "Bilingual JSON captions keep their translation.\n双语 JSON 字幕保留中文。"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, cues.size)
        assertEquals(1000L, cues.first().startMs)
        assertEquals("English-only captions should be translated later.", cues.first().english)
        assertEquals("", cues.first().chinese)
        assertEquals(3000L, cues[1].startMs)
        assertEquals("Bilingual JSON captions keep their translation.", cues[1].english)
        assertEquals("双语 JSON 字幕保留中文。", cues[1].chinese)
    }

    @Test
    fun parsesYoutubeJson3Events() {
        val cues = SubtitleParser.parse(
            "youtube.json3",
            """
            {
              "events": [
                {
                  "tStartMs": 1200,
                  "dDurationMs": 2300,
                  "segs": [
                    {"utf8": "Automatic "},
                    {"utf8": "captions work."}
                  ]
                },
                {
                  "tStartMs": 4100,
                  "dDurationMs": 1800,
                  "segs": [
                    {"utf8": "Second line."}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, cues.size)
        assertEquals(1200L, cues.first().startMs)
        assertEquals(3500L, cues.first().endMs)
        assertEquals("Automatic captions work.", cues.first().english)
        assertEquals(4100L, cues[1].startMs)
    }
}
