package com.xingyue.english.android.data

import com.xingyue.english.android.data.db.LexicalItemEntity
import com.xingyue.english.android.data.db.PhraseChunkEntity
import com.xingyue.english.android.data.db.VocabularyDeckEntity
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.android.data.db.enumValueOrDefault
import com.xingyue.english.android.data.db.toArticleEntities
import com.xingyue.english.android.data.db.toCaptionEntities
import com.xingyue.english.android.data.db.toEntity
import com.xingyue.english.android.data.db.toEntities
import com.xingyue.english.android.data.db.toRawJson
import com.xingyue.english.android.data.db.toSourceEntity
import com.xingyue.english.android.data.db.toWordEntities
import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.SourceContext
import com.xingyue.english.core.SourceType
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.source.MuJingVocabulary
import com.xingyue.english.core.source.MuJingVocabularyAdapter
import com.xingyue.english.core.source.MuJingVocabularyParser
import com.xingyue.english.core.source.TypeWordsArticle
import com.xingyue.english.core.source.TypeWordsDict
import com.xingyue.english.core.source.TypeWordsDictType
import com.xingyue.english.core.source.TypeWordsJsonParser
import com.xingyue.english.core.source.toLearningWord
import org.json.JSONObject

internal class SourceProjectImporter(
    private val database: XingYueDatabase
) {
    fun importIfRecognized(content: ImportedContent, rawText: String): ImportedContent? {
        val trimmed = rawText.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
        importMuJingVocabulary(content, trimmed)?.let { return it }
        importTypeWordsData(content, trimmed)?.let { return it }
        return null
    }

    private fun importMuJingVocabulary(content: ImportedContent, rawText: String): ImportedContent? {
        val root = runCatching { JSONObject(rawText) }.getOrNull() ?: return null
        if (!root.has("wordList")) return null
        val vocabulary = runCatching { MuJingVocabularyParser.parse(rawText, content.title) }.getOrNull() ?: return null
        if (vocabulary.wordList.isEmpty()) return null

        val now = System.currentTimeMillis()
        val vocabularyId = "mujing-${content.id}"
        database.sourceModelDao().upsertMuJingVocabulary(vocabulary.toSourceEntity(vocabularyId, rawText, now))
        database.sourceModelDao().upsertMuJingWords(vocabulary.toWordEntities(vocabularyId, now))
        database.sourceModelDao().upsertMuJingCaptions(vocabulary.toCaptionEntities(vocabularyId, now))

        val caption = MuJingVocabularyAdapter.vocabularyToCaption(vocabulary, content.id)
        val captionId = if (caption.cues.isNotEmpty()) "cap-${content.id}" else ""
        if (caption.cues.isNotEmpty()) {
            database.captionDao().upsertAll(
                caption.copy(id = captionId, sourceItemId = content.id).toEntities()
            )
        }

        vocabulary.toLearningWords(content, captionId, now).forEach(::upsertLearningWord)
        val ready = content.copy(
            title = vocabulary.name.ifBlank { content.title },
            kind = when (vocabulary.type.name) {
                "SUBTITLES", "MKV" -> SourceType.SUBTITLE
                else -> SourceType.DOCUMENT
            },
            originalText = rawText,
            status = ImportProcessingStatus.READY_TO_LEARN,
            statusMessage = "已导入词库：${vocabulary.wordList.size} 个词，${caption.cues.size} 条字幕上下文",
            progress = 100,
            captionId = captionId,
            wordCount = vocabulary.wordList.size
        )
        database.contentDao().upsert(ready.toEntity())
        return ready
    }

    private fun importTypeWordsData(content: ImportedContent, rawText: String): ImportedContent? {
        val dicts = runCatching { TypeWordsJsonParser.parseDicts(rawText, content.title) }.getOrDefault(emptyList())
        val useful = dicts.filter { it.words.isNotEmpty() || it.articles.isNotEmpty() }
        if (useful.isEmpty()) return null

        val now = System.currentTimeMillis()
        val sourceDao = database.sourceModelDao()
        useful.forEach { dict ->
            sourceDao.upsertTypeWordsDict(dict.toSourceEntity(rawText, now))
            sourceDao.upsertTypeWordsWords(dict.toWordEntities(now))
            sourceDao.upsertTypeWordsArticles(dict.toArticleEntities(now))
        }
        database.vocabularyDeckDao().upsertAll(useful.mapIndexed(::toDeckEntity))
        database.lexicalItemDao().upsertAll(useful.flatMap { it.toLexicalItems(now) })
        database.phraseChunkDao().upsertAll(useful.flatMap { it.toPhraseChunks(now) })
        useful.forEach { importTypeWordsBookWords(it, now) }
        useful.flatMap { dict -> dict.articles.mapIndexed { index, article -> dict to (index to article) } }
            .forEach { (dict, indexedArticle) ->
                importTypeWordsArticle(dict, indexedArticle.first, indexedArticle.second, now)
            }

        val wordCount = useful.sumOf { it.words.size + it.articles.sumOf { article -> article.newWords.size } }
        val articleCount = useful.sumOf { it.articles.size }
        val ready = content.copy(
            originalText = rawText,
            status = ImportProcessingStatus.READY_TO_LEARN,
            statusMessage = "已导入词书：${useful.size} 本，${wordCount} 个词，${articleCount} 篇文章",
            progress = 100,
            wordCount = wordCount
        )
        database.contentDao().upsert(ready.toEntity())
        return ready
    }

    private fun toDeckEntity(index: Int, dict: TypeWordsDict): VocabularyDeckEntity =
        VocabularyDeckEntity(
            id = "typewords-${dict.id}",
            name = dict.name.ifBlank { "导入词书" },
            stage = "MY_SUBTITLE_WORDS",
            goalMode = "GENERAL",
            description = dict.resource.description.ifBlank { "导入词书" },
            licenseSource = "导入词书",
            itemTarget = dict.length,
            displayOrder = 10_000 + index,
            createdAt = System.currentTimeMillis()
        )

    private fun TypeWordsDict.toLexicalItems(now: Long): List<LexicalItemEntity> =
        words.filter { it.normalized.isNotBlank() }.mapIndexed { index, word ->
            LexicalItemEntity(
                id = "typewords-$id-${word.normalized}",
                word = word.word,
                normalized = word.normalized,
                phonetic = word.phonetic0.ifBlank { word.phonetic1 },
                definition = word.definitionText(),
                cefr = "",
                deckId = "typewords-$id",
                stage = "MY_SUBTITLE_WORDS",
                tags = resource.tags.joinToString("\n"),
                phrases = word.phrases.joinToString("\n") { item ->
                    listOf(item.c, item.cn).filter { it.isNotBlank() }.joinToString(" - ")
                },
                example = word.sentences.firstOrNull()?.let {
                    listOf(it.c, it.cn).filter { part -> part.isNotBlank() }.joinToString("\n")
                }.orEmpty(),
                licenseSource = "导入词书",
                difficulty = index + 1,
                createdAt = now
            )
        }

    private fun TypeWordsDict.toPhraseChunks(now: Long): List<PhraseChunkEntity> {
        val wordPhrases = words.flatMap { word ->
            word.phrases.mapIndexedNotNull { index, phrase ->
                val english = phrase.c.trim()
                if (english.isBlank()) return@mapIndexedNotNull null
                PhraseChunkEntity(
                    id = "typewords-$id-${word.normalized}-phrase-$index",
                    english = english,
                    chinese = phrase.cn,
                    useCase = "DAILY",
                    deckId = "typewords-$id",
                    keywords = listOf(word.word, word.normalized).filter { it.isNotBlank() }.distinct().joinToString("\n"),
                    licenseSource = "导入词书",
                    ttsCacheKey = "",
                    createdAt = now
                )
            }
        }
        val articleSentences = articles.flatMapIndexed { articleIndex, article ->
            article.sections.flatten().mapIndexedNotNull { sentenceIndex, sentence ->
                val english = sentence.text.trim()
                if (english.isBlank()) return@mapIndexedNotNull null
                PhraseChunkEntity(
                    id = "typewords-$id-article-$articleIndex-sentence-$sentenceIndex",
                    english = english,
                    chinese = sentence.translate,
                    useCase = "DAILY",
                    deckId = "typewords-$id",
                    keywords = article.title,
                    licenseSource = "导入词书",
                    ttsCacheKey = "",
                    createdAt = now
                )
            }
        }
        return (wordPhrases + articleSentences).distinctBy { it.id }
    }

    private fun importTypeWordsBookWords(dict: TypeWordsDict, now: Long) {
        val status = when (dict.resource.type) {
            TypeWordsDictType.COLLECT -> LearningWordStatus.LEARNING
            TypeWordsDictType.WRONG -> LearningWordStatus.DUE
            TypeWordsDictType.KNOWN -> LearningWordStatus.MASTERED
            else -> null
        }
        if (status == null) return
        dict.words.forEach { word ->
            val imported = word.toLearningWord(dict.name, now).copy(
                id = "typewords-${dict.id}-${word.normalized}",
                status = status,
                notes = listOf("导入词书：${dict.name}", word.relWords.root).filter { it.isNotBlank() }.joinToString(" | ")
            )
            upsertLearningWord(imported)
        }
    }

    private fun importTypeWordsArticle(dict: TypeWordsDict, index: Int, article: TypeWordsArticle, now: Long) {
        val articleKey = article.id ?: "article-$index-${article.title.hashCode()}"
        val contentId = "typewords-article-${dict.id}-$articleKey"
        val text = article.text.ifBlank {
            article.sections.flatten().joinToString("\n") { it.text }
        }
        val imported = ImportedContent(
            id = contentId,
            title = article.title.ifBlank { "${dict.name} 文章 ${index + 1}" },
            kind = SourceType.DOCUMENT,
            extension = "typewords",
            sourcePath = contentId,
            sourceUrl = article.audioSrc,
            originalText = text,
            status = ImportProcessingStatus.READY_TO_LEARN,
            statusMessage = "文章已导入，可逐句练习和保存生词",
            progress = 100,
            captionId = "cap-$contentId",
            wordCount = article.newWords.size,
            createdAt = now
        )
        database.contentDao().upsert(imported.toEntity())

        val cues = article.sentencesForPractice().mapIndexed { sentenceIndex, pair ->
            CaptionCue(
                id = "tw-$contentId-$sentenceIndex",
                startMs = sentenceIndex * 1000L,
                endMs = (sentenceIndex + 1) * 1000L,
                english = pair.first,
                chinese = pair.second
            )
        }
        if (cues.isNotEmpty()) {
            database.captionDao().upsertAll(
                BilingualCaption(
                    id = imported.captionId,
                    sourceItemId = contentId,
                    cues = cues,
                    source = CaptionSource.DOCUMENT,
                    createdAt = now
                ).toEntities()
            )
        }
        article.newWords.forEach { word ->
            val learningWord = word.toLearningWord(article.title.ifBlank { dict.name }, now).copy(
                id = "typewords-article-$articleKey-${word.normalized}",
                contexts = cues.take(3).mapIndexed { cueIndex, cue ->
                    SourceContext(
                        sourceItemId = contentId,
                        sourceTitle = imported.title,
                        captionId = imported.captionId,
                        captionStartMs = cue.startMs,
                        captionEndMs = cue.endMs,
                        englishSentence = cue.english,
                        chineseSentence = cue.chinese,
                        sourceType = LearningSourceType.DOCUMENT,
                        paragraphIndex = cueIndex,
                        createdAt = now
                    )
                }
            )
            upsertLearningWord(learningWord)
        }
    }

    private fun upsertLearningWord(word: LearningWord) {
        if (word.normalized.isBlank()) return
        val dao = database.learningWordDao()
        val previous = dao.get(word.normalized)
        val previousContexts = dao.contextsForWord(word.normalized)
        val newContexts = word.contexts.filterNot { context ->
            previousContexts.any {
                it.sourceItemId == context.sourceItemId &&
                    it.captionId == context.captionId &&
                    it.captionStartMs == context.captionStartMs &&
                    it.englishSentence == context.englishSentence
            }
        }
        newContexts.forEach { dao.insertContext(it.toEntity(word.normalized)) }
        val allContexts = dao.contextsForWord(word.normalized)
        val previousStatus = previous?.status?.let { enumValueOrDefault(it, LearningWordStatus.NEW_WORD) }
        dao.upsert(
            word.copy(
                id = previous?.id ?: word.id,
                phonetic = word.phonetic.ifBlank { previous?.phonetic.orEmpty() },
                chineseDefinition = word.chineseDefinition.ifBlank { previous?.chineseDefinition.orEmpty() },
                status = when {
                    word.status == LearningWordStatus.MASTERED -> LearningWordStatus.MASTERED
                    previousStatus == LearningWordStatus.MASTERED -> LearningWordStatus.MASTERED
                    previousStatus != null -> previousStatus
                    else -> word.status
                },
                occurrenceCount = allContexts.size.coerceAtLeast(word.occurrenceCount),
                createdAt = previous?.createdAt ?: word.createdAt,
                updatedAt = System.currentTimeMillis(),
                dueAt = previous?.dueAt ?: word.dueAt,
                notes = previous?.notes?.ifBlank { word.notes } ?: word.notes
            ).toEntity()
        )
    }

    private fun MuJingVocabulary.toLearningWords(
        content: ImportedContent,
        captionId: String,
        now: Long
    ): List<LearningWord> =
        MuJingVocabularyAdapter.vocabularyToLearningWords(this, now).map { word ->
            word.copy(
                id = "mujing-${content.id}-${word.normalized}",
                contexts = word.contexts.map { context ->
                    context.copy(
                        sourceItemId = content.id,
                        sourceTitle = content.title,
                        captionId = captionId.ifBlank { context.captionId },
                        createdAt = now
                    )
                }
            )
        }

    private fun TypeWordsArticle.sentencesForPractice(): List<Pair<String, String>> {
        val fromSections = sections.flatten()
            .map { it.text to it.translate }
            .filter { it.first.isNotBlank() || it.second.isNotBlank() }
        if (fromSections.isNotEmpty()) return fromSections
        return text.split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { TextTools.hasEnglish(it) }
            .map { it to "" }
    }
}
