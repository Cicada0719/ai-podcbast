package com.xingyue.english.core

object TextTools {
    private val wordRegex = Regex("[A-Za-z]+(?:['-][A-Za-z]+)?")

    private val stopWords = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "could",
        "did", "do", "does", "for", "from", "had", "has", "have", "he", "her", "his",
        "i", "if", "in", "into", "is", "it", "its", "me", "my", "not", "of", "on",
        "or", "our", "she", "so", "that", "the", "their", "them", "then", "there",
        "they", "this", "to", "was", "we", "were", "what", "when", "where", "which",
        "who", "will", "with", "you", "your"
    )

    fun normalizeWord(word: String): String =
        word.lowercase()
            .trim('\'', '-', '.', ',', ':', ';', '!', '?', '"', ')', '(', '[', ']')
            .replace(Regex("^[^a-z]+|[^a-z]+$"), "")

    fun tokenize(sentence: String, savedWords: Set<String> = emptySet()): List<CaptionToken> =
        wordRegex.findAll(sentence).map { match ->
            val normalized = normalizeWord(match.value)
            CaptionToken(
                text = match.value,
                normalized = normalized,
                startIndex = match.range.first,
                endIndex = match.range.last + 1,
                saved = normalized in savedWords
            )
        }.filter { it.normalized.isNotBlank() }.toList()

    fun candidateWords(text: String, limit: Int = 80): List<String> =
        wordRegex.findAll(text)
            .map { normalizeWord(it.value) }
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()
            .take(limit)
            .toList()

    fun candidateWords(cues: List<CaptionCue>, limit: Int = 80): List<String> =
        candidateWords(cues.joinToString(" ") { it.english }, limit)

    fun containsCjk(text: String): Boolean =
        text.any { it.code in 0x4E00..0x9FFF }

    fun hasEnglish(text: String): Boolean =
        wordRegex.containsMatchIn(text)

    fun stripMarkup(text: String): String =
        text.replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\{[^}]+\\}"), "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .trim()
}
