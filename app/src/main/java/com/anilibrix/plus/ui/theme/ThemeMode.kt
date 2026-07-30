package com.anilibrix.plus.ui.theme

/** Режим оформления, выбранный пользователем. */
enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: DARK
    }
}

/** Разрешает выбранный режим в конкретную «тёмность» с учётом системной темы. */
fun ThemeMode.resolveDark(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
