package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Pill segmented control (prototype ui-bits `SegButtons`): a hairline-bordered rounded
 * track where the selected segment fills with accent + accentInk text and the rest stay
 * dim. Each segment owns its own `selectable` tap, so indication stays confined to the
 * control (spec §4.7) — there is no whole-row click. Generic over the option type.
 *
 * Segments are always equal width. By default the track is content-sized: `width(IntrinsicSize.Max)`
 * on the Row + `weight(1f)` per segment makes every segment as wide as the WIDEST label (Compose
 * computes a weighted Row's intrinsic width as `max(intrinsic/weight) × totalWeight`), so
 * Mode / Angle / Restore read evenly instead of lopsided. [spanWidth] instead lets the segments
 * split the width the CALLER gives the control (e.g. a `fillMaxWidth()` tab bar) — no intrinsic
 * sizing, just weights filling the supplied track.
 */
@Composable
fun <T> SifrSegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    spanWidth: Boolean = false,
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            // Content-sized (each segment = widest label) unless the caller supplies the track width.
            .then(if (spanWidth) Modifier else Modifier.width(IntrinsicSize.Max))
            .clip(shape)
            .border(1.dp, sifr.hairline, shape),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // background before selectable so the tap ripple draws over the fill
                    .background(if (isSelected) sifr.accent else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    // Tighter side padding when the caller pins the width (fixed quarters) so a longer
                    // label still fits; roomier when the track is content-sized.
                    .padding(horizontal = if (spanWidth) 8.dp else 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) sifr.accentInk else sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Preview(name = "SifrSegmented — DEG/RAD (Layl dark)", showBackground = true)
@Composable
private fun PreviewSifrSegmented() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    Box(Modifier.padding(16.dp)) {
        SifrSegmented(
            options = listOf("System", "Light", "Dark"),
            selected = "Dark",
            label = { it },
            onSelect = {},
        )
    }
}
