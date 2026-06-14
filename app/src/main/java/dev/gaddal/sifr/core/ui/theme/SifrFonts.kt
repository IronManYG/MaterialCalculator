package dev.gaddal.sifr.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
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

// v1.8 — per-palette Arabic chrome fonts (translated UI text only). Weights chosen to exist
// on the Google Fonts provider; re-verify with the dependency-version-lookup skill if a weight
// fails to download (Tajawal has no 600; IBM Plex Sans Arabic / Cairo / Baloo Bhaijaan 2 do).
val Cairo = family("Cairo", 400, 500, 600, 700)
val Tajawal = family("Tajawal", 400, 500, 700)
val BalooBhaijaan2 = family("Baloo Bhaijaan 2", 400, 500, 600, 700)
val IBMPlexArabic = family("IBM Plex Sans Arabic", 400, 500, 600)

/** Material Typography keyed to the active palette's UI font family. */
@Composable
fun sifrTypography(sifr: SifrColors): Typography {
    val base = Typography()
    fun androidx.compose.ui.text.TextStyle.f() = copy(fontFamily = sifr.uiFamily)
    return Typography(
        displayLarge = base.displayLarge.f(),
        displayMedium = base.displayMedium.f(),
        displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(),
        headlineMedium = base.headlineMedium.f(),
        headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f(),
        titleMedium = base.titleMedium.f(),
        titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(),
        bodyMedium = base.bodyMedium.f(),
        bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(),
        labelMedium = base.labelMedium.f(),
        labelSmall = base.labelSmall.f(),
    )
}
