package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.toDisplayExpression

/**
 * Tape in-display receipt (prototype display.jsx `isTape` branch): a centered pull-handle that opens
 * the full History screen, above the last 3 history entries shown as dashed rows with rising opacity
 * (oldest faint at top → newest brightest at the bottom). Each row reads "expression … result" and
 * restores its result on tap. Shown only when keypadLayout == Tape; sits at the top of the display.
 *
 * Math mirrors with the locale (hugs the start edge — right in Arabic, left in English) like the
 * History rows and Settings, but each line still *reads* left-to-right (forced text direction).
 */
@Composable
fun TapeReceipt(
    entries: List<HistoryEntry>,
    onOpenHistory: () -> Unit,
    onRestore: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
    // The display's DEG/M chips, tucked onto the leading edge of the pull-handle row so the
    // receipt reclaims the standalone chips-band row in the squeezed Tape layout. Null = none.
    chips: (@Composable () -> Unit)? = null,
    // Scientific mode squeezes the display; `compact` shrinks the row fonts and gaps so the
    // input + result + COPY block below still fits.
    compact: Boolean = false,
    // How many recent rows to show. Tight layouts (scientific, or a surface palette whose
    // card/inset has less usable height) pass 2 so the main lines clear; roomy flat palettes
    // in basic mode pass 3 (the prototype default).
    maxRows: Int = 3,
) {
    val sifr = SifrTokens.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val mathAlign = if (rtl) TextAlign.Right else TextAlign.Left
    val ltrMath = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    val handleLabel = stringResource(R.string.calc_open_history)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 2.dp else 4.dp),
    ) {
        // Pull-handle → History, centered, with the DEG/M chips hugging the leading edge of
        // the same row. Confined ripple: the clipped, padded Box owns the handle tap; its
        // rounded hit area hugs the bar rather than spanning the row.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (chips != null) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) { chips() }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onOpenHistory)
                    .semantics {
                        contentDescription = handleLabel
                        role = Role.Button
                    }
                    .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(sifr.hairline),
                )
            }
        }

        // Newest renders last (bottom) with the highest opacity (prototype's reversed slice +
        // rising ramp). Tight layouts pass maxRows = 2 to keep the input + result + COPY block
        // clear of the surface palettes' card/inset.
        val rows = entries.take(maxRows.coerceAtLeast(1)).reversed()
        rows.forEachIndexed { i, entry ->
            // Ramp from 0.3 (oldest) to 0.8 (newest); single row sits at 0.8.
            val rowAlpha = if (rows.size <= 1) 0.8f else 0.3f + 0.5f * i / (rows.size - 1)
            TapeRow(
                entry = entry,
                rowAlpha = rowAlpha,
                mathAlign = mathAlign,
                ltrStyle = ltrMath,
                compact = compact,
                onRestore = { onRestore(entry) },
            )
        }
    }
}

@Composable
private fun TapeRow(
    entry: HistoryEntry,
    rowAlpha: Float,
    mathAlign: TextAlign,
    ltrStyle: TextStyle,
    compact: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    val hairline = sifr.hairline
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Button role + clickable merges the expression+result texts into one SR node
            // ("12+5 17, double-tap to activate") instead of two stray fragments.
            .clickable(role = Role.Button, onClick = onRestore)
            .alpha(rowAlpha)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2f
                drawLine(
                    color = hairline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                        0f,
                    ),
                )
            }
            .padding(vertical = if (compact) 2.dp else 3.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = entry.expression.toDisplayExpression(),
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
            color = sifr.dim,
            fontFamily = sifr.displayFamily,
            fontSize = if (compact) 12.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = mathAlign,
            style = ltrStyle,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.result,
            modifier = Modifier.alignByBaseline(),
            color = sifr.displayExpression,
            fontFamily = sifr.displayFamily,
            fontSize = if (compact) 13.sp else 14.sp,
            maxLines = 1,
            style = ltrStyle,
        )
    }
}

// region previews

private val previewEntries = listOf(
    HistoryEntry(1, "12+5", "17", 1_700_000_000_000),
    HistoryEntry(2, "100x0.15", "15", 1_699_999_940_000),
    HistoryEntry(3, "(8-3)x6", "30", 1_699_996_400_000),
)

private val previewChips: @Composable () -> Unit = {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SifrChip(label = "DEG", active = false)
    }
}

@Composable
private fun TapeReceiptPreviewFrame(
    palette: SifrPalette,
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) = SifrTheme(palette = palette, themeMode = themeMode) {
    Box(
        modifier = Modifier
            .background(SifrTokens.colors.backgroundFlat)
            .padding(24.dp),
    ) { content() }
}

@Preview(name = "Tape — Farah light")
@PreviewFontScale
@Composable
private fun TapeReceiptPreviewFarahLight() =
    TapeReceiptPreviewFrame(SifrPalette.Farah, ThemeMode.Light) {
        TapeReceipt(
            entries = previewEntries,
            onOpenHistory = {},
            onRestore = {},
            chips = previewChips,
        )
    }

@Preview(name = "Tape — Layl dark")
@Composable
private fun TapeReceiptPreviewLaylDark() =
    TapeReceiptPreviewFrame(SifrPalette.Layl, ThemeMode.Dark) {
        TapeReceipt(
            entries = previewEntries,
            onOpenHistory = {},
            onRestore = {},
            chips = previewChips,
        )
    }

@Preview(name = "Tape — compact (scientific)")
@Composable
private fun TapeReceiptPreviewCompact() =
    TapeReceiptPreviewFrame(SifrPalette.Mizan, ThemeMode.Dark) {
        TapeReceipt(
            entries = previewEntries,
            onOpenHistory = {},
            onRestore = {},
            chips = previewChips,
            compact = true,
            maxRows = 2,
        )
    }

@Preview(name = "Tape — overflow")
@Composable
private fun TapeReceiptPreviewOverflow() =
    TapeReceiptPreviewFrame(SifrPalette.Bayan, ThemeMode.Light) {
        TapeReceipt(
            entries = listOf(
                HistoryEntry(1, "1234567+89012345x67890-11111", "5000000.5", 1_700_000_000_000),
                HistoryEntry(2, "9+9", "18", 1_699_999_940_000),
            ),
            onOpenHistory = {},
            onRestore = {},
        )
    }

@Preview(name = "Tape — Arabic / RTL", locale = "ar")
@Composable
private fun TapeReceiptPreviewArabic() =
    TapeReceiptPreviewFrame(SifrPalette.Mizan, ThemeMode.Dark) {
        TapeReceipt(entries = previewEntries, onOpenHistory = {}, onRestore = {})
    }

@Preview(name = "Tape — empty (handle only)")
@Composable
private fun TapeReceiptPreviewEmpty() =
    TapeReceiptPreviewFrame(SifrPalette.Farah, ThemeMode.Light) {
        TapeReceipt(entries = emptyList(), onOpenHistory = {}, onRestore = {})
    }

// endregion
