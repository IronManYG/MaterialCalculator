package dev.gaddal.sifr.feature.tools.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.domain.util.SifrNumberFormat
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.DateCalculator
import dev.gaddal.sifr.feature.tools.domain.TipCalculator
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import dev.gaddal.sifr.feature.tools.domain.UnitConverter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val KEY_TAB = "tools_tab"
private const val KEY_U_CAT = "tools_u_cat"
private const val KEY_U_FROM = "tools_u_from"
private const val KEY_U_TO = "tools_u_to"
private const val KEY_U_VAL = "tools_u_val"
private const val KEY_C_FROM = "tools_c_from"
private const val KEY_C_TO = "tools_c_to"
private const val KEY_C_VAL = "tools_c_val"
private const val KEY_BILL = "tools_bill"
private const val KEY_TIP_PCT = "tools_tip_pct"
private const val KEY_SPLIT = "tools_split"
private const val KEY_DATE1 = "tools_date1"
private const val KEY_DATE2 = "tools_date2"
private const val KEY_ADD_DAYS = "tools_add_days"

class ToolsViewModel(
    private val repository: CurrencyRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(restoreState())
    val state: StateFlow<ToolsState> = _state.asStateFlow()

    private val _events = Channel<ToolsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.rates.collect { resource ->
                _state.update { it.copy(rates = resource) }
                _state.update { it.copy(cResult = computeCurrencyResult(it)) }
            }
        }
        viewModelScope.launch { repository.refresh() }
    }

    fun onAction(action: ToolsAction) {
        when (action) {
            ToolsAction.BackClicked -> emit(ToolsEvent.NavigateBack)

            is ToolsAction.SelectTab -> {
                val defaultFocus = defaultFocusFor(action.tab)
                _state.update { it.copy(activeTab = action.tab, focusedField = defaultFocus) }
                persist(KEY_TAB, action.tab.ordinal)
            }

            is ToolsAction.FocusField -> _state.update { it.copy(focusedField = action.field) }

            is ToolsAction.NumKey -> appendChar(action.char)
            ToolsAction.Backspace -> deleteChar()

            is ToolsAction.SelectCategory -> {
                val cat = action.cat
                val newFrom = cat.units.first()
                val newTo = cat.units.getOrElse(1) { cat.units.first() }
                _state.update {
                    val updated = it.copy(unitsCat = cat, uFrom = newFrom, uTo = newTo)
                    updated.copy(uResult = computeUnitResult(updated))
                }
                persist(KEY_U_CAT, cat.ordinal)
                persist(KEY_U_FROM, newFrom)
                persist(KEY_U_TO, newTo)
            }
            is ToolsAction.SelectFromUnit -> {
                _state.update { s ->
                    val updated = s.copy(uFrom = action.unit)
                    updated.copy(uResult = computeUnitResult(updated))
                }
                persist(KEY_U_FROM, action.unit)
            }
            is ToolsAction.SelectToUnit -> {
                _state.update { s ->
                    val updated = s.copy(uTo = action.unit)
                    updated.copy(uResult = computeUnitResult(updated))
                }
                persist(KEY_U_TO, action.unit)
            }

            is ToolsAction.SelectFromCurrency -> {
                _state.update { s ->
                    val updated = s.copy(cFrom = action.code)
                    updated.copy(cResult = computeCurrencyResult(updated))
                }
                persist(KEY_C_FROM, action.code)
            }
            is ToolsAction.SelectToCurrency -> {
                _state.update { s ->
                    val updated = s.copy(cTo = action.code)
                    updated.copy(cResult = computeCurrencyResult(updated))
                }
                persist(KEY_C_TO, action.code)
            }
            ToolsAction.RefreshRates -> viewModelScope.launch {
                val prevRates = _state.value.rates
                repository.refresh(force = true)
                if (_state.value.rates === prevRates) {
                    _events.send(ToolsEvent.RatesRefreshFailed)
                }
            }

            is ToolsAction.SetTipPct -> {
                _state.update { s ->
                    val updated = s.copy(tipPct = action.pct)
                    updated.copy(
                        tipOut = computeTipOut(updated),
                        totalOut = computeTotalOut(updated),
                        eachOut = computeEachOut(updated),
                    )
                }
                persist(KEY_TIP_PCT, action.pct)
            }
            ToolsAction.IncrementSplit -> changeSplit(+1)
            ToolsAction.DecrementSplit -> changeSplit(-1)

            is ToolsAction.SetDate1 -> {
                _state.update { s ->
                    val updated = s.copy(date1 = action.date)
                    val days = DateCalculator.daysBetween(updated.date1, updated.date2)
                    val (w, d) = DateCalculator.weeksAndDays(days)
                    updated.copy(
                        diffDays = days,
                        diffWeeks = w,
                        diffRemainingDays = d,
                        addResult = computeAddResult(updated),
                    )
                }
                persist(KEY_DATE1, action.date.toEpochDay())
            }
            is ToolsAction.SetDate2 -> {
                _state.update { s ->
                    val updated = s.copy(date2 = action.date)
                    val days = DateCalculator.daysBetween(updated.date1, updated.date2)
                    val (w, d) = DateCalculator.weeksAndDays(days)
                    updated.copy(
                        diffDays = days,
                        diffWeeks = w,
                        diffRemainingDays = d,
                    )
                }
                persist(KEY_DATE2, action.date.toEpochDay())
            }
        }
    }

    private fun appendChar(char: Char) {
        val field = _state.value.focusedField ?: return
        val current = getField(field)
        if (char == '.' && current.contains('.')) return
        val next = when {
            char.isDigit() && current == "0" -> char.toString()
            else -> if (current.length >= 12) current else current + char
        }
        setField(field, next)
    }

    private fun deleteChar() {
        val field = _state.value.focusedField ?: return
        val current = getField(field)
        setField(field, if (current.length <= 1) "" else current.dropLast(1))
    }

    private fun getField(field: FocusedField): String = when (field) {
        FocusedField.UVal -> _state.value.uVal
        FocusedField.CVal -> _state.value.cVal
        FocusedField.Bill -> _state.value.bill
        FocusedField.AddDays -> _state.value.addDays
    }

    private fun setField(field: FocusedField, value: String) {
        _state.update { s ->
            when (field) {
                FocusedField.UVal -> {
                    val updated = s.copy(uVal = value)
                    updated.copy(uResult = computeUnitResult(updated)).also { persist(KEY_U_VAL, value) }
                }
                FocusedField.CVal -> {
                    val updated = s.copy(cVal = value)
                    updated.copy(cResult = computeCurrencyResult(updated)).also { persist(KEY_C_VAL, value) }
                }
                FocusedField.Bill -> {
                    val updated = s.copy(bill = value)
                    updated.copy(
                        tipOut = computeTipOut(updated),
                        totalOut = computeTotalOut(updated),
                        eachOut = computeEachOut(updated),
                    ).also { persist(KEY_BILL, value) }
                }
                FocusedField.AddDays -> {
                    val updated = s.copy(addDays = value)
                    updated.copy(addResult = computeAddResult(updated)).also { persist(KEY_ADD_DAYS, value) }
                }
            }
        }
    }

    private fun computeUnitResult(s: ToolsState): String {
        val v = s.uVal.toDoubleOrNull() ?: return ""
        val r = UnitConverter.convert(s.unitsCat, v, s.uFrom, s.uTo)
        return SifrNumberFormat.format(r)
    }

    private fun computeCurrencyResult(s: ToolsState): String {
        val snapshot = s.rates.snapshotOrNull ?: return ""
        val v = s.cVal.toDoubleOrNull() ?: return ""
        val fromRate = snapshot.rates[s.cFrom] ?: return ""
        val toRate = snapshot.rates[s.cTo] ?: return ""
        if (fromRate == 0.0) return ""
        val r = v / fromRate * toRate
        return SifrNumberFormat.format(r)
    }

    private fun computeTipOut(s: ToolsState): String {
        val b = s.bill.toDoubleOrNull() ?: return ""
        return SifrNumberFormat.format(TipCalculator.compute(b, s.tipPct, s.split).tip)
    }

    private fun computeTotalOut(s: ToolsState): String {
        val b = s.bill.toDoubleOrNull() ?: return ""
        return SifrNumberFormat.format(TipCalculator.compute(b, s.tipPct, s.split).total)
    }

    private fun computeEachOut(s: ToolsState): String {
        val b = s.bill.toDoubleOrNull() ?: return ""
        return SifrNumberFormat.format(TipCalculator.compute(b, s.tipPct, s.split).each)
    }

    private fun computeAddResult(s: ToolsState): LocalDate? {
        val days = s.addDays.toLongOrNull() ?: return null
        return DateCalculator.addDays(s.date1, days)
    }

    private fun changeSplit(delta: Int) {
        _state.update { s ->
            val newSplit = (s.split + delta).coerceAtLeast(1)
            val updated = s.copy(split = newSplit)
            updated.copy(
                tipOut = computeTipOut(updated),
                totalOut = computeTotalOut(updated),
                eachOut = computeEachOut(updated),
            ).also { persist(KEY_SPLIT, newSplit) }
        }
    }

    private fun restoreState(): ToolsState {
        val tabOrdinal = savedStateHandle.get<Int>(KEY_TAB) ?: 0
        val tab = ToolTab.fromOrdinal(tabOrdinal)

        val catOrdinal = savedStateHandle.get<Int>(KEY_U_CAT) ?: 0
        val cat = UnitCategory.entries.getOrElse(catOrdinal) { UnitCategory.Length }
        val uFrom = savedStateHandle.get<String>(KEY_U_FROM) ?: cat.units.first()
        val uTo = savedStateHandle.get<String>(KEY_U_TO) ?: cat.units.getOrElse(1) { cat.units.first() }
        val uVal = savedStateHandle.get<String>(KEY_U_VAL) ?: ""

        val cFrom = savedStateHandle.get<String>(KEY_C_FROM) ?: "USD"
        val cTo = savedStateHandle.get<String>(KEY_C_TO) ?: "SAR"
        val cVal = savedStateHandle.get<String>(KEY_C_VAL) ?: ""

        val bill = savedStateHandle.get<String>(KEY_BILL) ?: ""
        val tipPct = savedStateHandle.get<Int>(KEY_TIP_PCT) ?: 15
        val split = savedStateHandle.get<Int>(KEY_SPLIT) ?: 1

        val today = LocalDate.now()
        val date1 = savedStateHandle.get<Long>(KEY_DATE1)?.let { LocalDate.ofEpochDay(it) } ?: today
        val date2 = savedStateHandle.get<Long>(KEY_DATE2)?.let { LocalDate.ofEpochDay(it) } ?: today
        val addDays = savedStateHandle.get<String>(KEY_ADD_DAYS) ?: ""

        val (diffW, diffD) = DateCalculator.weeksAndDays(DateCalculator.daysBetween(date1, date2))

        val base = ToolsState(
            activeTab = tab,
            unitsCat = cat, uFrom = uFrom, uTo = uTo, uVal = uVal,
            cFrom = cFrom, cTo = cTo, cVal = cVal,
            bill = bill, tipPct = tipPct, split = split,
            date1 = date1, date2 = date2, addDays = addDays,
            diffDays = DateCalculator.daysBetween(date1, date2),
            diffWeeks = diffW, diffRemainingDays = diffD,
            focusedField = defaultFocusFor(tab),
        )
        return base.copy(
            uResult = computeUnitResult(base),
            tipOut = computeTipOut(base),
            totalOut = computeTotalOut(base),
            eachOut = computeEachOut(base),
            addResult = computeAddResult(base),
        )
    }

    private fun persist(key: String, value: Any) {
        savedStateHandle[key] = value
    }

    private fun defaultFocusFor(tab: ToolTab): FocusedField? = when (tab) {
        ToolTab.Units -> FocusedField.UVal
        ToolTab.Currency -> FocusedField.CVal
        ToolTab.Tip -> FocusedField.Bill
        ToolTab.Date -> null
    }

    private fun emit(event: ToolsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
