# Tools — Presentation Layer (Plan 2 of 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the Tools feature end-to-end — MVI state/action/event/ViewModel, navigation hook-up, all shared and feature-local UI components, four tab screens (Units · Currency · Tip · Date), strings in both locales (en + ar), and preview coverage — so tapping the `⊞` top-bar icon opens a live, fully-interactive Tools screen.

**Pre-condition:** Plan 1 (`2026-06-14-tools-foundation-data.md`) is fully executed. All domain types, data layer, and Koin wiring in `feature/tools/di/ToolsModule.kt` + `di/AppModule.kt` already exist and the build is green.

**Branch:** `feature/tools-module` (already checked out). Commit per task. Do **not** merge.

**Conventions (hard rules):**
- Build with `sh gradlew` — never `./gradlew`.
- Tests: JUnit4 (`org.junit.Test`) + Google Truth (`assertThat(x).isEqualTo(y)`), not JUnit assertions.
- Conventional Commits, **no** `Co-Authored-By` / AI footer.
- **Any version literal** must come from the `dependency-version-lookup` skill — do not invent versions.
- New files: `git add` immediately.
- `ToolsViewModel` must be registered with `viewModelOf` — not manually constructed — for SavedStateHandle injection to work.

**Spec:** `docs/superpowers/specs/2026-06-14-tools-module-design.md` (§3–§10). Prototype source of truth: `docs/design_handoff_sifr_redesign/reference_prototype/app/tools.jsx`.

---

## File Structure

Created in this plan:

```
app/src/main/java/dev/gaddal/sifr/feature/tools/
  ui/
    ToolTab.kt                // enum Units/Currency/Tip/Date + displayLabel() helper
    ToolsState.kt             // @Stable data class
    ToolsAction.kt            // sealed interface
    ToolsEvent.kt             // sealed interface
    ToolsViewModel.kt         // SavedStateHandle-backed VM, Channel events
    ToolsRoot.kt              // koinViewModel + ObserveAsEvents + windowSizeClass split
    ToolsScreen.kt            // portrait stateless composable
    ToolsScreenLandscape.kt   // landscape stateless composable
    components/
      ToolNumPad.kt           // 3-col × 4-row numpad over CalculatorButton
      ToolField.kt            // tappable display-font value box
      ToolSelect.kt           // ExposedDropdownMenuBox wrapper
      ToolOut.kt              // labelled output; big variant
      Stepper.kt              // −  value  + pill
      ToolTabBar.kt           // SifrSegmented<ToolTab> wrapper

app/src/main/res/
  values/strings.xml          // tools_* keys appended
  values-ar/strings.xml       // Arabic translations appended

app/src/main/java/dev/gaddal/sifr/navigation/
  Routes.kt                   // + ToolsRoute

app/src/main/java/dev/gaddal/sifr/navigation/
  NavRoot.kt                  // + entry<ToolsRoute>

app/src/main/java/dev/gaddal/sifr/feature/calculator/domain/
  CalculatorAction.kt         // + ToolsClicked

app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/
  CalculatorEvent.kt          // + NavigateToTools
  CalculatorViewModel.kt      // handle ToolsClicked → emit NavigateToTools
  CalculatorScreen.kt         // add onTools param + un-gate the callback
  components/CalculatorScreenLandscape.kt  // same

app/src/test/java/dev/gaddal/sifr/feature/tools/ui/
  ToolsViewModelTest.kt       // Turbine state tests
```

Modified: `feature/tools/di/ToolsModule.kt` (add `viewModelOf(::ToolsViewModel)`).

---

## Task 1: `ToolTab` enum

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolTab.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.ui

/**
 * The four Tools tabs, in display order. [ordinal] is used for SavedStateHandle persistence.
 * [stringKey] is the R.string key name (looked up by the VM via a resource-string lambda).
 */
enum class ToolTab {
    Units,
    Currency,
    Tip,
    Date,
    ;

    companion object {
        fun fromOrdinal(i: Int): ToolTab = entries.getOrElse(i) { Units }
    }
}
```

- [ ] **Step 2: Add & build**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolTab.kt
```

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(tools): ToolTab enum (Units/Currency/Tip/Date)"
```

---

## Task 2: MVI types — `ToolsState`, `ToolsAction`, `ToolsEvent`

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsState.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsAction.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsEvent.kt`

- [ ] **Step 1: Implement `ToolsState.kt`**

The `focusedField` sentinel uses the same `ToolTab` for routing numpad input. `rates` reuses the existing domain type. Derived results (unit conversion, currency conversion, tip math, date diff) are computed in the ViewModel and exposed as pre-formatted strings in state so the UI reads pure strings — no logic in composables.

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.runtime.Immutable
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import java.time.LocalDate

/**
 * Which input field owns the numpad. Null = no field focused (Date tab does not use the numpad).
 */
enum class FocusedField { UVal, CVal, Bill, AddDays }

/**
 * Single source of truth for the Tools screen. All derived output is pre-formatted here
 * (in the ViewModel) so every UI composable is a pure read.
 *
 * [uResult], [cResult], [tipResult], [dateResult] are "" when input is blank or conversion
 * produces NaN — the ViewModel guards before calling SifrNumberFormat.format().
 */
@Immutable
data class ToolsState(
    // ── Tab ──────────────────────────────────────────────────────────────────
    val activeTab: ToolTab = ToolTab.Units,

    // ── Units ─────────────────────────────────────────────────────────────────
    val unitsCat: UnitCategory = UnitCategory.Length,
    val uFrom: String = UnitCategory.Length.units.first(),       // "m"
    val uTo: String = UnitCategory.Length.units[1],             // "km"
    val uVal: String = "",                                       // raw string from numpad
    val uResult: String = "",                                    // formatted output

    // ── Currency ──────────────────────────────────────────────────────────────
    val cFrom: String = "USD",
    val cTo: String = "SAR",
    val cVal: String = "",
    val cResult: String = "",
    val rates: RatesResource = RatesResource.Loading,

    // ── Tip ───────────────────────────────────────────────────────────────────
    val bill: String = "",
    val tipPct: Int = 15,
    val split: Int = 1,
    val tipOut: String = "",     // formatted tip amount
    val totalOut: String = "",   // formatted total
    val eachOut: String = "",    // formatted each-person share

    // ── Date ──────────────────────────────────────────────────────────────────
    val date1: LocalDate = LocalDate.now(),
    val date2: LocalDate = LocalDate.now(),
    val addDays: String = "",
    val diffDays: Long = 0L,          // signed; updated when dates change
    val diffWeeks: Int = 0,
    val diffRemainingDays: Int = 0,
    val addResult: LocalDate? = null, // null until addDays is valid

    // ── Numpad focus ──────────────────────────────────────────────────────────
    val focusedField: FocusedField? = FocusedField.UVal,
)
```

- [ ] **Step 2: Implement `ToolsAction.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import java.time.LocalDate

