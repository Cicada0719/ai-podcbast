package com.xingyue.english.core

import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object SubtitleParser {
    private val cueTimeRegex = Regex(
        "(\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3})\\s*-->\\s*(\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3})"
    )

    fun parse(fileName: String, text: String): List<CaptionCue> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext == "ass" || ext == "ssa" || text.contains("[Events]", ignoreCase = true) -> parseAss(text)
            ext == "json" || looksLikeJson(text) -> parseJson(text).ifEmpty { parseTimedText(text) }
            else -> parseTimedText(text)
        }
    }

    fun fromPlainText(text: String): List<CaptionCue> {
        val chunks = text
            .split(Regex("\\n\\s*\\n|(?<=[.!?])\\s+(?=[A-Z])"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(120)

        return chunks.mapIndexed { index, paragraph ->
            val start = index * 5000L
            CaptionCue(
                id = "doc-${index + 1}",
                startMs = start,
                endMs = start + 4500L,
                english = paragraph
            )
        }
    }

    private fun parseTimedText(text: String): List<CaptionCue> {
        val normalized = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lineSequence()
            .filterNot { it.trim().equals("WEBVTT", ignoreCase = true) }
            .joinToString("\n")

        return normalized.split(Regex("\\n\\s*\\n"))
            .mapNotNull { block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
                val timeLineIndex = lines.indexOfFirst { cueTimeRegex.containsMatchIn(it) }
                if (timeLineIndex < 0) return@mapNotNull null

                val match = cueTimeRegex.find(lines[timeLineIndex]) ?: return@mapNotNull null
                val bodyLines = lines.drop(timeLineIndex + 1)
                    .map { TextTools.stripMarkup(it) }
                    .filter { it.isNotBlank() && !it.startsWith("NOTE", ignoreCase = true) }
                if (bodyLines.isEmpty()) return@mapNotNull null

                val bilingual = splitBilingual(bodyLines)
                CaptionCue(
                    id = "cue-${match.range.first}-${match.range.last}-${bodyLines.hashCode()}",
                    startMs = parseTimestamp(match.groupValues[1]),
                    endMs = parseTimestamp(match.groupValues[2]),
                    english = bilingual.first,
                    chinese = bilingual.second
                )
            }
    }

    private fun parseAss(text: String): List<CaptionCue> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("Dialogue:", ignoreCase = true) }
            .mapIndexedNotNull { index, line ->
                val payload = line.substringAfter(":", "")
                val parts = payload.split(",", limit = 10)
                if (parts.size < 10) return@mapIndexedNotNull null
                val body = TextTools.stripMarkup(parts[9])
                if (body.isBlank()) return@mapIndexedNotNull null
                val bilingual = splitBilingual(body.split(Regex("\\\\N|\\n")).map { it.trim() })
                CaptionCue(
                    id = "ass-${index + 1}",
                    startMs = parseAssTimestamp(parts[1].trim()),
                    endMs = parseAssTimestamp(parts[2].trim()),
                    english = bilingual.first,
                    chinese = bilingual.second
                )
            }.toList()

    private fun parseJson(text: String): List<CaptionCue> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        return runCatching {
            when (val root = JSONTokener(trimmed).nextValue()) {
                is JSONArray -> jsonArrayToCues(root)
                is JSONObject -> {
                    val cueArray = firstJsonArray(root, "cues", "captions", "subtitles", "items", "segments", "events")
                    if (cueArray != null) jsonArrayToCues(cueArray) else listOfNotNull(jsonObjectToCue(root, 0))
                }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun jsonArrayToCues(array: JSONArray): List<CaptionCue> =
        (0 until array.length()).mapNotNull { index ->
            when (val item = array.opt(index)) {
                is JSONObject -> jsonObjectToCue(item, index)
                is String -> jsonObjectToCue(JSONObject().put("text", item), index)
                else -> null
            }
        }

    private fun jsonObjectToCue(item: JSONObject, index: Int): CaptionCue? {
        val explicitEnglish = firstJsonString(item, "english", "en", "source", "sourceText", "source_text")
        val looseText = firstJsonString(item, "text", "sentence", "caption", "line", "content", "body")
            .ifBlank { youtubeJson3Segments(item) }
        val splitText = splitBilingual(looseText.lines())
        val english = explicitEnglish.ifBlank { splitText.first }.trim()
        if (english.isBlank()) return null

        val chinese = firstJsonString(
            item,
            "chinese",
            "zh",
            "cn",
            "translation",
            "translated",
            "translationZh",
            "translation_zh",
            "target",
            "targetText",
            "target_text"
        ).ifBlank { splitText.second }.trim()

        val start = readJsonMillis(
            item,
            listOf("startMs", "start_ms", "tStartMs", "beginMs", "begin_ms", "start", "from", "begin", "startTime", "start_time"),
            index * 5000L
        )
        val end = readJsonMillis(
            item,
            listOf("endMs", "end_ms", "finishMs", "finish_ms", "end", "to", "finish", "endTime", "end_time"),
            start + readJsonMillis(item, listOf("dDurationMs", "durationMs", "duration_ms", "duration"), 4500L)
        ).coerceAtLeast(start + 250L)

        return CaptionCue(
            id = firstJsonString(item, "id", "cueId", "cue_id").ifBlank { "json-${index + 1}" },
            startMs = start,
            endMs = end,
            english = english,
            chinese = chinese
        )
    }

    private fun firstJsonArray(item: JSONObject, vararg keys: String): JSONArray? =
        keys.firstNotNullOfOrNull { key -> item.optJSONArray(key) }

    private fun firstJsonString(item: JSONObject, vararg keys: String): String =
        keys.firstNotNullOfOrNull { key ->
            if (!item.has(key) || item.isNull(key)) null else item.optString(key).trim().takeIf { it.isNotBlank() }
        }.orEmpty()

    private fun readJsonMillis(item: JSONObject, keys: List<String>, default: Long): Long {
        keys.forEach { key ->
            if (item.has(key) && !item.isNull(key)) {
                return jsonValueToMillis(item.opt(key), key.contains("ms", ignoreCase = true), default)
            }
        }
        return default
    }

    private fun jsonValueToMillis(value: Any?, valueIsMillis: Boolean, default: Long): Long =
        when (value) {
            is Number -> if (valueIsMillis) value.toLong() else (value.toDouble() * 1000.0).roundToLong()
            is String -> {
                val trimmed = value.trim()
                when {
                    trimmed.contains(":") -> parseTimestamp(trimmed)
                    else -> trimmed.toDoubleOrNull()?.let {
                        if (valueIsMillis) it.roundToLong() else (it * 1000.0).roundToLong()
                    } ?: default
                }
            }
            else -> default
        }

    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        return listOf("\"cues\"", "\"captions\"", "\"subtitles\"", "\"english\"", "\"text\"", "\"segments\"", "\"events\"", "\"segs\"")
            .any { trimmed.contains(it, ignoreCase = true) }
    }

    private fun youtubeJson3Segments(item: JSONObject): String {
        val segs = item.optJSONArray("segs") ?: return ""
        return (0 until segs.length()).joinToString("") { index ->
            segs.optJSONObject(index)?.optString("utf8").orEmpty()
        }.trim()
    }

    private fun splitBilingual(lines: List<String>): Pair<String, String> {
        val english = mutableListOf<String>()
        val chinese = mutableListOf<String>()
        lines.map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            if (TextTools.containsCjk(line) && !TextTools.hasEnglish(line)) {
                chinese += line
            } else if (TextTools.containsCjk(line)) {
                chinese += line
            } else {
                english += line
            }
        }
        val englishText = english.joinToString(" ").ifBlank {
            lines.firstOrNull { TextTools.hasEnglish(it) } ?: lines.firstOrNull().orEmpty()
        }
        return englishText to chinese.joinToString(" ")
    }

    private fun parseTimestamp(value: String): Long {
        val parts = value.replace(',', '.').split(':', '.')
        if (parts.size < 4) return 0L
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val seconds = parts[2].toLongOrNull() ?: 0L
        val millis = parts[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (((hours * 60L) + minutes) * 60L + seconds) * 1000L + millis
    }

    private fun parseAssTimestamp(value: String): Long {
        val parts = value.split(':', '.')
        if (parts.size < 4) return 0L
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val seconds = parts[2].toLongOrNull() ?: 0L
        val centis = parts[3].padEnd(2, '0').take(2).toLongOrNull() ?: 0L
        return (((hours * 60L) + minutes) * 60L + seconds) * 1000L + centis * 10L
    }
}
