package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Settings / History list row (prototype ui-bits `SifrRow`): an optional [leading] icon, a
 * [label] with an optional [sub]title, and an optional [trailing] control, 16×13 padding.
 *
 * The row is NOT clickable by default — a trailing control (SifrToggle / SifrSegmented)
 * owns its own tap and indication (spec §4.7, confined ripple — see [[confined-ripple-controls]]).
 * Pass [onClick] only for a genuinely whole-row action (e.g. a row that opens a dialog).
 */
@Composable
fun SifrRow(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val sifr = SifrTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, color = sifr.text, fontFamily = sifr.uiFamily, fontSize = 14.5.sp)
            if (sub != null) {
                Text(text = sub, color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 11.5.sp)
            }
        }
        trailing?.invoke()
    }
}

@Preview(name = "SifrRow — label+sub+toggle (Layl dark)", showBackground = true)
@Composable
private fun PreviewSifrRow() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    SifrCard(Modifier.padding(16.dp)) {
        SifrRow(
            label = "Memory keys",
            sub = "Show MC, M+, M−, MR above the keypad",
            trailing = { SifrToggle(checked = true, onCheckedChange = {}) },
        )
        SifrRowDivider()
        SifrRow(
            label = "Error sounds",
            trailing = { SifrToggle(checked = false, onCheckedChange = {}) },
        )
    }
}
