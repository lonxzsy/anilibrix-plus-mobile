package com.anilibrix.plus.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Переход 4 → 5 на реальных данных.
 *
 * Миграция переносит «Буду смотреть» из отдельной таблицы в коллекции и
 * добавляет истории серверный ключ серии. Ошибка здесь стоит человеку всего
 * списка — проверяем не «база открылась», а что записи действительно на месте
 * и в правильном виде.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AnilibrixDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate4To5_переноситСпискиИСохраняетИсторию() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO watch_later (titleId, titleName, posterUrl) " +
                    "VALUES (100, 'Отложенный тайтл', 'https://example/poster.jpg')"
            )
            execSQL(
                "INSERT INTO history (titleId, episodeId, episodeNumber, timestamp, duration, watchedAt, titleName, posterUrl) " +
                    "VALUES (200, 5, 5, 60000, 1440000, 111, 'Смотрю', NULL)"
            )
            execSQL(
                "INSERT INTO collections (titleId, collectionType, titleName, posterUrl, addedAt) " +
                    "VALUES (300, 'WATCHING', 'Уже в списке', NULL, 42)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // «Буду смотреть» переехало в коллекции.
        db.query("SELECT collectionType FROM collections WHERE titleId = 100").use { cursor ->
            assertTrue("запись из watch_later не перенесена", cursor.moveToFirst())
            assertEquals("WATCH_LATER", cursor.getString(0))
        }

        // Существующая коллекция не пострадала.
        db.query("SELECT titleName, addedAt FROM collections WHERE titleId = 300").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Уже в списке", cursor.getString(0))
            assertEquals(42L, cursor.getLong(1))
        }

        // История цела, новая колонка на месте и пустая.
        db.query("SELECT episodeNumber, timestamp, releaseEpisodeId FROM history WHERE titleId = 200").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
            assertEquals(60000L, cursor.getLong(1))
            assertEquals("", cursor.getString(2))
        }

        // Старая таблица удалена — иначе два источника правды разъедутся.
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='watch_later'").use { cursor ->
            assertFalse("watch_later должна быть удалена", cursor.moveToFirst())
        }

        // Новые таблицы созданы.
        listOf("downloads", "sync_operations").forEach { table ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use { cursor ->
                assertTrue("таблица $table не создана", cursor.moveToFirst())
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
