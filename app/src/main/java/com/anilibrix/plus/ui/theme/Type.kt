package com.anilibrix.plus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.anilibrix.plus.R

/**
 * Inter Variable (SIL OFL 1.1), см. LICENSE-Inter.txt.
 *
 * Раньше `InterFontFamily` был просто FontFamily.Default — имя врало, реально
 * рисовался Roboto, а папки res/font не существовало вовсе.
 *
 * `variationSettings` здесь несущая часть: без него платформа берёт ближайший
 * статический инстанс, и Medium/SemiBold молча рендерятся как Regular.
 * Проверено fontTools: 96/96 кириллических кодпоинтов U+0400–045F, оси
 * wght 100–900 и opsz 14–32.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val InterFontFamily = FontFamily(
    interWeight(300),
    interWeight(400),
    interWeight(500),
    interWeight(600),
    interWeight(700),
    interWeight(800),
)

/**
 * Натуральная высота строки Inter — 1.21 em (ascender 1984 / descender −494
 * при unitsPerEm 2048). Это выше, чем у Roboto, поэтому при
 * `includeFontPadding = false` кириллица с выносными элементами (Й, Щ, ф, р)
 * обрезалась бы без явного LineHeightStyle.
 *
 * Замечание: у displayLarge (57/64 = 1.12) и displayMedium (45/52 = 1.156)
 * lineHeight по спеке НИЖЕ натуральных 1.21. Сейчас обе роли в приложении не
 * используются; если начнём — брать displaySmall (1.222) либо поднимать
 * lineHeight, иначе глиф вылезет за строку.
 */
private val InterPlatformStyle = PlatformTextStyle(includeFontPadding = false)

private val InterLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun interStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    platformStyle = InterPlatformStyle,
    lineHeightStyle = InterLineHeightStyle,
)

val AnilibrixTypography = Typography(
    displayLarge = interStyle(FontWeight.Normal, 57, 64, -0.25),
    displayMedium = interStyle(FontWeight.Normal, 45, 52),
    displaySmall = interStyle(FontWeight.Normal, 36, 44),
    headlineLarge = interStyle(FontWeight.Normal, 32, 40),
    headlineMedium = interStyle(FontWeight.Normal, 28, 36),
    headlineSmall = interStyle(FontWeight.Normal, 24, 32),
    titleLarge = interStyle(FontWeight.Medium, 22, 28),
    titleMedium = interStyle(FontWeight.Medium, 16, 24, 0.15),
    titleSmall = interStyle(FontWeight.Medium, 14, 20, 0.1),
    bodyLarge = interStyle(FontWeight.Normal, 16, 24, 0.5),
    bodyMedium = interStyle(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = interStyle(FontWeight.Normal, 12, 16, 0.4),
    labelLarge = interStyle(FontWeight.Medium, 14, 20, 0.1),
    labelMedium = interStyle(FontWeight.Medium, 12, 16, 0.5),
    labelSmall = interStyle(FontWeight.Medium, 11, 16, 0.5),
)

/**
 * Emphasized-роли MD3 Expressive. В material3 1.4.0 Typography ещё не содержит
 * этих слотов, поэтому они живут сбоку — под теми же именами, что появятся в 1.5,
 * чтобы будущая миграция была переименованием, а не переписыванием.
 *
 * Использовать экономно: заголовок hero, заголовки экранов, выбранный таб,
 * основной CTA, заголовки секций. Если акцент везде — он не значит ничего.
 */
object AnilibrixTypeExtras {
    val displaySmallEmphasized = AnilibrixTypography.displaySmall
        .copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp)
    val headlineLargeEmphasized = AnilibrixTypography.headlineLarge
        .copy(fontWeight = FontWeight.SemiBold)
    val headlineMediumEmphasized = AnilibrixTypography.headlineMedium
        .copy(fontWeight = FontWeight.SemiBold)
    val headlineSmallEmphasized = AnilibrixTypography.headlineSmall
        .copy(fontWeight = FontWeight.SemiBold)
    val titleLargeEmphasized = AnilibrixTypography.titleLarge
        .copy(fontWeight = FontWeight.SemiBold)
    val titleMediumEmphasized = AnilibrixTypography.titleMedium
        .copy(fontWeight = FontWeight.SemiBold)
    val titleSmallEmphasized = AnilibrixTypography.titleSmall
        .copy(fontWeight = FontWeight.SemiBold)
    val bodyLargeEmphasized = AnilibrixTypography.bodyLarge
        .copy(fontWeight = FontWeight.Medium)
    val labelLargeEmphasized = AnilibrixTypography.labelLarge
        .copy(fontWeight = FontWeight.SemiBold)
    val labelMediumEmphasized = AnilibrixTypography.labelMedium
        .copy(fontWeight = FontWeight.SemiBold)
}
