package dev.gaddal.sifr.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.gaddal.sifr.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun family(name: String, vararg weights: Int): FontFamily =
    FontFamily(weights.map { Font(GoogleFont(name), provider, FontWeight(it)) })

val SpaceGrotesk = family("Space Grotesk", 400, 500, 600)
val Archivo = family("Archivo", 500, 700, 800, 900)
val Cormorant = family("Cormorant Garamond", 300, 400, 500)
val Baloo2 = family("Baloo 2", 500, 600, 700)
val PlexMono = family("IBM Plex Mono", 400, 500)
val Amiri = family("Amiri", 400, 700) // Arabic wordmark صفر (used from v1.5+)

// TODO(A3): restore sifrTypography once SifrColors is introduced in Task A3.
// At that point, un-comment the block below, add the @Composable import,
// and add the androidx.compose.material3.Typography import.
//
// @Composable
// fun sifrTypography(sifr: SifrColors): Typography {
//     val base = Typography()
//     fun androidx.compose.ui.text.TextStyle.f() = copy(fontFamily = sifr.uiFamily)
//     return Typography(
//         displayLarge  = base.displayLarge.f(),  displayMedium = base.displayMedium.f(),  displaySmall  = base.displaySmall.f(),
//         headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
//         titleLarge    = base.titleLarge.f(),    titleMedium   = base.titleMedium.f(),    titleSmall    = base.titleSmall.f(),
//         bodyLarge     = base.bodyLarge.f(),     bodyMedium    = base.bodyMedium.f(),     bodySmall     = base.bodySmall.f(),
//         labelLarge    = base.labelLarge.f(),    labelMedium   = base.labelMedium.f(),    labelSmall    = base.labelSmall.f(),
//     )
// }
