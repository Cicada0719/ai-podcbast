package com.xingyue.english.core

interface BilingualCaptionRepository {
    fun save(caption: BilingualCaption)
    fun findByContentId(sourceItemId: String): BilingualCaption?
    fun get(captionId: String): BilingualCaption?
    fun markSavedWords(captionId: String, savedWords: Set<String>): BilingualCaption?
}

class InMemoryBilingualCaptionRepository : BilingualCaptionRepository {
    private val captions = linkedMapOf<String, BilingualCaption>()

    override fun save(caption: BilingualCaption) {
        captions[caption.id] = caption
    }

    override fun findByContentId(sourceItemId: String): BilingualCaption? =
        captions.values.lastOrNull { it.sourceItemId == sourceItemId }

    override fun get(captionId: String): BilingualCaption? = captions[captionId]

    override fun markSavedWords(captionId: String, savedWords: Set<String>): BilingualCaption? {
        val current = captions[captionId] ?: return null
        val highlighted = current.copy(
            cues = current.cues.map { cue ->
                cue.copy(tokens = TextTools.tokenize(cue.english, savedWords))
            }
        )
        captions[captionId] = highlighted
        return highlighted
    }
}

interface LearningWordRepository {
    fun addFromSelection(context: WordSelectionContext): LearningWord
    fun markMastered(word: String): LearningWord?
    fun markDue(word: String): LearningWord?
    fun search(query: String = "", filters: LearningWordFilters = LearningWordFilters()): List<LearningWord>
    fun wordsForCaption(captionId: String): List<LearningWord>
    fun all(): List<LearningWord>
}

class InMemoryLearningWordRepository : LearningWordRepository {
    private val words = linkedMapOf<String, LearningWord>()

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

        val previous = words[normalized]
        val mergedContexts = previous?.contexts.orEmpty().let { existing ->
            if (existing.any {
                    it.sourceItemId == context.sourceItemId &&
                        it.captionId == context.captionId &&
                        it.captionStartMs == context.captionStartMs &&
                        it.englishSentence == context.englishSentence
                }
            ) {
                existing
            } else {
                existing + sourceContext
            }
        }
        val merged = LearningWord(
            id = previous?.id ?: "lw-$normalized-$now",
            word = previous?.word ?: context.word,
            normalized = normalized,
            phonetic = context.phonetic.ifBlank { previous?.phonetic.orEmpty() },
            chineseDefinition = context.chineseDefinition.ifBlank { previous?.chineseDefinition.orEmpty() },
            status = previous?.status ?: LearningWordStatus.NEW_WORD,
            contexts = mergedContexts,
            occurrenceCount = mergedContexts.size,
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
            dueAt = previous?.dueAt ?: now,
            notes = previous?.notes.orEmpty()
        )
        words[normalized] = merged
        return merged
    }

    override fun markMastered(word: String): LearningWord? =
        updateStatus(word, LearningWordStatus.MASTERED, Long.MAX_VALUE)

    override fun markDue(word: String): LearningWord? =
        updateStatus(word, LearningWordStatus.DUE, System.currentTimeMillis())

    override fun search(query: String, filters: LearningWordFilters): List<LearningWord> {
        val normalizedQuery = TextTools.normalizeWord(query)
        return words.values.asSequence()
            .filter { normalizedQuery.isBlank() || it.normalized.contains(normalizedQuery) || it.word.contains(query, ignoreCase = true) }
            .filter { filters.status == null || it.status == filters.status }
            .filter { filters.sourceType == null || it.contexts.any { context -> context.sourceType == filters.sourceType } }
            .filter { filters.sourceItemId == null || it.contexts.any { context -> context.sourceItemId == filters.sourceItemId } }
            .sortedWith(compareByDescending<LearningWord> { it.updatedAt }.thenBy { it.normalized })
            .toList()
    }

    override fun wordsForCaption(captionId: String): List<LearningWord> =
        words.values.filter { word -> word.contexts.any { it.captionId == captionId } }

    override fun all(): List<LearningWord> = words.values.toList()

    private fun updateStatus(word: String, status: LearningWordStatus, dueAt: Long): LearningWord? {
        val normalized = TextTools.normalizeWord(word)
        val previous = words[normalized] ?: return null
        val updated = previous.copy(
            status = status,
            updatedAt = System.currentTimeMillis(),
            dueAt = dueAt
        )
        words[normalized] = updated
        return updated
    }
}

interface ImportedContentStore {
    fun get(itemId: String): ImportedContent?
    fun save(item: ImportedContent)
    fun update(item: ImportedContent)
}

class InMemoryImportedContentStore(initialItems: List<ImportedContent> = emptyList()) : ImportedContentStore {
    private val items = linkedMapOf<String, ImportedContent>()

    init {
        initialItems.forEach { save(it) }
    }

    override fun get(itemId: String): ImportedContent? = items[itemId]

    override fun save(item: ImportedContent) {
        items[item.id] = item
    }

    override fun update(item: ImportedContent) {
        items[item.id] = item
    }

    fun all(): List<ImportedContent> = items.values.toList()
}
