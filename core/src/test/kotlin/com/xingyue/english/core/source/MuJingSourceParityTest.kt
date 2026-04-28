package com.xingyue.english.core.source

import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.LearningSourceType
import kotlin.test.Test
import kotlin.test.assertEquals

class MuJingSourceParityTest {
    @Test
    fun `MuJing vocabulary json keeps original word caption structure`() {
        val json = """
            {
              "name": "Sample",
              "type": "SUBTITLES",
              "language": "English",
              "size": 1,
              "relateVideoPath": "/video/sample.mp4",
              "subtitlesTrackId": 2,
              "wordList": [
                {
                  "value": "listening",
                  "usphone": "/'lɪsənɪŋ/",
                  "definition": "the act of hearing attentively",
                  "translation": "听力；倾听",
                  "pos": "n",
                  "collins": 3,
                  "oxford": true,
                  "tag": "exam",
                  "bnc": 100,
                  "frq": 200,
                  "exchange": "listen:d",
                  "captions": [
                    {
                      "start": "00:00:01,000",
                      "end": "00:00:02,500",
                      "content": "Practice listening every day."
                    }
                  ],
                  "externalCaptions": [
                    {
                      "relateVideoPath": "/video/other.mp4",
                      "subtitlesTrackId": 7,
                      "subtitlesName": "other.srt",
                      "start": "00:00:03.000",
                      "end": "00:00:04.000",
                      "content": "Save useful words from subtitles."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val vocabulary = MuJingVocabularyParser.parse(json)

        assertEquals("Sample", vocabulary.name)
        assertEquals(MuJingVocabularyType.SUBTITLES, vocabulary.type)
        assertEquals(1, vocabulary.normalizedSize)
        assertEquals("listening", vocabulary.wordList.single().value)
        assertEquals(1, vocabulary.wordList.single().captions.size)
        assertEquals(1, vocabulary.wordList.single().externalCaptions.size)
    }

    @Test
    fun `MuJing word maps to learning word with source contexts`() {
        val vocabulary = MuJingVocabulary(
            name = "Sample",
            type = MuJingVocabularyType.SUBTITLES,
            relateVideoPath = "/video/sample.mp4",
            subtitlesTrackId = 2,
            wordList = listOf(
                MuJingWord(
                    value = "listening",
                    usphone = "/'lɪsənɪŋ/",
                    translation = "听力；倾听",
                    captions = listOf(
                        MuJingCaption("00:00:01,000", "00:00:02,500", "Practice listening every day.")
                    )
                )
            )
        )

        val word = MuJingVocabularyAdapter.vocabularyToLearningWords(vocabulary, now = 100L).single()

        assertEquals("mujing-listening", word.id)
        assertEquals("listening", word.normalized)
        assertEquals("/'lɪsənɪŋ/", word.phonetic)
        assertEquals("听力；倾听", word.chineseDefinition)
        assertEquals(LearningSourceType.SUBTITLE, word.contexts.single().sourceType)
        assertEquals(1000L, word.contexts.single().captionStartMs)
        assertEquals(2500L, word.contexts.single().captionEndMs)
    }

    @Test
    fun `MuJing linked captions can rebuild an app caption timeline`() {
        val vocabulary = MuJingVocabulary(
            name = "Sample",
            type = MuJingVocabularyType.MKV,
            relateVideoPath = "/video/sample.mkv",
            wordList = listOf(
                MuJingWord(
                    value = "save",
                    externalCaptions = listOf(
                        MuJingExternalCaption(
                            relateVideoPath = "/video/sample.mkv",
                            subtitlesTrackId = 1,
                            subtitlesName = "eng.srt",
                            start = "00:00:03,000",
                            end = "00:00:04,000",
                            content = "Save useful words from subtitles."
                        )
                    )
                )
            )
        )

        val caption = MuJingVocabularyAdapter.vocabularyToCaption(vocabulary)

        assertEquals(CaptionSource.IMPORTED_SUBTITLE, caption.source)
        assertEquals("/video/sample.mkv", caption.sourceItemId)
        assertEquals(3000L, caption.cues.single().startMs)
        assertEquals("Save useful words from subtitles.", caption.cues.single().english)
    }
}
