package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.tools.domain.CurrencyDisplay
import java.util.Locale

/**
 * Currency selector. Renders a field-like anchor (flag + code) that opens a searchable
 * [ModalBottomSheet] of all available currencies — replacing the plain dropdown, which was
 * hard to scan across ~160 codes. Each row shows flag · code · localized name and filters by
 * code or name. Sheet open-state and the query are local UI mechanics (matching ToolSelect's
 * local `expanded`); the selection itself flows up via [onSelect].
 */
@Composable
fun CurrencySelect(
    selected: String,
    currencies: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, sifr.hairline, shape)
            .clickable { open = true }
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CurrencyDisplay.flag(selected)?.let { Text(it, fontSize = 16.sp) }
        Text(
            text = selected,
            fontFamily = sifr.uiFamily,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            color = sifr.text,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = sifr.dim)
    }

    if (open) {
        CurrencyPickerSheet(
            currencies = currencies,
            selected = selected,
            onSelect = { onSelect(it); open = false },
            onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    currencies: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sifr = SifrTokens.colors
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(currencies, query, locale) {
        if (query.isBlank()) {
            currencies
        } else {
            currencies.filter { code ->
                code.contains(query, ignoreCase = true) ||
                    CurrencyDisplay.name(code, locale).contains(query, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sifr.surface.compositeOver(sifr.backgroundFlat),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.tools_currency_pick),
                color = sifr.text,
                fontFamily = sifr.uiFamily,
                fontWeight = FontWeight.W600,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = sifr.dim) },
                placeholder = {
                    Text(stringResource(R.string.tools_currency_search), color = sifr.dim, fontFamily = sifr.uiFamily)
                },
                textStyle = TextStyle(fontFamily = sifr.uiFamily, color = sifr.text, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.tools_currency_none),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(filtered, key = { it }) { code ->
                        CurrencyRow(
                            code = code,
                            locale = locale,
                            selected = code == selected,
                            onClick = { onSelect(code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    locale: Locale,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val sifr = SifrTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 💱 fallback for the supranational / metal codes that have no country flag.
        Text(CurrencyDisplay.flag(code) ?: "💱", fontSize = 20.sp)
        Text(
            text = code,
            fontFamily = sifr.uiFamily,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            color = if (selected) sifr.accent else sifr.text,
        )
        Text(
            text = CurrencyDisplay.name(code, locale),
            fontFamily = sifr.uiFamily,
            fontSize = 13.sp,
            color = sifr.dim,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = sifr.accent)
        }
    }
}
