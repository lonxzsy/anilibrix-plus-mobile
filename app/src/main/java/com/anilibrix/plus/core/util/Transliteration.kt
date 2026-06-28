package com.anilibrix.plus.core.util

object Transliteration {

    private val cyrillicToLatin = mapOf(
        'а' to 'a', 'б' to 'b', 'в' to 'v', 'г' to 'g', 'д' to 'd',
        'е' to 'e', 'ё' to 'e', 'ж' to 'z', 'з' to 'z', 'и' to 'i',
        'й' to 'i', 'к' to 'k', 'л' to 'l', 'м' to 'm', 'н' to 'n',
        'о' to 'o', 'п' to 'p', 'р' to 'r', 'с' to 's', 'т' to 't',
        'у' to 'u', 'ф' to 'f', 'х' to 'h', 'ц' to 'c', 'ч' to 'c',
        'ш' to 's', 'щ' to 's', 'ъ' to '\'', 'ы' to 'y', 'ь' to '\'',
        'э' to 'e', 'ю' to 'u', 'я' to 'a',
        'А' to 'A', 'Б' to 'B', 'В' to 'V', 'Г' to 'G', 'Д' to 'D',
        'Е' to 'E', 'Ё' to 'E', 'Ж' to 'Z', 'З' to 'Z', 'И' to 'I',
        'Й' to 'I', 'К' to 'K', 'Л' to 'L', 'М' to 'M', 'Н' to 'N',
        'О' to 'O', 'П' to 'P', 'Р' to 'R', 'С' to 'S', 'Т' to 'T',
        'У' to 'U', 'Ф' to 'F', 'Х' to 'H', 'Ц' to 'C', 'Ч' to 'C',
        'Ш' to 'S', 'Щ' to 'S', 'Ъ' to '\'', 'Ы' to 'Y', 'Ь' to '\'',
        'Э' to 'E', 'Ю' to 'U', 'Я' to 'A'
    )

    private val digraphs = mapOf(
        "ж" to "zh", "з" to "z", "й" to "y", "х" to "kh",
        "ц" to "ts", "ч" to "ch", "ш" to "sh", "щ" to "shch",
        "ю" to "yu", "я" to "ya",
        "Ж" to "Zh", "З" to "Z", "Й" to "Y", "Х" to "Kh",
        "Ц" to "Ts", "Ч" to "Ch", "Ш" to "Sh", "Щ" to "Shch",
        "Ю" to "Yu", "Я" to "Ya"
    )

    fun transliterate(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (i + 1 <= text.length) {
                val pair = text.substring(i, minOf(i + 2, text.length))
                if (pair.length == 2) {
                    val mapped = digraphs[pair]
                    if (mapped != null) {
                        sb.append(mapped)
                        i += 2
                        continue
                    }
                }
            }
            val ch = text[i]
            sb.append(cyrillicToLatin[ch] ?: ch)
            i++
        }
        return sb.toString()
    }

    fun toSearchQuery(text: String): String {
        return transliterate(text)
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
