package com.xingyue.english.core.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.xingyue.english.core.PracticeMode
import com.xingyue.english.core.TypeWordsPracticeFlowEngine

class TypeWordsSourceParityTest {
    @Test
    fun `default settings keep TypeWords practice defaults`() {
        val settings = TypeWordsSettingState()

        assertEquals("us", settings.soundType)
        assertEquals(3, settings.wordReviewRatio)
        assertEquals(TypeWordsPracticeMode.SYSTEM, settings.wordPracticeMode)
        assertEquals(TypeWordsPracticeType.FOLLOW_WRITE, settings.wordPracticeType)
        assertEquals(0, settings.fsrsEasyLimit)
        assertEquals(3, settings.fsrsGoodLimit)
        assertEquals(6, settings.fsrsHardLimit)
        assertEquals(21, settings.fsrsParameters.w.size)
        assertEquals("Enter", settings.shortcutKeyMap["ToggleCollect"])
    }

    @Test
    fun `system mode stage map follows original TypeWords sequence`() {
        assertEquals(
            listOf(
                TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
                TypeWordsPracticeStage.LISTEN_NEW_WORD,
                TypeWordsPracticeStage.DICTATION_NEW_WORD,
                TypeWordsPracticeStage.IDENTIFY_REVIEW,
                TypeWordsPracticeStage.LISTEN_REVIEW,
                TypeWordsPracticeStage.DICTATION_REVIEW,
                TypeWordsPracticeStage.COMPLETE
            ),
            TypeWordsPracticeRules.stageMap[TypeWordsPracticeMode.SYSTEM]
        )
        assertNull(TypeWordsPracticeRules.stageMap[TypeWordsPracticeMode.SHUFFLE_WORDS_TEST])
        assertEquals(
            TypeWordsPracticeStage.DICTATION_NEW_WORD,
            TypeWordsPracticeRules.nextStage(
                TypeWordsPracticeMode.SYSTEM,
                TypeWordsPracticeStage.LISTEN_NEW_WORD
            )
        )
    }

    @Test
    fun `current study word selection keeps new review and ignore rules`() {
        val words = listOf("a", "abandon", "ability", "able", "about", "above")
            .map { word -> TypeWordsDefaults.defaultWord(word) }
        val dict = TypeWordsDefaults.defaultDict("cet4", "四级").copy(
            resource = TypeWordsDictResource(id = "cet4", name = "四级", length = words.size),
            lastLearnIndex = 0,
            perDayStudyNumber = 3,
            words = words
        )
        val settings = TypeWordsSettingState(ignoreSimpleWord = true, wordReviewRatio = 1)

        val task = TypeWordsPracticeRules.currentStudyWords(
            dict = dict,
            settings = settings,
            knownWords = listOf("about"),
            simpleWords = TypeWordsDefaults.simpleWords,
            fsrsDueByWord = mapOf("ability" to TypeWordsFsrsCard(dueAt = 1000L)),
            now = 2000L
        )

        assertEquals(listOf("abandon", "ability", "able"), task.new.map { it.word })
        assertEquals(emptyList(), task.review.map { it.word })
    }

    @Test
    fun `review queue falls back to learned words when fsrs due is not enough`() {
        val words = listOf("alpha", "bravo", "charlie", "delta", "echo")
            .map { word -> TypeWordsDefaults.defaultWord(word) }
        val dict = TypeWordsDefaults.defaultDict("daily", "Daily").copy(
            resource = TypeWordsDictResource(id = "daily", name = "Daily", length = words.size),
            lastLearnIndex = 3,
            perDayStudyNumber = 2,
            words = words,
            complete = false
        )

        val task = TypeWordsPracticeRules.currentStudyWords(
            dict = dict,
            settings = TypeWordsSettingState(wordReviewRatio = 1),
            fsrsDueByWord = mapOf("alpha" to TypeWordsFsrsCard(dueAt = 1L)),
            now = 2L
        )

        assertEquals(listOf("delta", "echo"), task.new.map { it.word })
        assertEquals(listOf("alpha", "charlie"), task.review.map { it.word })
    }

