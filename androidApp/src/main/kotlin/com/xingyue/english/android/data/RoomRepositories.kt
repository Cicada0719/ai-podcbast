package com.xingyue.english.android.data

import com.xingyue.english.android.data.db.CaptionDao
import com.xingyue.english.android.data.db.ContentDao
import com.xingyue.english.android.data.db.LearningWordDao
import com.xingyue.english.android.data.db.matches
import com.xingyue.english.android.data.db.toCaption
import com.xingyue.english.android.data.db.toDomain
import com.xingyue.english.android.data.db.toEntities
import com.xingyue.english.android.data.db.toEntity
import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.BilingualCaptionRepository
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.ImportedContentStore
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordFilters
import com.xingyue.english.core.LearningWordRepository
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.SourceContext
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.WordSelectionContext

class RoomImportedContentStore(private val contentDao: ContentDao) : ImportedContentStore {
    override fun get(itemId: String): ImportedContent? =
        contentDao.get(itemId)?.toDomain()

    override fun save(item: ImportedContent) {
        contentDao.upsert(item.toEntity())
    }

    override fun update(item: ImportedContent) {
        contentDao.upsert(item.toEntity())
    }
}

class RoomBilingualCaptionRepository(private val captionDao: CaptionDao) : BilingualCaptionRepository {
    override fun save(caption: BilingualCaption) {
        captionDao.upsertAll(caption.toEntities())
    }

    override fun findByContentId(sourceItemId: String): BilingualCaption? =
        captionDao.findByContentId(sourceItemId).toCaption()

    override fun get(captionId: String): BilingualCaption? =
        captionDao.get(captionId).toCaption()

    override fun markSavedWords(captionId: String, savedWords: Set<String>): BilingualCaption? {
        val current = get(captionId) ?: return null
        return current.copy(
            cues = current.cues.map { cue ->
                cue.copy(tokens = TextTools.tokenize(cue.english, savedWords))
            }
        )
    }
}

class RoomLearningWordRepository(private val wordDao: LearningWordDao) : LearningWordRepository {
    override fun addFromSelection(context: WordSelectionContext): LearningWord {
        val normalized = context.normalized.ifBlank { TextTools.normalizeWord(context.word) }
        require(normalized.isNotBlank()) { "word must contain at least one English letter" }

        val now = System.currentTimeMillis()
        val sourceContext = SourceContext(
            sourceItemId = context.sourceItemId,
            sourceTitle = context.sourceTitle,
            captionId = context.captionId,
            captionStartMs = context.captionStartMs,
            captionEndMs = context.captionEndMs,
            englishSentence = context.englishSentence,
            chineseSentence = context.chineseSentence,
            sourceType = context.sourceType,
            sourceUrl = context.sourceUrl,
            paragraphIndex = context.paragraphIndex,
            createdAt = now
        )
        val existingContexts = wordDao.contextsForWord(normalized)
        val alreadyHasContext = existingContexts.any {
            it.sourceItemId == context.sourceItemId &&
                it.captionId == context.captionId &&
                it.captionStartMs == context.captionStartMs &&
                it.englishSentence == context.englishSentence
        }
        if (!alreadyHasContext) {
            wordDao.insertContext(sourceContext.toEntity(normalized))
        }

        val previous = wordDao.get(normalized)
        val contexts = wordDao.contextsForWord(normalized)
        val updated = LearningWord(
            id = previous?.id ?: "lw-$normalized-$now",
            word = previous?.word ?: context.word,
            normalized = normalized,
            phonetic = context.phonetic.ifBlank { previous?.phonetic.orEmpty() },
            chineseDefinition = context.chineseDefinition.ifBlank { previous?.chineseDefinition.orEmpty() },
            status = previous?.status?.let { enumValueOf<LearningWordStatus>(it) } ?: LearningWordStatus.NEW_WORD,
            contexts = contexts.map { it.toDomain() },
            occurrenceCount = contexts.size,
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
            dueAt = previous?.dueAt ?: now,
            notes = previous?.notes.orEmpty()
        )
        wordDao.upsert(updated.toEntity())
        return updated
    }

    override fun markMastered(word: String): LearningWord? =
        updateStatus(word, LearningWordStatus.MASTERED, Long.MAX_VALUE)

    override fun markDue(word: String): LearningWord? =
        updateStatus(word, LearningWordStatus.DUE, System.currentTimeMillis())

    override fun search(query: String, filters: LearningWordFilters): List<LearningWord> {
        val contexts = wordDao.allContexts().groupBy { it.wordNormalized }
        return wordDao.all()
            .asSequence()
            .filter { it.matches(query, filters.sourceType, filters.status, contexts[it.normalized].orEmpty()) }
            .filter { filters.sourceItemId == null || contexts[it.normalized].orEmpty().any { context -> context.sourceItemId == filters.sourceItemId } }
            .map { it.toDomain(contexts[it.normalized].orEmpty()) }
            .sortedWith(compareByDescending<LearningWord> { it.updatedAt }.thenBy { it.normalized })
            .toList()
    }

    override fun wordsForCaption(captionId: String): List<LearningWord> {
        val contextsForCaption = wordDao.contextsForCaption(captionId)
        val keys = contextsForCaption.map { it.wordNormalized }.toSet()
        return keys.mapNotNull { normalized ->
            wordDao.get(normalized)?.toDomain(wordDao.contextsForWord(normalized))
        }
    }

    override fun all(): List<LearningWord> {
        val contexts = wordDao.allContexts().groupBy { it.wordNormalized }
        return wordDao.all().map { it.toDomain(contexts[it.normalized].orEmpty()) }
    }

    fun updateNotes(normalized: String, notes: String): LearningWord? {
        val previous = wordDao.get(normalized) ?: return null
        val updated = previous.copy(notes = notes, updatedAt = System.currentTimeMillis())
        wordDao.upsert(updated)
        return updated.toDomain(wordDao.contextsForWord(normalized))
    }

    fun setStatus(normalized: String, status: LearningWordStatus): LearningWord? =
        updateStatus(normalized, status, if (status == LearningWordStatus.MASTERED) Long.MAX_VALUE else System.currentTimeMillis())

    private fun updateStatus(word: String, status: LearningWordStatus, dueAt: Long): LearningWord? {
        val normalized = TextTools.normalizeWord(word)
        val previous = wordDao.get(normalized) ?: return null
        val updated = previous.copy(
            status = status.name,
            updatedAt = System.currentTimeMillis(),
            dueAt = dueAt
        )
        wordDao.upsert(updated)
        return updated.toDomain(wordDao.contextsForWord(normalized))
    }
}
