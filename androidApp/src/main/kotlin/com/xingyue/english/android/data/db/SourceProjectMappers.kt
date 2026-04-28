package com.xingyue.english.android.data.db

import com.xingyue.english.core.source.MuJingVocabulary
import com.xingyue.english.core.source.MuJingVocabularyParser
import com.xingyue.english.core.source.TypeWordsArticle
import com.xingyue.english.core.source.TypeWordsDict
import com.xingyue.english.core.source.TypeWordsJsonParser
import org.json.JSONArray
import org.json.JSONObject

fun TypeWordsDict.toSourceEntity(rawJson: String, importedAt: Long): TypeWordsDictEntity =
    TypeWordsDictEntity(
        id = id,
        name = name,
        description = resource.description,
        url = resource.url,
        category = resource.category,
        tags = resource.tags.joinToString("\n"),
        translateLanguage = resource.translateLanguage,
        dictType = resource.type.name,
        language = resource.language,
        lastLearnIndex = lastLearnIndex,
        perDayStudyNumber = perDayStudyNumber,
        length = length,
        custom = custom,
        complete = complete,
        enName = enName,
        createdBy = createdBy,
        cover = cover,
        rawJson = rawJson,
        importedAt = importedAt
    )

fun TypeWordsDict.toWordEntities(importedAt: Long): List<TypeWordsWordEntity> =
    words.mapIndexed { index, word ->
        TypeWordsWordEntity(
            dictId = id,
            normalized = word.normalized,
            orderIndex = index,
            word = word.word,
            phonetic0 = word.phonetic0,
            phonetic1 = word.phonetic1,
            definitionText = word.definitionText(),
            rawJson = TypeWordsJsonParser.wordToJson(word).toString(),
            importedAt = importedAt
        )
    }

fun TypeWordsDict.toArticleEntities(importedAt: Long): List<TypeWordsArticleEntity> =
    articles.mapIndexed { index, article ->
        TypeWordsArticleEntity(
            dictId = id,
            articleKey = article.id ?: "article-$index-${article.title.hashCode()}",
            orderIndex = index,
            title = article.title,
            titleTranslate = article.titleTranslate,
            text = article.text,
            textTranslate = article.textTranslate,
            audioSrc = article.audioSrc,
            rawJson = article.toJson().toString(),
            importedAt = importedAt
        )
    }

fun MuJingVocabulary.toSourceEntity(vocabularyId: String, rawJson: String, importedAt: Long): MuJingVocabularyEntity =
    MuJingVocabularyEntity(
        id = vocabularyId,
        name = name,
        vocabularyType = type.name,
        language = language,
        size = normalizedSize,
        relateVideoPath = relateVideoPath,
        subtitlesTrackId = subtitlesTrackId,
        rawJson = rawJson,
        importedAt = importedAt
    )

fun MuJingVocabulary.toWordEntities(vocabularyId: String, importedAt: Long): List<MuJingWordEntity> =
    wordList.mapIndexed { index, word ->
        MuJingWordEntity(
            vocabularyId = vocabularyId,
            normalized = word.normalized,
            orderIndex = index,
            value = word.value,
            usphone = word.usphone,
            ukphone = word.ukphone,
            definition = word.definition,
            translation = word.translation,
            pos = word.pos,
            collins = word.collins,
            oxford = word.oxford,
            tag = word.tag,
            bnc = word.bnc,
            frq = word.frq,
            exchange = word.exchange,
            importedAt = importedAt
        )
    }

fun MuJingVocabulary.toCaptionEntities(vocabularyId: String, importedAt: Long): List<MuJingCaptionEntity> =
    wordList.flatMap { word ->
        val external = word.externalCaptions.mapIndexed { index, caption ->
            MuJingCaptionEntity(
                vocabularyId = vocabularyId,
                wordNormalized = word.normalized,
                captionKind = "external",
                captionIndex = index,
                relateVideoPath = caption.relateVideoPath,
                subtitlesTrackId = caption.subtitlesTrackId,
                subtitlesName = caption.subtitlesName,
                startText = caption.start,
                endText = caption.end,
                content = caption.content,
                importedAt = importedAt
            )
        }
        val internal = word.captions.mapIndexed { index, caption ->
            MuJingCaptionEntity(
                vocabularyId = vocabularyId,
                wordNormalized = word.normalized,
                captionKind = "internal",
                captionIndex = index,
                relateVideoPath = relateVideoPath,
                subtitlesTrackId = subtitlesTrackId,
                subtitlesName = name,
                startText = caption.start,
                endText = caption.end,
                content = caption.content,
                importedAt = importedAt
            )
        }
        external + internal
    }

fun MuJingVocabulary.toRawJson(): String =
    MuJingVocabularyParser.toJson(this).toString()

private fun TypeWordsArticle.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("title", title)
        .put("titleTranslate", titleTranslate)
        .put("text", text)
        .put("textTranslate", textTranslate)
        .put("newWords", JSONArray(newWords.map { TypeWordsJsonParser.wordToJson(it) }))
        .put("audioSrc", audioSrc)
        .put("audioFileId", audioFileId)
        .put("nameList", JSONArray(nameList))
