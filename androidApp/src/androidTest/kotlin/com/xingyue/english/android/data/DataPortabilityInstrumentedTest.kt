package com.xingyue.english.android.data

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xingyue.english.android.data.db.XingYueDatabase
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.SourceType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataPortabilityInstrumentedTest {
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
    fun apkgImportCreatesCaptionAndLearningWords() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val apkg = createApkg(context.cacheDir)
        val repository = XingYueRepository(context, database)
        val content = ImportedContent(
            id = "apkg-content",
            title = "sample.apkg",
            kind = SourceType.DOCUMENT,
            extension = "apkg",
            sourcePath = apkg.absolutePath
        )
        repository.contentStore.save(content)

        val imported = ApkgPackageImporter(context, repository).importApkg(content, apkg)

        assertEquals(com.xingyue.english.core.ImportProcessingStatus.READY_TO_LEARN, imported.status)
        assertNotNull(database.learningWordDao().get("listening"))
        assertTrue(database.captionDao().findByContentId(content.id).isNotEmpty())
    }

    @Test
    fun pdfImportExtractsTextAndCreatesCaptionPracticeMaterial() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pdf = createPdf(context.cacheDir, "Practice listening every day.")
        val repository = XingYueRepository(context, database)

        val imported = ImportProcessor(context, repository).importUri(Uri.fromFile(pdf))

        assertEquals(ImportProcessingStatus.READY_TO_LEARN, imported.status)
        assertTrue(imported.originalText.contains("Practice listening every day."))
        assertTrue(database.captionDao().findByContentId(imported.id).isNotEmpty())
    }

    @Test
    fun sqliteBackupRoundTripsAllAppTables() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = XingYueRepository(context, database)
        repository.contentStore.save(
            ImportedContent(
                id = "content-one",
                title = "Lesson",
                kind = SourceType.DOCUMENT,
                extension = "txt",
                originalText = "Practice listening every day."
            )
        )

        val json = SqliteBackupManager(database).exportJson()
        val restored = Room.inMemoryDatabaseBuilder(context, XingYueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val result = SqliteBackupManager(restored).importJson(json)
            assertTrue(result.tableCount > 0)
            assertEquals("Lesson", restored.contentDao().get("content-one")?.title)
        } finally {
            restored.close()
        }
    }

    @Test
    fun packagedEcdictIsQueryableWithoutSeedFallback() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val entry = OfflineDictionaryRepository(context, database.dictionaryEntryDao()).lookup("abandon")

        assertNotNull(entry)
        val dictionaryEntry = requireNotNull(entry)
        assertEquals("完整 ECDICT", dictionaryEntry.source)
        assertTrue(dictionaryEntry.definition.isNotBlank())
    }

    private fun createApkg(dir: File): File {
        val collection = File(dir, "collection.anki2").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(collection, null).use { db ->
            db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY, flds TEXT NOT NULL, sfld TEXT NOT NULL)")
            db.execSQL("CREATE TABLE cards(id INTEGER PRIMARY KEY, nid INTEGER NOT NULL, due INTEGER NOT NULL, reps INTEGER NOT NULL, lapses INTEGER NOT NULL)")
            db.execSQL(
                "INSERT INTO notes(id, flds, sfld) VALUES(1, ?, ?)",
                arrayOf("listening\u001f听力；倾听\u001fPractice listening every day.", "listening")
            )
            db.execSQL("INSERT INTO cards(id, nid, due, reps, lapses) VALUES(1, 1, 0, 2, 0)")
        }
        val apkg = File(dir, "sample.apkg").apply { delete() }
        ZipOutputStream(apkg.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("collection.anki2"))
            collection.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        return apkg
    }

    private fun createPdf(dir: File, text: String): File {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PDFBoxResourceLoader.init(context)
        val pdf = File(dir, "sample-text.pdf").apply { delete() }
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 14f)
                stream.newLineAtOffset(72f, 720f)
                stream.showText(text)
                stream.endText()
            }
            document.save(pdf)
        }
        return pdf
    }
}
