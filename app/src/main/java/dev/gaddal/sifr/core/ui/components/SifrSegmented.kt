package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
 * [equalWidth] makes the segments split the track evenly (each `weight(1f)`) instead of
 * hugging their labels — use it when the control is given a fixed width (e.g. a full-width
 * tab bar) so the segments fill it rather than clustering at the start and leaving a gap.
 */
@Composable
fun <T> SifrSegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    equalWidth: Boolean = false,
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, sifr.hairline, shape),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .then(if (equalWidth) Modifier.weight(1f) else Modifier)
                    // background before selectable so the tap ripple draws over the fill
                    .background(if (isSelected) sifr.accent else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) sifr.accentInk else sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 12.sp,
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
