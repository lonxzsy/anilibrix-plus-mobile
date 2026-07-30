package com.anilibrix.plus.data.sync

import com.anilibrix.plus.domain.model.CollectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Соответствие статусов и оценок обеим сторонам.
 *
 * Ошибка здесь не видна в приложении вовсе — она проявляется чужим статусом в
 * профиле пользователя на shikimori.one, поэтому проверяется весь набор
 * целиком, а не выборочно.
 */
class ShikimoriStatusMapperTest {

    @Test
    fun `все пять статусов переводятся туда и обратно без потерь`() {
        CollectionType.entries.forEach { type ->
            val shikimori = ShikimoriStatusMapper.toShikimori(type)
            assertEquals(
                "статус $type не пережил обратный перевод",
                type,
                ShikimoriStatusMapper.fromShikimori(shikimori),
            )
        }
    }

    @Test
    fun `регистр статуса с сервера не имеет значения`() {
        assertEquals(CollectionType.WATCHING, ShikimoriStatusMapper.fromShikimori("Watching"))
        assertEquals(CollectionType.ON_HOLD, ShikimoriStatusMapper.fromShikimori("ON_HOLD"))
    }

    @Test
    fun `пересматриваю приравнивается к смотрю`() {
        assertEquals(CollectionType.WATCHING, ShikimoriStatusMapper.fromShikimori("rewatching"))
    }

    @Test
    fun `неизвестный статус не угадывается`() {
        assertNull(ShikimoriStatusMapper.fromShikimori(""))
        assertNull(ShikimoriStatusMapper.fromShikimori("something_new"))
    }

    @Test
    fun `оценка переводится в обе стороны без потери`() {
        for (stars in 0..5) {
            val rating = stars.toFloat()
            val score = ShikimoriStatusMapper.scoreToShikimori(rating)
            assertEquals("оценка $rating → $score → обратно", rating, ShikimoriStatusMapper.scoreFromShikimori(score), 0.001f)
        }
    }

    @Test
    fun `оценка ограничена шкалой Shikimori`() {
        assertEquals(10, ShikimoriStatusMapper.scoreToShikimori(9f))
        assertEquals(0, ShikimoriStatusMapper.scoreToShikimori(-1f))
        assertEquals(5f, ShikimoriStatusMapper.scoreFromShikimori(99), 0.001f)
        assertEquals(0f, ShikimoriStatusMapper.scoreFromShikimori(-5), 0.001f)
    }
}
