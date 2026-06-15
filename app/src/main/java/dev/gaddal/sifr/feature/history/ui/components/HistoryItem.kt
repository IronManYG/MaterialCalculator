package dev.gaddal.sifr.feature.history.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.toDisplayExpression

/**
 * One history entry rendered as a row inside the History [dev.gaddal.sifr.core.ui.components.SifrCard]
 * (prototype HistoryScreen row): a dim expression line, a large "= result" line, a compact localized
 * timestamp, and a confined ✕ to delete. The whole row restores the result on tap.
 *
 * Math is forced left-to-right (like the calculator display) even under an Arabic/RTL layout.
 */
@Composable
fun HistoryItem(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    val context = LocalContext.current
    val locales = LocalConfiguration.current.locales
    val formattedDate = remember(entry.timestampMs, locales) {
        // "May 13, 7:27 PM" — abbreviated month, no year, short time (locale-aware).
        DateUtils.formatDateTime(
            context,
            entry.timestampMs,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_SHOW_TIME,
        )
    }
    // Positive tracking breaks Arabic letter-joins — apply the wide timestamp tracking in LTR only.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // History mirrors with the locale like Settings: the math hugs the start edge (right in Arabic,
    // left in English). It still *reads* left-to-right (forced text direction) so a "(" isn't
    // mirrored and the leading "=" stays before the number — only the block's alignment flips.
    val mathAlign = if (rtl) TextAlign.Right else TextAlign.Left
    val ltrMath = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Button role merges expression + result + timestamp into one tap-to-restore SR node;
            // the trailing delete IconButton stays a separate node (its own clickable boundary).
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.expression.toDisplayExpression(),
                modifier = Modifier.fillMaxWidth(),
                color = sifr.dim,
                fontFamily = sifr.displayFamily,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = mathAlign,
                style = ltrMath,
            )
            Text(
                text = "= ${entry.result}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                color = sifr.text,
                fontFamily = sifr.displayFamily,
                fontSize = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = mathAlign,
                style = ltrMath,
            )
            Text(
                text = formattedDate,
                modifier = Modifier.padding(top = 3.dp),
                color = sifr.dim,
                fontFamily = sifr.uiFamily,
                fontSize = 10.5.sp,
                letterSpacing = if (rtl) TextUnit.Unspecified else 0.04.em,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.history_delete_entry),
                tint = sifr.dim,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
