package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.runtime.Immutable
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import java.time.LocalDate

/**
 * Which input field owns the numpad. `null` = no numpad shown — the Date tab's default,
 * since its two dates are chosen via a DatePicker dialog. Focusing [AddDays] is the one
 * Date-tab case that brings the numpad up.
 */
enum class FocusedField { UVal, CVal, Bill, AddDays }

/**
 * Which calendar the Date tab renders. Day counts (diff, add) are calendar-independent —
 * only the rendered chip/result dates change. The DatePicker dialog stays Gregorian for input.
 */
enum class CalendarSystem { Gregorian, Hijri }

/**
 * Single source of truth for the Tools screen. All derived output is pre-formatted here
 * (in the ViewModel) so every UI composable is a pure read.
 *
 * [uResult], [cResult], [tipOut], etc. are "" when input is blank or conversion
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
    val calendar: CalendarSystem = CalendarSystem.Gregorian,

    // ── Numpad focus ──────────────────────────────────────────────────────────
    val focusedField: FocusedField? = FocusedField.UVal,
)