sealed interface ToolsAction {
    // Navigation
    data object BackClicked : ToolsAction

    // Tab
    data class SelectTab(val tab: ToolTab) : ToolsAction

    // Numpad (feeds the currently-focused field)
    data class NumKey(val char: Char) : ToolsAction
    data object Backspace : ToolsAction
    data class FocusField(val field: FocusedField) : ToolsAction

    // Units
    data class SelectCategory(val cat: UnitCategory) : ToolsAction
    data class SelectFromUnit(val unit: String) : ToolsAction
    data class SelectToUnit(val unit: String) : ToolsAction

    // Currency
    data class SelectFromCurrency(val code: String) : ToolsAction
    data class SelectToCurrency(val code: String) : ToolsAction
    data object RefreshRates : ToolsAction

    // Tip
    data class SetTipPct(val pct: Int) : ToolsAction
    data object IncrementSplit : ToolsAction
    data object DecrementSplit : ToolsAction

    // Date
    data class SetDate1(val date: LocalDate) : ToolsAction
    data class SetDate2(val date: LocalDate) : ToolsAction
}
```

- [ ] **Step 3: Implement `ToolsEvent.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui

sealed interface ToolsEvent {
    data object NavigateBack : ToolsEvent
    /** Emitted after a background refresh fails and the UI should show a brief note. */
    data object RatesRefreshFailed : ToolsEvent
}
```

- [ ] **Step 4: Add & build**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsState.kt \
        app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsAction.kt \
        app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsEvent.kt
```

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(tools): MVI types — ToolsState / ToolsAction / ToolsEvent"
```

---

## Task 3: `ToolsViewModel`

SavedStateHandle persists tab + all field strings + unit/currency selections + tip config + dates. Currency rates reload from cache on `init`. The VM is the only place that calls `SifrNumberFormat.format` — the composables receive pre-formatted strings.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsViewModel.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.domain.util.SifrNumberFormat
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.DateCalculator
import dev.gaddal.sifr.feature.tools.domain.RatesResource
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
private const val KEY_DATE1 = "tools_date1"         // stored as epoch-day Long
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
        // Observe live rates and push them into state.
        viewModelScope.launch {
            repository.rates.collect { resource ->
                _state.update { it.copy(rates = resource) }
                // Recalculate currency result when rates arrive.
                _state.update { it.copy(cResult = computeCurrencyResult(it)) }
            }
        }
        // Kick the repository to serve cache/seed then background-refresh.
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

            // ── Units ────────────────────────────────────────────────────────
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

            // ── Currency ──────────────────────────────────────────────────────
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
                // Failure is signalled by rates staying stale/seed; emit event if unchanged.
                if (_state.value.rates === prevRates) {
                    _events.send(ToolsEvent.RatesRefreshFailed)
                }
            }

            // ── Tip ───────────────────────────────────────────────────────────
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

            // ── Date ──────────────────────────────────────────────────────────
            is ToolsAction.SetDate1 -> {
                _state.update { s ->
                    val updated = s.copy(date1 = action.date)
                    updated.copy(
                        diffDays = DateCalculator.daysBetween(updated.date1, updated.date2),
                        diffWeeks = DateCalculator.weeksAndDays(DateCalculator.daysBetween(updated.date1, updated.date2)).first,
                        diffRemainingDays = DateCalculator.weeksAndDays(DateCalculator.daysBetween(updated.date1, updated.date2)).second,
                    )
                }
                persist(KEY_DATE1, action.date.toEpochDay())
            }
            is ToolsAction.SetDate2 -> {
                _state.update { s ->
                    val updated = s.copy(date2 = action.date)
                    updated.copy(
                        diffDays = DateCalculator.daysBetween(updated.date1, updated.date2),
                        diffWeeks = DateCalculator.weeksAndDays(DateCalculator.daysBetween(updated.date1, updated.date2)).first,
                        diffRemainingDays = DateCalculator.weeksAndDays(DateCalculator.daysBetween(updated.date1, updated.date2)).second,
                    )
                }
                persist(KEY_DATE2, action.date.toEpochDay())
            }
        }
    }

    // ── Numpad plumbing ────────────────────────────────────────────────────────

    private fun appendChar(char: Char) {
        val field = _state.value.focusedField ?: return
        val current = getField(field)
        // Validate: at most one '.'; max 12 chars; replace leading "0" with digit
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

    // ── Derived computations ────────────────────────────────────────────────────

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

    // ── SavedStateHandle persistence ──────────────────────────────────────────

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
        // Pre-compute derived outputs (rates still Loading at this point; currency stays "").
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
        ToolTab.Date -> null  // Date tab uses the system keyboard / DatePicker
    }

    private fun emit(event: ToolsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
```

- [ ] **Step 2: Register in `ToolsModule.kt`**

Open `app/src/main/java/dev/gaddal/sifr/feature/tools/di/ToolsModule.kt` and add the import and ViewModel registration at the end of the `module { }` block:

```kotlin
import dev.gaddal.sifr.feature.tools.ui.ToolsViewModel
import org.koin.core.module.dsl.viewModelOf
// ... inside module { } ...
    viewModelOf(::ToolsViewModel)
```

> Use the `org.koin.core.module.dsl.viewModelOf` import — **not** the older
> `org.koin.androidx.viewmodel.dsl` one. The `core.module.dsl` path is what
> `CalculatorModule.kt` uses for `CalculatorViewModel`, the only other VM that
> takes a `SavedStateHandle`, and it is the path that auto-injects the handle.
> Do not pass `SavedStateHandle` manually.

- [ ] **Step 3: Build**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsViewModel.kt
git add app/src/main/java/dev/gaddal/sifr/feature/tools/di/ToolsModule.kt
```

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(tools): ToolsViewModel (SavedStateHandle, currency flow, derived outputs)"
```

---

## Task 4: ViewModel tests

**Files:**
- Create: `app/src/test/java/dev/gaddal/sifr/feature/tools/ui/ToolsViewModelTest.kt`

> These tests require `kotlinx-coroutines-test` and Turbine (already in the dependency graph from Plan 1 via the existing test suite). Use `SavedStateHandle()` constructor directly — it accepts an initial map.

