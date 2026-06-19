package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.ui.components.SifrCard
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.components.SifrRowDivider
import dev.gaddal.sifr.core.ui.components.SifrSubScreenTopBar
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import dev.gaddal.sifr.feature.tools.ui.components.CurrencySelect
import dev.gaddal.sifr.feature.tools.ui.components.Stepper
import dev.gaddal.sifr.feature.tools.ui.components.ToolField
import dev.gaddal.sifr.feature.tools.ui.components.ToolNumPad
import dev.gaddal.sifr.feature.tools.ui.components.ToolOut
import dev.gaddal.sifr.feature.tools.ui.components.ToolSelect
import dev.gaddal.sifr.feature.tools.ui.components.ToolTabBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun ToolsScreen(
    state: ToolsState,
    onAction: (ToolsAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = false,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            SifrSubScreenTopBar(
                title = stringResource(R.string.tools_title),
                onBack = { onAction(ToolsAction.BackClicked) },
                modifier = Modifier.statusBarsPadding(),
                onRotate = onRotate,
                rotateActive = rotateActive,
                rotateCd = stringResource(R.string.calc_rotate_orientation),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Spacer(Modifier.height(8.dp))
            ToolTabBar(
                selected = state.activeTab,
                onSelect = { onAction(ToolsAction.SelectTab(it)) },
            )
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (state.activeTab) {
                    ToolTab.Units -> UnitsCard(state, onAction)
                    ToolTab.Currency -> CurrencyCard(state, onAction)
                    ToolTab.Tip -> TipCard(state, onAction)
                    ToolTab.Date -> DateCards(state, onAction)
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.activeTab != ToolTab.Date || state.focusedField == FocusedField.AddDays) {
                // The Date tab only shows the pad when AddDays is focused; keep it compact there
                // (shorter keys) so the scrollable Date cards above don't get crowded (round-4
                // note E). The pad stays bottom-pinned with the cards scrolling above it.
                ToolNumPad(
                    onNumKey = { onAction(ToolsAction.NumKey(it)) },
                    onBackspace = { onAction(ToolsAction.Backspace) },
                    compact = state.activeTab == ToolTab.Date,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

// ── Units tab ───────────────────────────────────────────────────────────────
@Composable
internal fun UnitsCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitCategory.entries.forEach { cat ->
                SifrChip(
                    label = stringResource(
                        when (cat) {
                            UnitCategory.Length -> R.string.tools_cat_length
                            UnitCategory.Weight -> R.string.tools_cat_weight
                            UnitCategory.Temp -> R.string.tools_cat_temp
                            UnitCategory.Data -> R.string.tools_cat_data
                        }
                    ),
                    active = cat == state.unitsCat,
                    onClick = { onAction(ToolsAction.SelectCategory(cat)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SifrCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolSelect(
                    selected = state.uFrom,
                    options = state.unitsCat.units,
                    onSelect = { onAction(ToolsAction.SelectFromUnit(it)) },
                    modifier = Modifier.weight(1f),
                )
                ToolField(
                    value = state.uVal,
                    focused = state.focusedField == FocusedField.UVal,
                    onFocus = { onAction(ToolsAction.FocusField(FocusedField.UVal)) },
                    modifier = Modifier.weight(1f),
                )
            }
            SifrRowDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolSelect(
                    selected = state.uTo,
                    options = state.unitsCat.units,
                    onSelect = { onAction(ToolsAction.SelectToUnit(it)) },
                    modifier = Modifier.weight(1f),
                )
                ToolOut(
                    label = stringResource(R.string.tools_result),
                    value = state.uResult,
                    big = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Currency tab ────────────────────────────────────────────────────────────
@Composable
internal fun CurrencyCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    val snapshot = state.rates.snapshotOrNull
    val currencies = snapshot?.currencies ?: listOf("USD", "SAR", "AED", "EGP", "EUR", "GBP")

    Column(Modifier.padding(horizontal = 18.dp)) {
        SifrCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencySelect(
                    selected = state.cFrom,
                    currencies = currencies,
                    onSelect = { onAction(ToolsAction.SelectFromCurrency(it)) },
                    modifier = Modifier.weight(1f),
                )
                ToolField(
                    value = state.cVal,
                    focused = state.focusedField == FocusedField.CVal,
                    onFocus = { onAction(ToolsAction.FocusField(FocusedField.CVal)) },
                    modifier = Modifier.weight(1f),
                )
            }
            SifrRowDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencySelect(
                    selected = state.cTo,
                    currencies = currencies,
                    onSelect = { onAction(ToolsAction.SelectToCurrency(it)) },
                    modifier = Modifier.weight(1f),
                )
                ToolOut(
                    label = stringResource(R.string.tools_result),
                    value = state.cResult,
                    big = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val noteText = when (val r = state.rates) {
            is RatesResource.Loading -> ""
            is RatesResource.Success ->
                if (r.stale) stringResource(R.string.tools_fx_offline, r.snapshot.asOf.toString())
                else stringResource(R.string.tools_fx_note, r.snapshot.asOf.toString())
            is RatesResource.SeedFallback -> stringResource(R.string.tools_fx_seed)
        }
        if (noteText.isNotEmpty()) {
            // NOTE: no positive letterSpacing here — the app forbids positive letterSpacing under ar.
            Text(
                text = noteText,
                color = sifr.dim,
                fontFamily = sifr.uiFamily,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

// ── Tip tab ─────────────────────────────────────────────────────────────────
private val TIP_PERCENTS = listOf(10, 12, 15, 18, 20)

@Composable
internal fun TipCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    Column(Modifier.padding(horizontal = 18.dp)) {
        SifrCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.tools_bill),
                    color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 13.sp,
                    modifier = Modifier.weight(0.3f),
                )
                ToolField(
                    value = state.bill,
                    focused = state.focusedField == FocusedField.Bill,
                    onFocus = { onAction(ToolsAction.FocusField(FocusedField.Bill)) },
                    modifier = Modifier.weight(0.7f),
                )
            }
            SifrRowDivider()
            // Label on its own line, then the 5 presets spread across the full width — an inline
            // [label][5 chips] row overflowed once the label got long (e.g. Vietnamese "Tiền boa"),
            // crushing the last chip until "20%" wrapped to "2 0 %".
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.tools_tip), color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TIP_PERCENTS.forEach { pct ->
                        SifrChip(
                            label = "$pct%",
                            active = pct == state.tipPct,
                            onClick = { onAction(ToolsAction.SetTipPct(pct)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            SifrRowDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.tools_split), color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stepper(
                        value = state.split,
                        onDecrement = { onAction(ToolsAction.DecrementSplit) },
                        onIncrement = { onAction(ToolsAction.IncrementSplit) },
                    )
                    Text(stringResource(R.string.tools_people), color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = sifr.hairline, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolOut(label = stringResource(R.string.tools_out_tip), value = state.tipOut, modifier = Modifier.weight(1f))
                ToolOut(label = stringResource(R.string.tools_out_total), value = state.totalOut, modifier = Modifier.weight(1f))
                ToolOut(label = stringResource(R.string.tools_out_each), value = state.eachOut, big = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Date tab ────────────────────────────────────────────────────────────────
@Composable
internal fun DateCards(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    Column(
        Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DateDiffCard(state, onAction)
        DateAddCard(state, onAction)
    }
}

@Composable
internal fun DateDiffCard(state: ToolsState, onAction: (ToolsAction) -> Unit, modifier: Modifier = Modifier) {
    val sifr = SifrTokens.colors
    var showDate1Picker by remember { mutableStateOf(false) }
    var showDate2Picker by remember { mutableStateOf(false) }
    if (showDate1Picker) {
        ToolsDatePickerDialog(
            initial = state.date1,
            onDismiss = { showDate1Picker = false },
            onConfirm = { onAction(ToolsAction.SetDate1(it)); showDate1Picker = false },
        )
    }
    if (showDate2Picker) {
        ToolsDatePickerDialog(
            initial = state.date2,
            onDismiss = { showDate2Picker = false },
            onConfirm = { onAction(ToolsAction.SetDate2(it)); showDate2Picker = false },
        )
    }
    SifrCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.tools_date_diff), color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 11.sp, fontWeight = FontWeight.W600)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DateChip(date = state.date1, onClick = { showDate1Picker = true }, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = sifr.dim,
                    modifier = Modifier.size(18.dp),
                )
                DateChip(date = state.date2, onClick = { showDate2Picker = true }, modifier = Modifier.weight(1f))
            }
            // Two equal-width outputs, top-aligned and BOTH `big` so their labels sit on one
            // line and their values share one font size (matches the prototype's flex:1 columns;
            // the staggered/mismatched-size look was the round-2 note).
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                ToolOut(
                    label = stringResource(R.string.tools_days),
                    value = state.diffDays.toString(),
                    big = true,
                    modifier = Modifier.weight(1f),
                )
                ToolOut(
                    label = stringResource(R.string.tools_weeks),
                    value = "${state.diffWeeks}${stringResource(R.string.tools_w)}  ${state.diffRemainingDays}${stringResource(R.string.tools_d)}",
                    big = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun DateAddCard(state: ToolsState, onAction: (ToolsAction) -> Unit, modifier: Modifier = Modifier) {
    val sifr = SifrTokens.colors
    var showDate1Picker by remember { mutableStateOf(false) }
    if (showDate1Picker) {
        ToolsDatePickerDialog(
            initial = state.date1,
            onDismiss = { showDate1Picker = false },
            onConfirm = { onAction(ToolsAction.SetDate1(it)); showDate1Picker = false },
        )
    }
    SifrCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.tools_date_add), color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 11.sp, fontWeight = FontWeight.W600)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DateChip(date = state.date1, onClick = { showDate1Picker = true }, modifier = Modifier.weight(1f))
                Text("+", color = sifr.dim, fontSize = 16.sp)
                ToolField(
                    value = state.addDays,
                    focused = state.focusedField == FocusedField.AddDays,
                    onFocus = { onAction(ToolsAction.FocusField(FocusedField.AddDays)) },
                    placeholder = "0",
                    modifier = Modifier.weight(1f),
                )
            }
            val locale = LocalConfiguration.current.locales[0]
            val addFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale) }
            val resultText = state.addResult?.format(addFormatter) ?: "—"
            ToolOut(label = stringResource(R.string.tools_result), value = resultText, big = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = pickerState.selectedDateMillis
                if (ms != null) {
                    onConfirm(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                } else {
                    onDismiss()
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) { DatePicker(state = pickerState) }
}

@Composable
private fun DateChip(
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    SifrChip(
        label = date.format(formatter),
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
    )
}

// ── Previews ────────────────────────────────────────────────────────────────
@PreviewLightDark
@Composable
private fun PreviewToolsUnits() = SifrTheme(palette = SifrPalette.Layl) {
    ToolsScreen(state = ToolsState(activeTab = ToolTab.Units, uVal = "100", uResult = "0.1"), onAction = {})
}

@PreviewLightDark
@Composable
private fun PreviewToolsCurrencyLoading() = SifrTheme(palette = SifrPalette.Bayan) {
    ToolsScreen(state = ToolsState(activeTab = ToolTab.Currency, rates = RatesResource.Loading), onAction = {})
}

@PreviewLightDark
@Composable
private fun PreviewToolsTip() = SifrTheme(palette = SifrPalette.Raqim) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Tip, bill = "86", tipPct = 15, split = 2,
            tipOut = "12.9", totalOut = "98.9", eachOut = "49.45", focusedField = FocusedField.Bill,
        ),
        onAction = {},
    )
}

@PreviewLightDark
@Composable
private fun PreviewToolsDate() = SifrTheme(palette = SifrPalette.Farah) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Date,
            date1 = LocalDate.of(2026, 6, 10), date2 = LocalDate.of(2026, 8, 1),
            diffDays = 52, diffWeeks = 7, diffRemainingDays = 3,
        ),
        onAction = {},
    )
}

@Preview(name = "Tools Date — Arabic RTL", locale = "ar")
@Composable
private fun PreviewToolsDateAr() = SifrTheme(palette = SifrPalette.Farah) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Date,
            date1 = LocalDate.of(2026, 6, 10), date2 = LocalDate.of(2026, 8, 1),
            diffDays = 52, diffWeeks = 7, diffRemainingDays = 3,
        ),
        onAction = {},
    )
}

@Preview(name = "Tools Units — Arabic RTL", locale = "ar")
@Composable
private fun PreviewToolsUnitsAr() = SifrTheme(palette = SifrPalette.Layl) {
    ToolsScreen(
        state = ToolsState(activeTab = ToolTab.Units, uVal = "١٠٠", uResult = "٠٫١"),
        onAction = {},
    )
}

@PreviewLightDark
@Composable
private fun PreviewToolsCurrencyOffline() = SifrTheme(palette = SifrPalette.Mizan) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Currency,
            rates = RatesResource.Success(
                snapshot = RatesSnapshot(
                    base = "USD",
                    rates = mapOf("USD" to 1.0, "SAR" to 3.75),
                    asOf = LocalDate.of(2026, 6, 1),
                ),
                stale = true,
            ),
            cVal = "100", cFrom = "USD", cTo = "SAR", cResult = "375",
        ),
        onAction = {},
    )
}
