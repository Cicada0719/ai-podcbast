package com.xingyue.english.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.xingyue.english.core.BilingualCaption
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionSource
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.WordSelectionContext
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream

class ApkgPackageImporter(
    private val context: Context,
    private val repository: XingYueRepository
) {
    fun importApkg(content: ImportedContent, file: File): ImportedContent {
        val notes = extractNotes(file)
        if (notes.isEmpty()) {
            val failed = content.copy(
                status = ImportProcessingStatus.FAILED,
                statusMessage = "APKG 未读取到可学习卡片。",
                progress = 0
            )
            repository.contentStore.update(failed)
            return failed
        }

        val captionId = "cap-${content.id}"
        val cues = notes.mapIndexed { index, note ->
            CaptionCue(
                id = "apkg-${note.noteId}",
                startMs = index * 5_000L,
                endMs = index * 5_000L + 4_500L,
                english = note.example.ifBlank { note.word },
                chinese = note.definition
            )
        }
        repository.captionRepository.save(
            BilingualCaption(
                id = captionId,
                sourceItemId = content.id,
                cues = cues,
                source = CaptionSource.DOCUMENT
            )
        )

        notes.forEachIndexed { index, note ->
            val cue = cues[index]
            val selection = WordSelectionContext(
                word = note.word,
                normalized = note.normalized,
                sourceItemId = content.id,
                captionStartMs = cue.startMs,
                captionEndMs = cue.endMs,
                englishSentence = cue.english,
                chineseSentence = note.definition,
                sourceType = LearningSourceType.DOCUMENT,
                captionId = captionId,
                sourceTitle = content.title,
                phonetic = note.phonetic,
                chineseDefinition = note.definition
            )
            val saved = repository.learningWordRepository.addFromSelection(selection)
            if (note.reviewCount > 0 && note.dueOffsetDays <= 0) {
                repository.learningWordRepository.markDue(saved.normalized)
            } else if (note.reviewCount > 0) {
                repository.learningWordRepository.markMastered(saved.normalized)
            }
        }

        val ready = content.copy(
            status = ImportProcessingStatus.READY_TO_LEARN,
            statusMessage = "APKG 已导入：${notes.size} 张卡片，已加入词库和逐句练习。",
            progress = 100,
            captionId = captionId,
            wordCount = notes.size,
            originalText = notes.joinToString("\n\n") { note ->
                listOf(note.word, note.phonetic, note.definition, note.example)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
        )
        repository.contentStore.update(ready)
        return ready
    }

    private fun extractNotes(file: File): List<ApkgNote> {
        val collection = extractCollection(file) ?: return emptyList()
        return runCatching {
            SQLiteDatabase.openDatabase(collection.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val cardState = readCardState(db)
                db.rawQuery(
                    "SELECT id, flds, sfld FROM notes ORDER BY id LIMIT 20000",
                    emptyArray()
                ).use { cursor ->
                    val rows = mutableListOf<ApkgNote>()
                    while (cursor.moveToNext()) {
                        val noteId = cursor.getLong(0)
                        val fields = cursor.getString(1)
                            .orEmpty()
                            .split('\u001f')
                            .map(::cleanField)
                            .filter { it.isNotBlank() }
                        val sortField = cleanField(cursor.getString(2).orEmpty())
                        val word = firstEnglishWord(sortField).ifBlank {
                            fields.asSequence().map(::firstEnglishWord).firstOrNull { it.isNotBlank() }.orEmpty()
                        }
                        val normalized = TextTools.normalizeWord(word)
                        if (normalized.isBlank()) continue
                        val definition = fields
                            .filterNot { TextTools.normalizeWord(it) == normalized }
                            .firstOrNull { it.any(Char::isLetterOrDigit) }
                            .orEmpty()
                        val example = fields.firstOrNull { field ->
                            field.contains(normalized, ignoreCase = true) &&
                                field.length > normalized.length + 8
                        }.orEmpty()
                        val phonetic = fields.firstOrNull { it.startsWith("/") && it.endsWith("/") }.orEmpty()
                        val state = cardState[noteId]
                        rows += ApkgNote(
                            noteId = noteId,
                            word = word,
                            normalized = normalized,
                            phonetic = phonetic,
                            definition = definition,
                            example = example,
                            dueOffsetDays = state?.dueOffsetDays ?: 0,
                            reviewCount = state?.reviewCount ?: 0
                        )
                    }
                    rows.distinctBy { it.normalized }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readCardState(db: SQLiteDatabase): Map<Long, ApkgCardState> =
        runCatching {
            db.rawQuery("SELECT nid, due, reps, lapses FROM cards", emptyArray()).use { cursor ->
                val cards = mutableMapOf<Long, ApkgCardState>()
                while (cursor.moveToNext()) {
                    val noteId = cursor.getLong(0)
                    val due = cursor.getInt(1)
                    val reps = cursor.getInt(2)
                    val lapses = cursor.getInt(3)
                    val previous = cards[noteId]
                    if (previous == null || due < previous.dueOffsetDays) {
                        cards[noteId] = ApkgCardState(due, reps, lapses)
                    }
                }
                cards
            }
        }.getOrDefault(emptyMap())

    private fun extractCollection(file: File): File? {
        val target = File(context.cacheDir, "${file.nameWithoutExtension}-collection.anki2")
        target.delete()
        ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "collection.anki2" || entry.name == "collection.anki21") {
                    target.outputStream().use { output -> zip.copyTo(output) }
                    return target
                }
            }
        }
        return null
    }

    private fun cleanField(value: String): String =
        value
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun firstEnglishWord(value: String): String =
        Regex("[A-Za-z][A-Za-z'\\-]{1,}")
            .find(value)
            ?.value
            ?.lowercase(Locale.ROOT)
            .orEmpty()
}

private data class ApkgNote(
    val noteId: Long,
    val word: String,
    val normalized: String,
    val phonetic: String,
    val definition: String,
    val example: String,
    val dueOffsetDays: Int,
    val reviewCount: Int
)

private data class ApkgCardState(
    val dueOffsetDays: Int,
    val reviewCount: Int,
    @Suppress("unused") val lapses: Int
)