- [ ] **Step 1: Write tests**

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class ToolsViewModelTest {

    private val seedSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.75, "EUR" to 0.87),
        asOf = LocalDate.of(2026, 6, 10),
    )

    private inner class FakeRepo : CurrencyRepository {
        val flow = MutableStateFlow<RatesResource>(RatesResource.Loading)
        override val rates: Flow<RatesResource> = flow
        override suspend fun refresh(force: Boolean) {
            flow.value = RatesResource.SeedFallback(seedSnapshot)
        }
    }

    private fun vm(
        repo: CurrencyRepository = FakeRepo(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = ToolsViewModel(repo, handle)

    @Test
    fun `initial tab is Units with UVal focused`() = runTest {
        val v = vm()
        v.state.test {
            val s = awaitItem()
            assertThat(s.activeTab).isEqualTo(ToolTab.Units)
            assertThat(s.focusedField).isEqualTo(FocusedField.UVal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectTab changes tab and resets focus to tab default`() = runTest {
        val v = vm()
        v.state.test {
            awaitItem() // initial
            v.onAction(ToolsAction.SelectTab(ToolTab.Tip))
            val s = awaitItem()
            assertThat(s.activeTab).isEqualTo(ToolTab.Tip)
            assertThat(s.focusedField).isEqualTo(FocusedField.Bill)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Date tab sets focusedField to null`() = runTest {
        val v = vm()
        v.state.test {
            awaitItem()
            v.onAction(ToolsAction.SelectTab(ToolTab.Date))
            val s = awaitItem()
            assertThat(s.focusedField).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `numpad builds uVal and recomputes uResult`() = runTest {
        val v = vm()
        v.state.test {
            awaitItem()
            v.onAction(ToolsAction.NumKey('1'))
            v.onAction(ToolsAction.NumKey('0'))
            v.onAction(ToolsAction.NumKey('0'))
            val s = awaitItem()
            // After batching 3 emissions — just check the final state
            cancelAndIgnoreRemainingEvents()
        }
        // Query state directly after
        val s = v.state.value
        assertThat(s.uVal).isEqualTo("100")
        // 100 m → km = 0.1
        assertThat(s.uResult).isEqualTo("0.1")
    }

    @Test
    fun `Backspace removes last char`() = runTest {
        val v = vm()
        v.onAction(ToolsAction.NumKey('5'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.Backspace)
        assertThat(v.state.value.uVal).isEqualTo("5")
    }

    @Test
    fun `SelectCategory resets from and to to category defaults`() = runTest {
        val v = vm()
        v.onAction(ToolsAction.SelectCategory(UnitCategory.Weight))
        val s = v.state.value
        assertThat(s.unitsCat).isEqualTo(UnitCategory.Weight)
        assertThat(s.uFrom).isEqualTo("kg")
        assertThat(s.uTo).isEqualTo("g")
    }

    @Test
    fun `IncrementSplit increases split and recomputes tip`() = runTest {
        val v = vm()
        v.onAction(ToolsAction.SelectTab(ToolTab.Tip))
        // Enter bill = 100
        v.onAction(ToolsAction.NumKey('1'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.IncrementSplit)
        val s = v.state.value
        assertThat(s.split).isEqualTo(2)
        // bill=100, tip=15%, total=115, each=57.5
        assertThat(s.eachOut).isEqualTo("57.5")
    }

    @Test
    fun `SavedStateHandle restores tab and field values`() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                "tools_tab" to ToolTab.Currency.ordinal,
                "tools_c_from" to "EUR",
                "tools_c_to" to "USD",
                "tools_c_val" to "50",
            )
        )
        val v = vm(handle = handle)
        val s = v.state.value
        assertThat(s.activeTab).isEqualTo(ToolTab.Currency)
        assertThat(s.cFrom).isEqualTo("EUR")
        assertThat(s.cVal).isEqualTo("50")
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.ui.ToolsViewModelTest"`
Expected: PASS. (If `numpad builds uVal` emits 3 intermediate states, use `cancelAndIgnoreRemainingEvents()` + query `v.state.value` directly after the test block — the test above does this.)

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/dev/gaddal/sifr/feature/tools/ui/ToolsViewModelTest.kt
git commit -m "test(tools): ViewModel — tab, numpad, category reset, split, SavedStateHandle"
```

---

## Task 5: Navigation wiring

Four surgical edits: add the route, add the nav entry, add `ToolsClicked` action + event, un-gate both top bars.

**Files:**
- Modify: `app/src/main/java/dev/gaddal/sifr/navigation/Routes.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/navigation/NavRoot.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/calculator/domain/CalculatorAction.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorEvent.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorViewModel.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorScreen.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/components/CalculatorScreenLandscape.kt`

**Note:** `ToolsRoot` doesn't exist until Task 9, but `NavRoot.kt` must reference it now. To keep the build green at every step, Step 8 below creates a minimal `ToolsRoot` stub (a centered `Box`) that Task 9 replaces wholesale.

- [ ] **Step 1: Add `ToolsRoute` to `Routes.kt`**

Append after `HistoryRoute`:

```kotlin
@Serializable
data object ToolsRoute : NavKey
```

- [ ] **Step 2: Add `ToolsClicked` to `CalculatorAction.kt`**

Append to the sealed interface after `HistoryClicked`:

```kotlin
    data object ToolsClicked : CalculatorAction
```

- [ ] **Step 3: Add `NavigateToTools` to `CalculatorEvent.kt`**

Append to the sealed interface:

```kotlin
    data object NavigateToTools : CalculatorEvent
```

- [ ] **Step 4: Handle `ToolsClicked` in `CalculatorViewModel.kt`**

In `onAction(action: CalculatorAction)`, add a branch alongside `HistoryClicked`:

```kotlin
            CalculatorAction.ToolsClicked -> emit(CalculatorEvent.NavigateToTools)
```

- [ ] **Step 5: Un-gate `onTools` in `CalculatorScreen.kt`**

In `CalculatorScreen`, add an `onTools: () -> Unit = {}` parameter to the composable and replace the hard-coded `onTools = {}` in the `SifrCalcTopBar` call:

Current:
```kotlin
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = false,
)
```

Change to:
```kotlin
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = false,
    onTools: () -> Unit = {},
)
```

And in the `SifrCalcTopBar` call inside the composable body, change:
```kotlin
                onTools = {},                              // gated: Tools screen ships in a later milestone
```
to:
```kotlin
                onTools = dropUnlessResumed { onTools() },
```

- [ ] **Step 6: Un-gate `onTools` in `CalculatorScreenLandscape.kt`**

Same pattern — add `onTools: () -> Unit = {}` to `CalculatorScreenLandscape`'s parameter list and wire the `SifrCalcTopBar`:

Change:
```kotlin
fun CalculatorScreenLandscape(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = true,
)
```

to:
```kotlin
fun CalculatorScreenLandscape(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = true,
    onTools: () -> Unit = {},
)
```

And inside the `SifrCalcTopBar` call:
```kotlin
                onTools = dropUnlessResumed { onTools() },
```

- [ ] **Step 7: Wire `onTools` in `CalculatorRoot` (in `CalculatorScreen.kt`)**

In `CalculatorRoot`, add `onNavigateToTools: () -> Unit` parameter and:
1. In `ObserveAsEvents` block add:
   ```kotlin
   CalculatorEvent.NavigateToTools -> onNavigateToTools()
   ```
2. Pass `onTools = onNavigateToTools` to both `CalculatorScreen(...)` and `CalculatorScreenLandscape(...)` calls.

Also add `CalculatorAction.ToolsClicked` dispatch in `CalculatorViewModel.onAction` is already done in Step 4; the Root's `onAction = viewModel::onAction` already routes it.

- [ ] **Step 8: Create a compile-time stub `ToolsRoot.kt`**

Create a minimal stub so `NavRoot.kt` can import it. This will be replaced entirely in Task 9:

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ToolsRoot(
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tools — coming soon")
    }
}
```

- [ ] **Step 9: Add `entry<ToolsRoute>` in `NavRoot.kt`**

Add the import and the entry:
```kotlin
import dev.gaddal.sifr.feature.tools.ui.ToolsRoot
// in entryProvider { ... }
                entry<ToolsRoute> {
                    ToolsRoot(
                        windowSizeClass = windowSizeClass,
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }
```

Also update `CalculatorRoute` entry to pass `onNavigateToTools`:
```kotlin
                entry<CalculatorRoute> {
                    CalculatorRoot(
                        windowSizeClass = windowSizeClass,
                        onNavigateToSettings = { backStack.add(SettingsRoute) },
                        onNavigateToHistory = { backStack.add(HistoryRoute) },
                        onNavigateToTools = { backStack.add(ToolsRoute) },
                    )
                }
```

- [ ] **Step 10: Build**

```bash
git add app/src/main/java/dev/gaddal/sifr/navigation/Routes.kt \
        app/src/main/java/dev/gaddal/sifr/navigation/NavRoot.kt \
        app/src/main/java/dev/gaddal/sifr/feature/calculator/domain/CalculatorAction.kt \
        app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorEvent.kt \
        app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorViewModel.kt \
        app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/CalculatorScreen.kt \
        app/src/main/java/dev/gaddal/sifr/feature/calculator/ui/components/CalculatorScreenLandscape.kt \
        app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsRoot.kt
```

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. The `⊞` top-bar icon now navigates to a "coming soon" placeholder.

- [ ] **Step 11: Commit**

```bash
git commit -m "feat(tools): navigation wiring — ToolsRoute, ToolsClicked action, un-gate top-bar buttons"
```

---

## Task 6: Strings (both locales)

Lint enforces `MissingTranslation` across both locale dirs. Add all keys to both files in one pass.

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ar/strings.xml`

- [ ] **Step 1: Append to `values/strings.xml`** (before `</resources>`)

```xml
    <!-- Tools screen -->
    <string name="tools_title">Tools</string>
    <string name="tools_back">Back</string>
    <string name="tools_tab_units">Units</string>
    <string name="tools_tab_currency">Currency</string>
    <string name="tools_tab_tip">Tip</string>
    <string name="tools_tab_date">Date</string>

    <!-- Units tab -->
    <string name="tools_cat_length">Length</string>
    <string name="tools_cat_weight">Weight</string>
    <string name="tools_cat_temp">Temp</string>
    <string name="tools_cat_data">Data</string>
    <string name="tools_result">Result</string>

    <!-- Currency tab -->
    <string name="tools_fx_note">Rates as of %1$s</string>
    <string name="tools_fx_offline">Offline — using saved rates (%1$s)</string>
    <string name="tools_fx_seed">Approx rates (bundled)</string>

    <!-- Tip tab -->
    <string name="tools_bill">Bill</string>
    <string name="tools_tip">Tip</string>
    <string name="tools_split">Split</string>
    <string name="tools_people">people</string>
    <string name="tools_out_tip">Tip</string>
    <string name="tools_out_total">Total</string>
    <string name="tools_out_each">Each</string>

    <!-- Date tab -->
    <string name="tools_date_diff">Date Difference</string>
    <string name="tools_date_add">Add Days</string>
    <string name="tools_days">Days</string>
    <string name="tools_weeks">Weeks</string>
    <string name="tools_w">w</string>
    <string name="tools_d">d</string>
```

- [ ] **Step 2: Append to `values-ar/strings.xml`** (before `</resources>`)

```xml
    <!-- Tools screen -->
    <string name="tools_title">الأدوات</string>
    <string name="tools_back">رجوع</string>
    <string name="tools_tab_units">الوحدات</string>
    <string name="tools_tab_currency">العملات</string>
    <string name="tools_tab_tip">الإكرامية</string>
    <string name="tools_tab_date">التاريخ</string>

    <!-- Units tab -->
    <string name="tools_cat_length">الطول</string>
    <string name="tools_cat_weight">الوزن</string>
    <string name="tools_cat_temp">الحرارة</string>
    <string name="tools_cat_data">البيانات</string>
    <string name="tools_result">النتيجة</string>

    <!-- Currency tab -->
    <string name="tools_fx_note">الأسعار بتاريخ %1$s</string>
    <string name="tools_fx_offline">غير متصل — تستخدم الأسعار المحفوظة (%1$s)</string>
    <string name="tools_fx_seed">أسعار تقريبية (مدمجة)</string>

    <!-- Tip tab -->
    <string name="tools_bill">الفاتورة</string>
    <string name="tools_tip">الإكرامية</string>
    <string name="tools_split">القسمة</string>
    <string name="tools_people">أشخاص</string>
    <string name="tools_out_tip">إكرامية</string>
    <string name="tools_out_total">المجموع</string>
    <string name="tools_out_each">لكل شخص</string>

    <!-- Date tab -->
    <string name="tools_date_diff">فارق التاريخ</string>
    <string name="tools_date_add">إضافة أيام</string>
    <string name="tools_days">أيام</string>
    <string name="tools_weeks">أسابيع</string>
    <string name="tools_w">أ</string>
    <string name="tools_d">ي</string>
```

- [ ] **Step 3: Lint check**

Run: `sh gradlew :app:lintDebug`
Expected: no `MissingTranslation` errors. Fix any missed keys before committing.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-ar/strings.xml
git commit -m "feat(tools): localized strings — en + ar for all tools_* keys"
```

---

## Task 7: Shared UI components (`feature/tools/ui/components/`)

These are feature-local, not in `core/ui/`. Build them in dependency order: `ToolField` and `ToolOut` are pure display; `ToolNumPad` depends on `CalculatorButton`; `ToolSelect` uses M3 `ExposedDropdownMenuBox`; `Stepper` is self-contained; `ToolTabBar` wraps `SifrSegmented`.

All components use `SifrTokens.colors` for palette colors. No hardcoded colors.

- [ ] **Step 1: `ToolTabBar.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.components.SifrSegmented
import dev.gaddal.sifr.feature.tools.ui.ToolTab

/**
 * 4-tab segmented control (spec §3, prototype SegButtons). Wraps the generic
 * [SifrSegmented] so it is typed to [ToolTab] and provides localized labels.
 */
@Composable
fun ToolTabBar(
    selected: ToolTab,
    onSelect: (ToolTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SifrSegmented(
        options = ToolTab.entries,
        selected = selected,
        label = { tab ->
            when (tab) {
                ToolTab.Units -> stringResource(R.string.tools_tab_units)
                ToolTab.Currency -> stringResource(R.string.tools_tab_currency)
                ToolTab.Tip -> stringResource(R.string.tools_tab_tip)
                ToolTab.Date -> stringResource(R.string.tools_tab_date)
            }
        },
        onSelect = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    )
}
```

- [ ] **Step 2: `ToolField.kt`**

The field shows the raw string the numpad typed. When focused: accent border + blinking caret. Right-aligned. Mirror in RTL (the CompositionLocal `LocalLayoutDirection` already does this naturally with `TextAlign.End`).

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Tappable display-font value box. When [focused], renders an accent border and a
 * blinking cursor after the value. Right-aligned; mirrors naturally in RTL layouts.
 */
@Composable
fun ToolField(
    value: String,
    focused: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (focused) sifr.accent else sifr.hairline

    // Blinking caret. rememberInfiniteTransition must be called unconditionally
    // (Compose forbids conditional remember*); gate the result to 0 when unfocused.
    val transition = rememberInfiniteTransition(label = "caret")
    val rawAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    val caretAlpha = if (focused) rawAlpha else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(sifr.background)
            .clickable(role = Role.Button) { onFocus() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val displayText = value.ifEmpty { placeholder }
        Text(
            text = displayText,
            color = if (value.isEmpty()) sifr.dim else sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = FontWeight.W300,
            fontSize = 24.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            style = TextStyle(textAlign = TextAlign.End),
        )
        if (focused) {
            Box(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .width(2.dp)
                    .background(sifr.accent)
                    .alpha(caretAlpha),
            ) {
                Text(text = "|", fontSize = 24.sp, color = sifr.accent) // sizing proxy
            }
        }
    }
}
```

- [ ] **Step 3: `ToolOut.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Labelled output. [big] renders the value in accent color at 28sp (the headline result).
 * Label is always dim, 11sp. Right-aligned to match [ToolField].
 */
@Composable
fun ToolOut(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    val sifr = SifrTokens.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            color = sifr.dim,
            fontFamily = sifr.uiFamily,
            fontSize = 11.sp,
        )
        Text(
            text = value.ifEmpty { "—" },
            color = if (big) sifr.accent else sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = if (big) FontWeight.W400 else FontWeight.W300,
            fontSize = if (big) 28.sp else 20.sp,
        )
    }
}
```

- [ ] **Step 4: `ToolSelect.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Dropdown selector for units and currencies. Shows the [selected] code; tapping
 * expands an [options] menu. Uses Material 3 [ExposedDropdownMenuBox].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSelect(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val sifr = SifrTokens.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it, fontSize = 11.sp, color = sifr.dim) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = sifr.uiFamily,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = sifr.text,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTrailingIconColor = sifr.dim,
                unfocusedTrailingIconColor = sifr.dim,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .border(1.dp, sifr.hairline, shape),
            shape = shape,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = sifr.surface,
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontFamily = sifr.uiFamily,
                            fontSize = 13.sp,
                            color = if (opt == selected) sifr.accent else sifr.text,
                        )
                    },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}
