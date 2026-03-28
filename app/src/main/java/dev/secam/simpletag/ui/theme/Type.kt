
package dev.secam.simpletag.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.secam.simpletag.R

@OptIn(ExperimentalTextApi::class)
val figtreeFamily = FontFamily(
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600)
        )
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800)
        )
    )
)
val baseline = Typography()
val Typography = Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = figtreeFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = figtreeFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = figtreeFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = figtreeFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = figtreeFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = figtreeFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = figtreeFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = figtreeFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = figtreeFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = figtreeFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = figtreeFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = figtreeFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = figtreeFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = figtreeFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = figtreeFamily),
)