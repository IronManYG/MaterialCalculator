package dev.gaddal.sifr.core.ui.locale

import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.gaddal.sifr.core.domain.settings.AppLanguage
import java.util.Locale

/**
 * Applies the in-app [language] to the subtree by overriding the resource Context,
 * Configuration, and layout direction — live, in composition, no Activity recreate.
 *
 * Two device-tested requirements (see spec §3/§5):
 *  - SYNCHRONOUS apply (no background AppLocalizer) so strings can't lag the recomposition.
 *  - Re-derived on every config change (the [configuration] key) so a non-recreating
 *    rotation can't revert a forced app-locale back to the device locale.
 * No `key(locale){}` is used: Android `stringResource(R.string)` reads LocalContext/
 * LocalConfiguration, so providing them re-resolves strings in place — ViewModels,
 * scroll, and SavedStateHandle survive the switch.
 */
@Composable
fun SifrLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val tag = language.tag
    if (tag == null) {            // System → follow device; no override, no forced direction
        content()
        return
    }
    val configuration = LocalConfiguration.current
    val baseContext = LocalContext.current
    val locale = remember(tag) { Locale.forLanguageTag(tag) }
    val localizedContext = remember(configuration, tag) {
        val config = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        baseContext.createConfigurationContext(config)
    }
    val rtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}
