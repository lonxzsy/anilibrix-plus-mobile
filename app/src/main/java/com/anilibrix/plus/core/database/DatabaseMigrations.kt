package com.anilibrix.plus.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) = ensureCurrentSchema(db)
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) = ensureCurrentSchema(db)
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) = ensureCurrentSchema(db)
}

val ANILIBRIX_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
)

private fun ensureCurrentSchema(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS favorites (
            titleId INTEGER NOT NULL PRIMARY KEY,
            titleName TEXT NOT NULL,
            posterUrl TEXT,
            addedAt INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
    db.ensureColumn("favorites", "titleName", "TEXT NOT NULL DEFAULT ''")
    db.ensureColumn("favorites", "posterUrl", "TEXT")
    db.ensureColumn("favorites", "addedAt", "INTEGER NOT NULL DEFAULT 0")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS history (
            titleId INTEGER NOT NULL,
            episodeId INTEGER NOT NULL,
            episodeNumber INTEGER NOT NULL DEFAULT 0,
            timestamp INTEGER NOT NULL DEFAULT 0,
            duration INTEGER NOT NULL DEFAULT 0,
            watchedAt INTEGER NOT NULL DEFAULT 0,
            titleName TEXT NOT NULL DEFAULT '',
            posterUrl TEXT,
            PRIMARY KEY(titleId, episodeId)
        )
        """.trimIndent()
    )
    db.ensureColumn("history", "episodeNumber", "INTEGER NOT NULL DEFAULT 0")
    db.ensureColumn("history", "timestamp", "INTEGER NOT NULL DEFAULT 0")
    db.ensureColumn("history", "duration", "INTEGER NOT NULL DEFAULT 0")
    db.ensureColumn("history", "watchedAt", "INTEGER NOT NULL DEFAULT 0")
    db.ensureColumn("history", "titleName", "TEXT NOT NULL DEFAULT ''")
    db.ensureColumn("history", "posterUrl", "TEXT")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS playlists (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            createdAt INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
    db.ensureColumn("playlists", "name", "TEXT NOT NULL DEFAULT ''")
    db.ensureColumn("playlists", "createdAt", "INTEGER NOT NULL DEFAULT 0")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS playlist_items (
            playlistId INTEGER NOT NULL,
            titleId INTEGER NOT NULL,
            titleName TEXT NOT NULL DEFAULT '',
            PRIMARY KEY(playlistId, titleId),
            FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.ensureColumn("playlist_items", "titleName", "TEXT NOT NULL DEFAULT ''")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_items_playlistId ON playlist_items(playlistId)")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS watch_later (
            titleId INTEGER NOT NULL PRIMARY KEY,
            titleName TEXT NOT NULL DEFAULT '',
            posterUrl TEXT
        )
        """.trimIndent()
    )
    db.ensureColumn("watch_later", "titleName", "TEXT NOT NULL DEFAULT ''")
    db.ensureColumn("watch_later", "posterUrl", "TEXT")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS ratings (
            titleId INTEGER NOT NULL PRIMARY KEY,
            rating REAL NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
    db.ensureColumn("ratings", "rating", "REAL NOT NULL DEFAULT 0")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS collections (
            titleId INTEGER NOT NULL,
            collectionType TEXT NOT NULL,
            titleName TEXT NOT NULL DEFAULT '',
            posterUrl TEXT,
            addedAt INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(titleId, collectionType)
        )
        """.trimIndent()
    )
    db.ensureColumn("collections", "titleName", "TEXT NOT NULL DEFAULT ''")
    db.ensureColumn("collections", "posterUrl", "TEXT")
    db.ensureColumn("collections", "addedAt", "INTEGER NOT NULL DEFAULT 0")
}

private fun SupportSQLiteDatabase.ensureColumn(table: String, column: String, definition: String) {
    if (!hasColumn(table, column)) {
        execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}