```

- [ ] **Step 5: `Stepper.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * `−  value  +` pill. Used for the Tip split count. [min] defaults to 1 (split ≥ 1 person).
 * The − and + are [IconButton]s so the touch target is 48×48dp even at small sizes.
 */
@Composable
fun Stepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .border(1.dp, sifr.hairline, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrement, enabled = value > min) {
            Text("−", color = if (value > min) sifr.text else sifr.dim, fontSize = 18.sp)
        }
        Text(
            text = value.toString(),
            color = sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = FontWeight.W400,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 36.dp)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onIncrement) {
            Text("+", color = sifr.text, fontSize = 18.sp)
        }
    }
}
```

- [ ] **Step 6: `ToolNumPad.kt`**

3 columns × 4 rows: `7 8 9 / 4 5 6 / 1 2 3 / 0 . ⌫`. Reuses `CalculatorButton` with `SifrKeyRole.Num` for palette-consistent key styling, press animation, haptics, and `Role.Button` semantics. Landscape: compact height (40dp) and tighter gap (6dp).

```kotlin
package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorButton

private val PAD_KEYS = listOf(
    '7', '8', '9',
    '4', '5', '6',
    '1', '2', '3',
    '0', '.', '⌫',
)

/**
 * 3-column numpad for Tools. Reuses [CalculatorButton] so key styling, animation,
 * and a11y are consistent with the calculator. [compact] is true in landscape (shorter rows).
 */
