package com.xingyue.english.android.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xingyue.english.android.data.db.XingYueDatabase
import org.json.JSONArray
import org.json.JSONObject

data class BackupResult(
    val tableCount: Int,
    val rowCount: Int
)

class SqliteBackupManager(
    private val database: XingYueDatabase
) {
    fun exportJson(): String {
        val db = database.openHelper.readableDatabase
        val tables = appTables(db)
        val root = JSONObject()
            .put("format", "xingyue-learning-backup")
            .put("formatVersion", 1)
            .put("createdAt", System.currentTimeMillis())
            .put("databaseVersion", db.version)
        val tableJson = JSONObject()
        tables.forEach { table ->
            tableJson.put(table, exportTable(db, table))
        }
        root.put("tables", tableJson)
        return root.toString()
    }

    fun importJson(text: String): BackupResult {
        val root = JSONObject(text)
        require(root.optString("format") == "xingyue-learning-backup") { "不是星月英语备份文件" }
        val tables = root.optJSONObject("tables") ?: error("备份文件缺少 tables")
        val db = database.openHelper.writableDatabase
        val currentTables = appTables(db).associateWith { tableColumns(db, it) }
        var rowCount = 0
        var tableCount = 0
        db.beginTransaction()
        try {
            val names = tables.keys().asSequence().filter { it in currentTables }.toList()
            names.forEach { table ->
                db.execSQL("DELETE FROM ${quoteIdentifier(table)}")
            }
            names.forEach { table ->
                val columns = currentTables.getValue(table)
                val rows = tables.optJSONArray(table) ?: JSONArray()
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val values = ContentValues()
                    columns.forEach { column ->
                        if (row.has(column)) values.putJsonValue(column, row.opt(column))
                    }
                    db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
                    rowCount++
                }
                tableCount++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return BackupResult(tableCount, rowCount)
    }

    private fun exportTable(db: SupportSQLiteDatabase, table: String): JSONArray {
        val rows = JSONArray()
        db.query("SELECT * FROM ${quoteIdentifier(table)}").use { cursor ->
            while (cursor.moveToNext()) rows.put(cursor.toJsonObject())
        }
        return rows
    }

    private fun appTables(db: SupportSQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        db.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
              AND name NOT IN ('android_metadata', 'room_master_table')
            ORDER BY name
            """.trimIndent()
        ).use { cursor ->
            while (cursor.moveToNext()) tables += cursor.getString(0)
        }
        return tables
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(${quoteIdentifier(table)})").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        return columns
    }

    private fun Cursor.toJsonObject(): JSONObject {
        val row = JSONObject()
        for (index in 0 until columnCount) {
            val name = getColumnName(index)
            when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(name, getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> row.put(name, getDouble(index))
                Cursor.FIELD_TYPE_BLOB -> row.put(name, JSONObject().put("__blobBase64", Base64.encodeToString(getBlob(index), Base64.NO_WRAP)))
                else -> row.put(name, getString(index))
            }
        }
        return row
    }

    private fun ContentValues.putJsonValue(key: String, value: Any?) {
        when (value) {
            null, JSONObject.NULL -> putNull(key)
            is Boolean -> put(key, if (value) 1 else 0)
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Double -> put(key, value)
            is Float -> put(key, value)
            is JSONObject -> {
                if (value.has("__blobBase64")) {
                    put(key, Base64.decode(value.optString("__blobBase64"), Base64.NO_WRAP))
                } else {
                    put(key, value.toString())
                }
            }
            is Number -> put(key, value.toString())
            else -> put(key, value.toString())
        }
    }

    private fun quoteIdentifier(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""
}
