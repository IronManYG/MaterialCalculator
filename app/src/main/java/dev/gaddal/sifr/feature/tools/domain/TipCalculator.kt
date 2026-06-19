package dev.gaddal.sifr.feature.tools.domain

/** Pure tip arithmetic (prototype: tip = bill*pct/100, total = bill+tip, each = total/split). */
object TipCalculator {

    data class TipResult(val tip: Double, val total: Double, val each: Double)

    fun compute(bill: Double, tipPercent: Int, split: Int): TipResult {
        val safeSplit = split.coerceAtLeast(1)
        val tip = bill * tipPercent / 100.0
        val total = bill + tip
        return TipResult(tip = tip, total = total, each = total / safeSplit)
    }
}
