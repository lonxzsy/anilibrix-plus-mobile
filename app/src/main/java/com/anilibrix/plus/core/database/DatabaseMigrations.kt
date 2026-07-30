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

/**
 * Единственная содержательная миграция проекта — все предыдущие лишь
 * догоняли схему до актуальной.
 *
 * Здесь три разных по смыслу изменения, объединённые намеренно: каждое из них
 * по отдельности потребовало бы своей версии базы, а пользователю от этого
 * ничего бы не стало лучше.
 *
 *  1. `history.releaseEpisodeId` — серверный ключ таймкода. Без него удаление
 *     записи из истории невозможно донести до сервера, и удалённое возвращалось
 *     при следующей синхронизации.
 *  2. `watch_later` сливается в `collections`. Это были две независимые
 *     сущности с одинаковым смыслом: локальная таблица и серверная коллекция
 *     WATCH_LATER, которые никогда не встречались друг с другом.
 *  3. Новые таблицы `downloads` и `sync_operations` — для офлайн-загрузок и
 *     очереди синхронизации.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        ensureCurrentSchema(db)

        // Перенос «Буду смотреть» в коллекции. INSERT OR IGNORE, а не REPLACE:
        // если тайтл уже лежит в collections как WATCH_LATER (пришёл с сервера),
        // серверная запись новее и её addedAt терять не надо.
        if (db.hasTable("watch_later")) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO collections (titleId, collectionType, titleName, posterUrl, addedAt, updatedAt, shikimoriId, progressEpisode)
                SELECT titleId, 'WATCH_LATER', titleName, posterUrl, 0, 0, NULL, 0 FROM watch_later
                """.trimIndent()
            )
            db.execSQL("DROP TABLE watch_later")
        }
    }
}

val ANILIBRIX_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5
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
    db.ensureColumn("history", "releaseEpisodeId", "TEXT NOT NULL DEFAULT ''")

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
    // updatedAt — арбитр в конфликтах синхронизации; shikimoriId — связь с
    // внешним трекером; progressEpisode — номер последней просмотренной серии,
    // который отправляется в Shikimori как есть.
    db.ensureColumn("collections", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
    db.ensureColumn("collections", "shikimoriId", "INTEGER")
    db.ensureColumn("collections", "progressEpisode", "INTEGER NOT NULL DEFAULT 0")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS downloads (
            requestId TEXT NOT NULL PRIMARY KEY,
            titleId INTEGER NOT NULL,
            titleName TEXT NOT NULL DEFAULT '',
            posterUrl TEXT,
            episodeId INTEGER NOT NULL DEFAULT 0,
            releaseEpisodeId TEXT NOT NULL DEFAULT '',
            episodeNumber INTEGER NOT NULL DEFAULT 0,
            episodeName TEXT NOT NULL DEFAULT '',
            quality TEXT NOT NULL DEFAULT '',
            durationMs INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_downloads_titleId ON downloads(titleId)")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS sync_operations (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            kind TEXT NOT NULL,
            titleId INTEGER NOT NULL DEFAULT 0,
            payload TEXT NOT NULL DEFAULT '',
            createdAt INTEGER NOT NULL DEFAULT 0,
            attempts INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
}

private fun SupportSQLiteDatabase.hasTable(table: String): Boolean {
    query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
        return cursor.moveToFirst()
    }
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