@Composable
fun ToolNumPad(
    onNumKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val rowHeight = if (compact) 40.dp else 56.dp
    val gap = if (compact) 6.dp else 8.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalArrangement = Arrangement.spacedBy(gap),
        userScrollEnabled = false,
    ) {
        items(PAD_KEYS) { key ->
            val isBackspace = key == '⌫'
            CalculatorButton(
                action = CalculatorUiAction(
                    text = if (isBackspace) "⌫" else key.toString(),
                    role = SifrKeyRole.Fn.takeIf { isBackspace } ?: SifrKeyRole.Num,
                    action = CalculatorAction.Delete, // unused; ToolNumPad uses its own callbacks
                ),
                modifier = Modifier.height(rowHeight),
                fontSize = 20.sp,
                onClick = {
                    if (isBackspace) onBackspace() else onNumKey(key)
                },
            )
        }
    }
}
```

> Note: `CalculatorButton` requires a `CalculatorAction` in `CalculatorUiAction.action` even though `ToolNumPad` never dispatches it — supply `CalculatorAction.Delete` as a no-op placeholder. The actual key behavior is driven by the `onClick` lambda.

- [ ] **Step 7: Build all components**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/ui/components/
```

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git commit -m "feat(tools): shared UI components — ToolTabBar, ToolField, ToolOut, ToolSelect, Stepper, ToolNumPad"
```

---

## Task 8: Tab content composables (four inline helpers)

Rather than four separate files, the four tab layouts live as private `@Composable` functions in `ToolsScreen.kt` itself — matching how `CalculatorButtonGrid` keeps its rows inline. The public `ToolsScreen` composes them.

No separate files needed here. These land in Task 9 as part of the full `ToolsScreen.kt`.

---

## Task 9: `ToolsScreen.kt`, `ToolsScreenLandscape.kt`, `ToolsRoot.kt`

This is the main UI task. Replace the stub `ToolsRoot.kt` written in Task 5 with the full implementation. Create the portrait and landscape screen composables.

**Files:**
- Replace: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsRoot.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreen.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreenLandscape.kt`

