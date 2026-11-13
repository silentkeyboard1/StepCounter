
package com.example.stepcounter.ui.theme

import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.stepcounter.R

val AppFontFamily = FontFamily(
    Font(
        R.font.oxanium_regular,
        FontWeight.Normal
    ),
    Font(
        R.font.oxanium_bold,
        FontWeight.Bold
    ),
)

private val defaultTypography = MaterialTypography()

val Typography = MaterialTypography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = AppFontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = AppFontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = AppFontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = AppFontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = AppFontFamily)
)
