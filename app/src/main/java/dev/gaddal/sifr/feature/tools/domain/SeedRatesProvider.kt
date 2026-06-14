package dev.gaddal.sifr.feature.tools.domain

/** Supplies the bundled fallback snapshot (read from assets in production; fakeable in tests). */
fun interface SeedRatesProvider {
    fun seed(): RatesSnapshot
}
