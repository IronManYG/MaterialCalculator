package dev.gaddal.sifr.feature.tools.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Pure date arithmetic over java.time (desugared on minSdk 24). */
object DateCalculator {

    /** Signed day count from [start] to [end] (negative if [end] precedes [start]). */
    fun daysBetween(start: LocalDate, end: LocalDate): Long =
        ChronoUnit.DAYS.between(start, end)

    fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)

    /** Splits an absolute day count into whole weeks + remainder days (both non-negative). */
    fun weeksAndDays(totalDays: Long): Pair<Int, Int> {
        val abs = abs(totalDays)
        return (abs / 7).toInt() to (abs % 7).toInt()
    }
}
