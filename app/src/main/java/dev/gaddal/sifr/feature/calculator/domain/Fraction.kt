package dev.gaddal.sifr.feature.calculator.domain

import kotlin.math.abs
import kotlin.math.floor

/**
 * Exact-fraction view of a result, e.g. 0.75 -> 3/4, 2.5 -> 2 1/2.
 * [sign] is +1 or -1; [whole] is the non-negative integer part; [n]/[d] is the
 * proper fractional remainder (n in 1..d-1, d in 2..9999).
 */
data class Fraction(val sign: Int, val whole: Long, val n: Long, val d: Long)

/**
 * Continued-fraction approximation of the fractional part. Mirrors the design
 * handoff's `toFraction` (docs/design_handoff_sifr_redesign/reference_prototype/app/engine.js):
 * 24 iterations, denominator capped at 9999, convergence tolerance 1e-9. Returns null for
 * integers, non-finite values, or values with no representable fraction.
 */
fun Double.toFraction(): Fraction? {
    if (!this.isFinite()) return null
    val sign = if (this < 0) -1 else 1
    val x = abs(this)
    val whole = floor(x).toLong()
    val frac = x - whole
    if (frac < 1e-12) return null // fractional part is effectively zero -> treat as integer

    // numerator/denominator of the last two continued-fraction convergents
    var h1 = 1L; var h0 = 0L
    var k1 = 0L; var k0 = 1L
    var b = frac
    for (i in 0 until 24) {
        val a = floor(b).toLong()
        val h2 = a * h1 + h0
        val k2 = a * k1 + k0
        if (k2 > 9999) break
        h0 = h1; h1 = h2; k0 = k1; k1 = k2
        if (abs(frac - h1.toDouble() / k1.toDouble()) < 1e-11) break // converged to ~11 digits -> stop refining
        val r = b - a
        if (r < 1e-12) break // remainder vanished -> exact rational found
        b = 1.0 / r
    }
    if (k1 <= 1) return null
    if (abs(frac - h1.toDouble() / k1.toDouble()) > 1e-9) return null // best approximation still too coarse -> not a clean fraction
    return Fraction(sign = sign, whole = whole, n = h1, d = k1)
}
