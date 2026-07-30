package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Отложенное действие, которое нужно донести до сервера.
 *
 * До этого синхронизация была односторонней и запускалась только при открытии
 * вкладки «Профиль»: поставленный без сети статус просто терялся, а человек об
 * этом не узнавал. Теперь любое действие сначала записывается локально (UI
 * реагирует мгновенно), а сюда кладётся запись о том, что должно уехать наружу.
 *
 * [payload] — JSON, потому что у разных операций разная форма, а заводить пять
 * таблиц ради очереди из десятка записей не окупается.
 */
@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Значение [com.anilibrix.plus.core.sync.SyncOperationKind]. */
    val kind: String,
    val titleId: Long = 0L,
    val payload: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Сколько раз операцию уже пытались выполнить. Нужен, чтобы навсегда
     * отравленная запись (удалённый на сервере тайтл, отозванный доступ) не
     * блокировала очередь вечно.
     */
    val attempts: Int = 0,
)