    @Test
    fun `android TypeWords system flow keeps original stage order`() {
        val newWords = listOf("alpha", "bravo").map { TypeWordsDefaults.defaultWord(it) }
        val reviewWords = listOf("charlie").map { TypeWordsDefaults.defaultWord(it) }

        val flow = TypeWordsPracticeFlowEngine.prompts(
            appMode = PracticeMode.TYPEWORDS_SYSTEM,
            newWords = newWords,
            reviewWords = reviewWords,
            sessionLimit = 20
        )

        assertEquals(
            listOf(
                TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
                TypeWordsPracticeStage.FOLLOW_WRITE_NEW_WORD,
                TypeWordsPracticeStage.LISTEN_NEW_WORD,
                TypeWordsPracticeStage.LISTEN_NEW_WORD,
                TypeWordsPracticeStage.DICTATION_NEW_WORD,
                TypeWordsPracticeStage.DICTATION_NEW_WORD,
                TypeWordsPracticeStage.IDENTIFY_REVIEW,
                TypeWordsPracticeStage.LISTEN_REVIEW,
                TypeWordsPracticeStage.DICTATION_REVIEW
            ),
            flow.map { it.stage }
        )
        assertEquals(listOf("alpha", "bravo", "alpha"), flow.take(3).map { it.prompt.expected })
    }

    @Test
    fun `word maps to app learning word without losing TypeWords fields`() {
        val word = TypeWordsWord(
            id = "w1",
            word = "listening",
            phonetic0 = "/'lɪsənɪŋ/",
            trans = listOf(TypeWordsTranslation("n", "听力；倾听")),
            sentences = listOf(TypeWordsSentenceExample("Practice listening every day.", "每天练习听力。")),
            relWords = TypeWordsRelatedWords(root = "listen")
        )

        val mapped = word.toLearningWord(sourceTitle = "TypeWords sample", now = 100L)

        assertEquals("w1", mapped.id)
        assertEquals("listening", mapped.normalized)
        assertEquals("/'lɪsənɪŋ/", mapped.phonetic)
        assertEquals("n 听力；倾听", mapped.chineseDefinition)
        assertEquals("Practice listening every day.", mapped.contexts.single().englishSentence)
        assertEquals("listen", mapped.notes)
    }

    @Test
    fun `parser keeps TypeWords save data word books and articles`() {
        val json = """
            {
              "version": 4,
              "val": {
                "word": {
                  "bookList": [
                    {
                      "id": "daily",
                      "name": "Daily",
                      "type": "word",
                      "lastLearnIndex": 1,
                      "perDayStudyNumber": 20,
                      "words": [
                        {
                          "id": "w1",
                          "word": "listen",
                          "phonetic0": "/listen/",
                          "trans": [{"pos": "v", "cn": "听"}],
                          "sentences": [{"c": "Listen closely.", "cn": "仔细听。"}],
                          "phrases": [],
                          "synos": [],
                          "relWords": {"root": "listen", "rels": []},
                          "etymology": []
                        }
                      ]
                    }
                  ]
                },
                "article": {
                  "bookList": [
                    {
                      "id": "reader",
                      "name": "Reader",
                      "type": "article",
                      "articles": [
                        {
                          "id": "a1",
                          "title": "A short story",
                          "text": "Hello world.",
                          "sections": [[{"text": "Hello world.", "translate": "你好世界。", "words": [], "audioPosition": []}]],
                          "newWords": []
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val dicts = TypeWordsJsonParser.parseDicts(json)

        assertEquals(2, dicts.size)
        assertEquals("Daily", dicts[0].name)
        assertEquals("listen", dicts[0].words.single().word)
        assertEquals("Reader", dicts[1].name)
        assertEquals("A short story", dicts[1].articles.single().title)
    }
}
