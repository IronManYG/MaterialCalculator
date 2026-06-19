package dev.gaddal.sifr.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

private val TRACK_W = 42.dp
private val TRACK_H = 24.dp
private val THUMB = 20.dp
private val PAD = 2.dp

/**
 * Custom Sifr toggle (spec §4.4 / decision D3): 42×24 pill, 20 thumb, accent on / hairline off,
 * 150ms. The toggleable (click + ripple) is on the 48dp wrapper so the target is a11y-compliant
 * but the visual pill stays 42×24 — and crucially the indication is confined to THIS control,
 * never a parent row (spec §4.7 / [[confined-ripple-controls]]).
 */
@Composable
fun SifrToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    val track by animateColorAsState(if (checked) sifr.accent else sifr.hairline, tween(150), label = "track")
    val thumbOffset by animateDpAsState(if (checked) TRACK_W - THUMB - PAD else PAD, tween(150), label = "thumb")

    Box(
        modifier = modifier
            .toggleable(
                value = checked,
                // No ripple: the pill's own colour/thumb slide IS the indication
                // (user request; keeps a clean control, consistent with the rest of
                // the design system's confined-indication rule).
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(12.dp),                    // expands the touch target to ≥48dp around the 42×24 pill
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(TRACK_W, TRACK_H)
                .clip(RoundedCornerShape(50))
                .background(track),
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .align(Alignment.CenterStart)
                    .shadow(2.dp, CircleShape)
                    .size(THUMB)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Preview(name = "SifrToggle — on/off, Layl dark", showBackground = true)
@Composable
private fun PreviewSifrToggle() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    Row {
        SifrToggle(checked = true, onCheckedChange = {})
        SifrToggle(checked = false, onCheckedChange = {})
    }
}
