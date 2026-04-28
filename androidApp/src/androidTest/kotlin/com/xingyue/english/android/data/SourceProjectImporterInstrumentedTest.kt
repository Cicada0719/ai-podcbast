package com.xingyue.english.android.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.SourceType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SourceProjectImporterInstrumentedTest {
    private lateinit var database: XingYueDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            XingYueDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importsMuJingVocabularyIntoSourceTablesCaptionsAndWordbook() {
        val content = ImportedContent(
            id = "content-mujing",
            title = "MuJing sample",
            kind = SourceType.DOCUMENT,
            extension = "json",
            sourcePath = "sample.json"
        )
        val json = """
            {
              "name": "Subtitle words",
              "type": "SUBTITLES",
              "language": "english",
              "size": 1,
              "relateVideoPath": "lesson.mp4",
              "subtitlesTrackId": 2,
              "wordList": [
                {
                  "value": "listening",
                  "usphone": "/listening/",
                  "ukphone": "",
                  "definition": "n. act of listening",
                  "translation": "n. 听力",
                  "pos": "n",
                  "collins": 3,
                  "oxford": false,
                  "tag": "audio",
                  "bnc": 100,
                  "frq": 200,
                  "exchange": "",
                  "externalCaptions": [
                    {
                      "relateVideoPath": "lesson.mp4",
                      "subtitlesTrackId": 2,
                      "subtitlesName": "English",
                      "start": "00:00:01.000",
                      "end": "00:00:03.000",
                      "content": "Practice listening every day."
                    }
                  ],
                  "captions": []
                }
              ]
            }
        """.trimIndent()

        val result = SourceProjectImporter(database).importIfRecognized(content, json)

        assertNotNull(result)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, result!!.status)
        assertEquals(1, database.sourceModelDao().muJingVocabularies().size)
        assertEquals(1, database.sourceModelDao().muJingWords("mujing-content-mujing").size)
        assertEquals(1, database.sourceModelDao().muJingCaptions("mujing-content-mujing").size)
        assertEquals(1, database.captionDao().findByContentId("content-mujing").size)
        assertNotNull(database.learningWordDao().get("listening"))
        assertTrue(database.learningWordDao().contextsForWord("listening").any { it.sourceItemId == "content-mujing" })
    }

    @Test
    fun importsTypeWordsBackupIntoSourceTablesDecksArticlesAndLearningWords() {
        val content = ImportedContent(
            id = "content-typewords",
            title = "TypeWords backup",
            kind = SourceType.DOCUMENT,
            extension = "json",
            sourcePath = "backup.json"
        )
        val json = """
            {
              "version": 4,
              "val": {
                "word": {
                  "bookList": [
                    {
                      "id": "daily",
                      "name": "Daily words",
                      "type": "word",
                      "lastLearnIndex": 0,
                      "perDayStudyNumber": 20,
                      "words": [
                        {
                          "id": "w1",
                          "word": "shadowing",
                          "phonetic0": "/shadowing/",
                          "phonetic1": "",
                          "trans": [{"pos": "n", "cn": "跟读"}],
                          "sentences": [{"c": "Shadowing improves listening.", "cn": "跟读提升听力。"}],
                          "phrases": [{"c": "shadowing practice", "cn": "跟读练习"}],
                          "synos": [],
                          "relWords": {"root": "shadow", "rels": []},
                          "etymology": []
                        }
                      ]
                    },
                    {
                      "id": "wordKnown",
                      "name": "已掌握",
                      "type": "known",
                      "words": [
                        {
                          "id": "w2",
                          "word": "known",
                          "phonetic0": "",
                          "phonetic1": "",
                          "trans": [{"pos": "adj", "cn": "已知的"}],
                          "sentences": [],
                          "phrases": [],
                          "synos": [],
                          "relWords": {"root": "", "rels": []},
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
                          "titleTranslate": "短故事",
                          "text": "Listen and repeat.",
                          "textTranslate": "听并重复。",
                          "sections": [[{"text": "Listen and repeat.", "translate": "听并重复。", "words": [], "audioPosition": []}]],
                          "newWords": [
                            {
                              "id": "w3",
                              "word": "repeat",
                              "phonetic0": "/repeat/",
                              "phonetic1": "",
                              "trans": [{"pos": "v", "cn": "重复"}],
                              "sentences": [],
                              "phrases": [],
                              "synos": [],
                              "relWords": {"root": "", "rels": []},
                              "etymology": []
                            }
                          ],
                          "audioSrc": "",
                          "audioFileId": "",
                          "lrcPosition": [],
                          "nameList": [],
                          "questions": []
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val result = SourceProjectImporter(database).importIfRecognized(content, json)

        assertNotNull(result)
        assertEquals(ImportProcessingStatus.READY_TO_LEARN, result!!.status)
        assertEquals(3, database.sourceModelDao().typeWordsDicts().size)
        assertEquals(1, database.sourceModelDao().typeWordsWords("daily").size)
        assertEquals(1, database.sourceModelDao().typeWordsArticles("reader").size)
        assertEquals(3, database.vocabularyDeckDao().all().size)
        assertEquals(2, database.lexicalItemDao().all().size)
        assertTrue(database.phraseChunkDao().all().any { it.english == "shadowing practice" })
        assertTrue(database.phraseChunkDao().all().any { it.english == "Listen and repeat." })
        assertNotNull(database.learningWordDao().get("known"))
        assertNotNull(database.learningWordDao().get("repeat"))
        assertNotNull(database.contentDao().get("typewords-article-reader-a1"))
        assertEquals(1, database.captionDao().findByContentId("typewords-article-reader-a1").size)
    }

    @Test
    fun initializesIntegratedSourceAssetsIntoQueryableLearningPools() = runBlocking {
        XingYueRepository(ApplicationProvider.getApplicationContext(), database).initialize()

        assertTrue(database.lexicalItemDao().count() > 10_000)
        assertTrue(database.vocabularyDeckDao().count() > 100)
        assertTrue(database.dictionaryEntryDao().count() > 1_000)
        assertTrue(database.phraseChunkDao().count() >= 20)
        assertTrue(database.lexicalItemDao().byStage("CORE_3500").size > 1_000)
    }
}
