package com.yilmaz.hybridtranslationapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class GlossaryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "glossary.db"
        private const val TABLE_TRANSLATIONS = "translations"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_HASH = "hash"
        private const val COLUMN_TARGET_LANG = "target_lang"
        private const val COLUMN_TARGET_TEXT = "target_text"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTranslationsTableQuery = "CREATE TABLE $TABLE_TRANSLATIONS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TEXT TEXT, " +
                "$COLUMN_HASH TEXT, " +
                "$COLUMN_TARGET_LANG TEXT, " +
                "$COLUMN_TARGET_TEXT TEXT)"

        db?.execSQL(createTranslationsTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Drop the table if it exists and create a new one
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSLATIONS")
        onCreate(db)
    }

    private fun generateHash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun addElement(sourceLangCode: String, targetLangCode: String, sourceText: String, targetText: String) {
        val db = writableDatabase

        val sourceTextHash = generateHash(sourceText)
        val targetTextHash = generateHash(targetText)

        // Check if the translation already exists in the database
        val existingCursor = db.query(
            TABLE_TRANSLATIONS,
            null,
            "$COLUMN_TEXT = ? AND $COLUMN_TARGET_LANG = ?",
            arrayOf(sourceText, targetLangCode),
            null,
            null,
            null
        )

        if (existingCursor.count == 0) {
            // Insert the translation
            val contentValues = ContentValues()
            contentValues.put(COLUMN_TEXT, sourceText)
            contentValues.put(COLUMN_HASH, sourceTextHash)
            contentValues.put(COLUMN_TARGET_LANG, targetLangCode)
            contentValues.put(COLUMN_TARGET_TEXT, targetText) // Set the correct target text

            db.insert(TABLE_TRANSLATIONS, null, contentValues)
        }

        existingCursor.close()

        // Check if the reverse translation is missing
        val reverseCursor = db.query(
            TABLE_TRANSLATIONS,
            null,
            "$COLUMN_TEXT = ? AND $COLUMN_TARGET_LANG = ?",
            arrayOf(targetText, sourceLangCode),
            null,
            null,
            null
        )

        if (reverseCursor.count == 0) {
            // Insert the reverse translation
            val reverseContentValues = ContentValues()
            reverseContentValues.put(COLUMN_TEXT, targetText)
            reverseContentValues.put(COLUMN_HASH, targetTextHash) // Use the separate hash for the target text
            reverseContentValues.put(COLUMN_TARGET_LANG, sourceLangCode)
            reverseContentValues.put(COLUMN_TARGET_TEXT, sourceText)

            db.insert(TABLE_TRANSLATIONS, null, reverseContentValues)
        }

        reverseCursor.close()
    }

    @SuppressLint("Range")
    fun getTranslation(sourceLangCode: String, targetLangCode: String, sourceText: String, visitedLanguages: MutableSet<String> = mutableSetOf()): String? {
        val db = readableDatabase
        val sourceTextHash = generateHash(sourceText)
        print("hash = $sourceTextHash")
        //printTableContents()

        // check if a direct translation exists
        val directSelection = "($COLUMN_HASH = ? AND $COLUMN_TARGET_LANG = ?) OR ($COLUMN_TEXT = ? AND $COLUMN_TARGET_LANG = ?)"
        val directSelectionArgs = arrayOf(sourceTextHash, targetLangCode, sourceTextHash, sourceLangCode)

        val directQueryCursor = db.query(
            TABLE_TRANSLATIONS,
            arrayOf(COLUMN_TARGET_TEXT),
            directSelection,
            directSelectionArgs,
            null,
            null,
            null
        )

        if (directQueryCursor.moveToFirst()) {
            val translation = directQueryCursor.getString(directQueryCursor.getColumnIndex(COLUMN_TARGET_TEXT))
            directQueryCursor.close()
            return translation
        }

        directQueryCursor.close()

        // find intermediate translations recursively
        val intermediateQueryCursor = db.query(
            TABLE_TRANSLATIONS,
            arrayOf(COLUMN_TARGET_LANG),
            "$COLUMN_TEXT = ? AND $COLUMN_TARGET_TEXT != ?",
            arrayOf(sourceText, sourceText),
            null,
            null,
            null
        )

        if (intermediateQueryCursor.moveToFirst()) {
            do {
                val intermediateLangCode = intermediateQueryCursor.getString(intermediateQueryCursor.getColumnIndex(COLUMN_TARGET_LANG))

                // Check if we have already visited the intermediate language
                if (visitedLanguages.contains(intermediateLangCode)) {
                    continue
                }

                visitedLanguages.add(intermediateLangCode)

                val intermediateTranslation = getTranslation(sourceLangCode, intermediateLangCode, sourceText, visitedLanguages)
                if (intermediateTranslation != null) {
                    val directTranslation = getTranslation(intermediateLangCode, targetLangCode, intermediateTranslation, visitedLanguages)
                    if (directTranslation != null) {
                        intermediateQueryCursor.close()
                        return directTranslation
                    }
                }

                visitedLanguages.remove(intermediateLangCode)
            } while (intermediateQueryCursor.moveToNext())
        }

        intermediateQueryCursor.close()

        return null
    }

    fun resetTranslations() {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSLATIONS")
        onCreate(db)
    }

    @SuppressLint("Range")
    fun printTableContents() {
        val db = readableDatabase

        val query = "SELECT * FROM $TABLE_TRANSLATIONS"
        val cursor = db.rawQuery(query, null)

        cursor?.let {
            if (it.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex("id"))
                    val text = cursor.getString(cursor.getColumnIndex(COLUMN_TEXT))
                    val hash = cursor.getString(cursor.getColumnIndex(COLUMN_HASH))
                    val targetLang = cursor.getString(cursor.getColumnIndex(COLUMN_TARGET_LANG))
                    val targetText = cursor.getString(cursor.getColumnIndex(COLUMN_TARGET_TEXT))

                    // Print the row data
                    println("Row: id=$id, text=$text, hash=$hash, targetLang=$targetLang, targetText=$targetText")
                } while (it.moveToNext())
            }
            cursor.close()
        }
    }
}