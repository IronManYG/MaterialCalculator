package dev.gaddal.sifr.feature.tools.domain

/**
 * Pure unit conversion, mirroring the prototype `sifrConvert`. Linear categories
 * convert through their SI-relative factor; Temp is an affine special case.
 */
object UnitConverter {

    fun convert(category: UnitCategory, value: Double, from: String, to: String): Double {
        if (category == UnitCategory.Temp) return convertTemp(value, from, to)
        val factors = category.factors
        val fromFactor = factors[from] ?: return Double.NaN
        val toFactor = factors[to] ?: return Double.NaN
        return value * fromFactor / toFactor
    }

    private fun convertTemp(value: Double, from: String, to: String): Double {
        // Normalise to Celsius first, then out.
        val celsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            else -> value - 273.15 // K
        }
        return when (to) {
            "°C" -> celsius
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            else -> celsius + 273.15 // K
        }
    }
}