- [ ] **Step 1: Implement `ToolsRoot.kt`** (replaces the Task 5 stub)

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun ToolsRoot(
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    viewModel: ToolsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ToolsEvent.NavigateBack -> onNavigateBack()
            ToolsEvent.RatesRefreshFailed -> {
                // Silent for now — the currency footnote already shows "offline" state.
                // A future iteration can show a brief Snackbar here.
            }
        }
    }
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    if (isLandscape) {
        ToolsScreenLandscape(state = state, onAction = viewModel::onAction)
    } else {
        ToolsScreen(state = state, onAction = viewModel::onAction)
    }
}
```

- [ ] **Step 2: Implement `ToolsScreen.kt`** (portrait)

The screen uses a `Scaffold` (M3, transparent background — same as Settings/History) with `SifrSubScreenTopBar`. Content is a `Column` with: `ToolTabBar` → tab card (scrollable) → `ToolNumPad` (except Date tab). Date tab shows two `SifrCard`s instead of a numpad.

```kotlin
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrCard
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.components.SifrRowDivider
import dev.gaddal.sifr.core.ui.components.SifrSubScreenTopBar
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import dev.gaddal.sifr.feature.tools.ui.components.Stepper
import dev.gaddal.sifr.feature.tools.ui.components.ToolField
import dev.gaddal.sifr.feature.tools.ui.components.ToolNumPad
import dev.gaddal.sifr.feature.tools.ui.components.ToolOut
import dev.gaddal.sifr.feature.tools.ui.components.ToolSelect
import dev.gaddal.sifr.feature.tools.ui.components.ToolTabBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ToolsScreen(
    state: ToolsState,
    onAction: (ToolsAction) -> Unit,
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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            ToolTabBar(
                selected = state.activeTab,
                onSelect = { onAction(ToolsAction.SelectTab(it)) },
            )
            Spacer(Modifier.height(16.dp))

            // Tool card (scrollable) + optional numpad
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                when (state.activeTab) {
                    ToolTab.Units -> UnitsCard(state, onAction)
                    ToolTab.Currency -> CurrencyCard(state, onAction)
                    ToolTab.Tip -> TipCard(state, onAction)
                    ToolTab.Date -> DateCards(state, onAction)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Numpad (hidden on Date tab)
            if (state.activeTab != ToolTab.Date) {
                ToolNumPad(
                    onNumKey = { onAction(ToolsAction.NumKey(it)) },
                    onBackspace = { onAction(ToolsAction.Backspace) },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

// ── Units tab ─────────────────────────────────────────────────────────────────

@Composable
private fun UnitsCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    Column(Modifier.padding(horizontal = 18.dp)) {
        // Category chips
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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

// ── Currency tab ──────────────────────────────────────────────────────────────

@Composable
private fun CurrencyCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    val snapshot = state.rates.snapshotOrNull
    val currencies = snapshot?.currencies ?: listOf("USD", "SAR", "AED", "EGP", "EUR", "GBP")

    Column(Modifier.padding(horizontal = 18.dp)) {
        SifrCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolSelect(
                    selected = state.cFrom,
                    options = currencies,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolSelect(
                    selected = state.cTo,
                    options = currencies,
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
        // FX footnote
        val noteText = when (val r = state.rates) {
            is RatesResource.Loading -> ""
            is RatesResource.Success ->
                if (r.stale)
                    stringResource(R.string.tools_fx_offline, r.snapshot.asOf.toString())
                else
                    stringResource(R.string.tools_fx_note, r.snapshot.asOf.toString())
            is RatesResource.SeedFallback -> stringResource(R.string.tools_fx_seed)
        }
        if (noteText.isNotEmpty()) {
            Text(
                text = noteText,
                color = sifr.dim,
                fontFamily = sifr.uiFamily,
                fontSize = 10.5.sp,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

// ── Tip tab ───────────────────────────────────────────────────────────────────

private val TIP_PERCENTS = listOf(10, 12, 15, 18, 20)

@Composable
private fun TipCard(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    Column(Modifier.padding(horizontal = 18.dp)) {
        SifrCard {
            // Bill row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.tools_bill),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 13.sp,
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
            // Tip % chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.tools_tip),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TIP_PERCENTS.forEach { pct ->
                        SifrChip(
                            label = "$pct%",
                            active = pct == state.tipPct,
                            onClick = { onAction(ToolsAction.SetTipPct(pct)) },
                        )
                    }
                }
            }
            SifrRowDivider()
            // Split row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.tools_split),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 13.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Stepper(
                        value = state.split,
                        onDecrement = { onAction(ToolsAction.DecrementSplit) },
                        onIncrement = { onAction(ToolsAction.IncrementSplit) },
                    )
                    Text(
                        stringResource(R.string.tools_people),
                        color = sifr.dim,
                        fontFamily = sifr.uiFamily,
                        fontSize = 13.sp,
                    )
                }
            }
            // Output row
            HorizontalDivider(color = sifr.hairline, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolOut(
                    label = stringResource(R.string.tools_out_tip),
                    value = state.tipOut,
                    modifier = Modifier.weight(1f),
                )
                ToolOut(
                    label = stringResource(R.string.tools_out_total),
                    value = state.totalOut,
                    modifier = Modifier.weight(1f),
                )
                ToolOut(
                    label = stringResource(R.string.tools_out_each),
                    value = state.eachOut,
                    big = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Date tab ──────────────────────────────────────────────────────────────────

private val DATE_DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val ADD_RESULT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@Composable
private fun DateCards(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    val sifr = SifrTokens.colors
    Column(
        Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Card 1: Date difference
        SifrCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.tools_date_diff),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                )
                // Two date pickers (read: SifrChip-style date display; M3 DatePicker dialog)
                // For now: tappable date chips that open M3 DatePickerDialog on click.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateChip(
                        date = state.date1,
                        onClick = { /* open DatePickerDialog, then dispatch SetDate1 */ },
                        modifier = Modifier.weight(1f),
                    )
                    Text("→", color = sifr.dim, fontSize = 16.sp)
                    DateChip(
                        date = state.date2,
                        onClick = { /* open DatePickerDialog, then dispatch SetDate2 */ },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ToolOut(
                        label = stringResource(R.string.tools_days),
                        value = state.diffDays.toString(),
                        big = true,
                    )
                    val (w, d) = state.diffWeeks to state.diffRemainingDays
                    ToolOut(
                        label = "${stringResource(R.string.tools_weeks)} / ${stringResource(R.string.tools_days)}",
                        value = "${w}${stringResource(R.string.tools_w)}  ${d}${stringResource(R.string.tools_d)}",
                    )
                }
            }
        }

        // Card 2: Add days
        SifrCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.tools_date_add),
                    color = sifr.dim,
                    fontFamily = sifr.uiFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateChip(
                        date = state.date1,
                        onClick = { /* open DatePickerDialog for date1 */ },
                        modifier = Modifier.weight(1f),
                    )
                    Text("+", color = sifr.dim, fontSize = 16.sp)
                    ToolField(
                        value = state.addDays,
                        focused = state.focusedField == FocusedField.AddDays,
                        onFocus = { onAction(ToolsAction.FocusField(FocusedField.AddDays)) },
                        placeholder = "0",
                        modifier = Modifier.weight(1f),
                    )
                }
                val resultText = state.addResult?.format(ADD_RESULT_FORMAT) ?: "—"
                ToolOut(
                    label = stringResource(R.string.tools_result),
                    value = resultText,
                    big = true,
                )
            }
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SifrChip(
        label = date.format(DATE_DISPLAY_FORMAT),
        onClick = onClick,
        modifier = modifier,
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun PreviewToolsUnits() = SifrTheme(palette = SifrPalette.Layl) {
    ToolsScreen(
        state = ToolsState(activeTab = ToolTab.Units, uVal = "100", uResult = "0.1"),
        onAction = {},
    )
}

@PreviewLightDark
@Composable
private fun PreviewToolsCurrencyLoading() = SifrTheme(palette = SifrPalette.Bayan) {
    ToolsScreen(
        state = ToolsState(activeTab = ToolTab.Currency, rates = RatesResource.Loading),
        onAction = {},
    )
}

@PreviewLightDark
@Composable
private fun PreviewToolsTip() = SifrTheme(palette = SifrPalette.Raqim) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Tip,
            bill = "86",
            tipPct = 15,
            split = 2,
            tipOut = "12.9",
            totalOut = "98.9",
            eachOut = "49.45",
            focusedField = FocusedField.Bill,
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
            date1 = LocalDate.of(2026, 6, 10),
            date2 = LocalDate.of(2026, 8, 1),
            diffDays = 52,
            diffWeeks = 7,
            diffRemainingDays = 3,
        ),
        onAction = {},
    )
}
```

> **Date DatePicker integration note:** The two `onClick = { /* open DatePickerDialog ... */ }` placeholders in `DateChip` should be wired to M3 `DatePickerDialog` using `rememberDatePickerState()`. The `ToolsScreen` implementation above is sufficient for initial integration; the `onClick` stubs must be replaced with a proper dialog that dispatches `ToolsAction.SetDate1` / `SetDate2`. This wiring is detailed in Task 10 (Date picker dialog).

- [ ] **Step 3: Implement `ToolsScreenLandscape.kt`**

Landscape layout: `SifrSubScreenTopBar` + `ToolTabBar` (top) → `Row` of `[ tool card (scrollable, weight 1f) | ToolNumPad (fixed 300dp, hidden on Date) ]`. On Date: both sub-cards sit in a Row side by side.

```kotlin
package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.components.SifrSubScreenTopBar
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.tools.ui.components.ToolNumPad
import dev.gaddal.sifr.feature.tools.ui.components.ToolTabBar

@Composable
fun ToolsScreenLandscape(
    state: ToolsState,
    onAction: (ToolsAction) -> Unit,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SifrSubScreenTopBar(
                    title = stringResource(R.string.tools_title),
                    onBack = { onAction(ToolsAction.BackClicked) },
                )
                Spacer(Modifier.height(8.dp))
                ToolTabBar(
                    selected = state.activeTab,
                    onSelect = { onAction(ToolsAction.SelectTab(it)) },
                )
                Spacer(Modifier.height(8.dp))
            }
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tool card — scrollable, takes remaining width
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = 8.dp),
            ) {
                when (state.activeTab) {
                    ToolTab.Units -> UnitsCardLandscape(state, onAction)
                    ToolTab.Currency -> CurrencyCardLandscape(state, onAction)
                    ToolTab.Tip -> TipCardLandscape(state, onAction)
                    ToolTab.Date -> DateCardsLandscape(state, onAction)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Numpad — fixed 300dp, hidden on Date tab
            if (state.activeTab != ToolTab.Date) {
                ToolNumPad(
                    onNumKey = { onAction(ToolsAction.NumKey(it)) },
                    onBackspace = { onAction(ToolsAction.Backspace) },
                    compact = true,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

// Landscape tab cards: reuse the portrait implementations via the same private
// composables from ToolsScreen.kt. Because they are private, duplicate them here
// OR make them internal. Simplest: make the 4 tab-card functions `internal` in
// ToolsScreen.kt so they can be called from ToolsScreenLandscape.kt.
//
// Rename in ToolsScreen.kt:
//   private fun UnitsCard(...)  → internal fun UnitsCard(...)
//   same for CurrencyCard, TipCard, DateCards
//
// Then use them directly here:

@Composable
private fun UnitsCardLandscape(state: ToolsState, onAction: (ToolsAction) -> Unit) =
    UnitsCard(state, onAction)

@Composable
private fun CurrencyCardLandscape(state: ToolsState, onAction: (ToolsAction) -> Unit) =
    CurrencyCard(state, onAction)

@Composable
private fun TipCardLandscape(state: ToolsState, onAction: (ToolsAction) -> Unit) =
    TipCard(state, onAction)

@Composable
private fun DateCardsLandscape(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    // In landscape, the two date cards sit side by side (proto: flex-row).
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
        DateDiffCard(state, onAction, modifier = Modifier.weight(1f))
        DateAddCard(state, onAction, modifier = Modifier.weight(1f))
    }
}
```

> **Implementation note on landscape date cards:** `DateCards` in `ToolsScreen.kt` currently renders both sub-cards in a single Column. To share them with landscape (where they go side by side), split `DateCards` into two sub-functions `DateDiffCard(state, onAction, modifier)` and `DateAddCard(state, onAction, modifier)`, make them `internal`, and call them from both portrait `DateCards` (stacked Column) and `DateCardsLandscape` (Row). Do this refactor before this commit.

- [ ] **Step 4: Build**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsRoot.kt \
        app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreen.kt \
        app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreenLandscape.kt
```

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any unresolved references (import the `RatesResource` sealed class from `feature.tools.domain`, not `ui`).

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(tools): portrait + landscape screens + ToolsRoot — all four tabs"
```

---

## Task 10: Date picker dialog integration

The `DateChip` `onClick` stubs in `DateCards` / `DateDiffCard` need to open a `DatePickerDialog`. Material 3 exposes `DatePicker` as a composable that can live inside a `DatePickerDialog`. The date is stored as `LocalDate`; M3 dialog works with millis.

**Files:**
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreen.kt`

- [ ] **Step 1: Replace the DateChip stubs**

In `DateDiffCard` (and reuse the same pattern in `DateAddCard` for `date1`):

```kotlin
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneOffset

// Inside DateDiffCard:
var showDate1Picker by remember { mutableStateOf(false) }
var showDate2Picker by remember { mutableStateOf(false) }

if (showDate1Picker) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date1
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = { showDate1Picker = false },
        confirmButton = {
            TextButton(onClick = {
                val ms = pickerState.selectedDateMillis
                if (ms != null) {
                    val date = Instant.ofEpochMilli(ms)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    onAction(ToolsAction.SetDate1(date))
                }
                showDate1Picker = false
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = { showDate1Picker = false }) { Text("Cancel") }
        },
    ) { DatePicker(state = pickerState) }
}

if (showDate2Picker) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date2
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = { showDate2Picker = false },
        confirmButton = {
            TextButton(onClick = {
                val ms = pickerState.selectedDateMillis
                if (ms != null) {
                    val date = Instant.ofEpochMilli(ms)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    onAction(ToolsAction.SetDate2(date))
                }
                showDate2Picker = false
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = { showDate2Picker = false }) { Text("Cancel") }
        },
    ) { DatePicker(state = pickerState) }
}

DateChip(date = state.date1, onClick = { showDate1Picker = true }, ...)
DateChip(date = state.date2, onClick = { showDate2Picker = true }, ...)
```

Apply the same `DatePickerDialog` pattern in `DateAddCard` for `date1` (the base date of the Add operation also uses `date1`).

- [ ] **Step 2: Build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(tools): wire DatePickerDialog for date diff and add-days tabs"
```

---

## Task 11: AddDays numpad support

Per the spec (open item O1), the default is system keyboard. However the prototype shows `addDays` in the same `fields` map as the numpad. The plan decision: show the numpad on the Date tab **when `AddDays` field is focused**. This gives a consistent feel without breaking the DatePicker flow (date pickers still use the dialog; only the `addDays` number field uses the numpad).

- [ ] **Step 1: Update `ToolsScreen.kt`**

In the portrait `Column`, change the numpad visibility guard from:

```kotlin
if (state.activeTab != ToolTab.Date) {
```

to:

```kotlin
if (state.activeTab != ToolTab.Date || state.focusedField == FocusedField.AddDays) {
```

Apply the same change in `ToolsScreenLandscape.kt`.

- [ ] **Step 2: Build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(tools): show numpad on Date tab when addDays field is focused"
```

---

## Task 12: Preview coverage

Every tab × light/dark is already covered by `@PreviewLightDark` in `ToolsScreen.kt` (Task 9 added 4 previews). Add the remaining coverage gaps: Arabic RTL, landscape, and currency seed/offline states.

**Files:**
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreen.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/feature/tools/ui/ToolsScreenLandscape.kt`

- [ ] **Step 1: Add Arabic RTL preview in `ToolsScreen.kt`**

```kotlin
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.gaddal.sifr.core.ui.theme.PalettePreviewProvider

@Preview(name = "Tools Units — Arabic RTL", locale = "ar")
@Composable
private fun PreviewToolsUnitsAr() = SifrTheme(palette = SifrPalette.Layl) {
    ToolsScreen(
        state = ToolsState(activeTab = ToolTab.Units, uVal = "١٠٠", uResult = "٠٫١"),
        onAction = {},
    )
}
```

- [ ] **Step 2: Add currency states preview**

```kotlin
@PreviewLightDark
@Composable
private fun PreviewToolsCurrencyOffline() = SifrTheme(palette = SifrPalette.Mizan) {
    ToolsScreen(
        state = ToolsState(
            activeTab = ToolTab.Currency,
            rates = RatesResource.Success(
                snapshot = dev.gaddal.sifr.feature.tools.domain.RatesSnapshot(
                    base = "USD",
                    rates = mapOf("USD" to 1.0, "SAR" to 3.75),
                    asOf = java.time.LocalDate.of(2026, 6, 1),
                ),
                stale = true,
            ),
            cVal = "100", cFrom = "USD", cTo = "SAR", cResult = "375",
        ),
        onAction = {},
    )
}
```

- [ ] **Step 3: Add landscape preview in `ToolsScreenLandscape.kt`**

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme

@Preview(name = "ToolsScreenLandscape — Units (Layl dark)", widthDp = 800, heightDp = 360)
@Composable
private fun PreviewToolsLandscapeUnits() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    ToolsScreenLandscape(
        state = ToolsState(activeTab = ToolTab.Units, uVal = "1", uResult = "1000"),
        onAction = {},
    )
}

@Preview(name = "ToolsScreenLandscape — Date (Bayan light)", widthDp = 800, heightDp = 360)
@Composable
private fun PreviewToolsLandscapeDate() = SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
    ToolsScreenLandscape(
        state = ToolsState(
            activeTab = ToolTab.Date,
            date1 = LocalDate.of(2026, 1, 1),
            date2 = LocalDate.of(2026, 12, 31),
            diffDays = 364,
            diffWeeks = 52,
            diffRemainingDays = 0,
        ),
        onAction = {},
    )
}
```

- [ ] **Step 4: Build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(tools): preview coverage — Arabic RTL, offline FX, landscape tabs"
```

---

## Task 13: Full green checkpoint

- [ ] **Step 1: Full unit-test run**

Run: `sh gradlew :app:testDebugUnitTest`
Expected: PASS. The domain tests from Plan 1 and the ViewModel tests from this plan should all be green.

- [ ] **Step 2: Lint**

Run: `sh gradlew :app:lintDebug`
Expected: no `MissingTranslation` errors and no new lint errors.

- [ ] **Step 3: Assemble all variants**

Run: `sh gradlew assemble`
Expected: debug and staging BUILD SUCCESSFUL. Release is blocked by missing keystore — this is expected.

- [ ] **Step 4: Confirm clean tree**

Run: `git status --short`
Expected: only `.idea/*` + `.notes/` noise. No stray uncommitted source files.

---

## Self-Review

- **Spec coverage:** §3 per-tab layouts → Tasks 7–9 implement all four cards. §4 navigation → Task 5. §5 packages → all files land in `feature/tools/ui/`. §7 MVI → Tasks 1–4. §8 shared components → Task 7. §9 strings → Task 6. §10 testing → Task 4 (VM Turbine tests) + Plan 1 domain tests. Preview coverage → Task 12.
- **Deferred by spec:** Currency "frequent group" ordering (O4) — the VM returns `snapshot.currencies` (alphabetical); a grouped list can be added to the VM later without any UI changes.
- **O1 resolved:** AddDays uses the numpad when focused (Task 11); DatePicker dialogs handle the date fields (Task 10). This matches the prototype's behavior where `addDays` is in the `fields` map.
- **O5 (haptics):** `ToolNumPad` reuses `CalculatorButton`, which already wires `FeedbackController` through `LocalFeedbackController`. No extra wiring needed; the numpad presses play haptics automatically.
- **SavedStateHandle types:** all persisted values are primitives (`Int`, `String`, `Long`) — no custom Parcelable. `LocalDate` is stored as `Long` (epochDay). All keys are unique prefixed with `tools_`.
- **Type consistency:** `ToolsState.rates` is `RatesResource` (domain type). The ViewModel's `init` block collects `repository.rates` and calls `repository.refresh()` — this is the minimum needed to serve cache/seed and then go live, per the CurrencyRepositoryImpl design from Plan 1.
