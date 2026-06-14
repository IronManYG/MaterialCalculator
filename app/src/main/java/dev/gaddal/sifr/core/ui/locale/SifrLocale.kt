package dev.gaddal.sifr.core.ui.locale

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
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
 *
 * The override is a [ContextWrapper] around the Activity that swaps ONLY resources/
 * assets. A bare `createConfigurationContext()` returns a `ContextImpl` that is no
 * longer the Activity, which breaks every `LocalContext.current` consumer that needs
 * the Activity — `context.startActivity()` (the share sheet) throws without
 * FLAG_ACTIVITY_NEW_TASK, and `context as Activity` (Pulsar haptics) ClassCasts.
 * Wrapping the Activity keeps those delegations working while still localizing
 * resources. (A wrapper is still not `is Activity`, so consumers needing the literal
 * Activity must read it from `LocalView.current.context` instead — see FeedbackController.)
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
    val localizedContext = remember(configuration, tag, baseContext) {
        val config = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedResources = baseContext.createConfigurationContext(config).resources
        object : ContextWrapper(baseContext) {       // delegates to the Activity, serves localized resources
            override fun getResources(): Resources = localizedResources
            override fun getAssets(): AssetManager = localizedResources.assets
        }
    }
    val rtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}
