package dev.gaddal.sifr.feature.tools.domain

/**
 * Unit-conversion categories and their SI-relative factor tables, verbatim from
 * the prototype `tools.jsx → SIFR_UNITS`. [units] preserves declaration order
 * (the picker shows them in this order; category-change resets to the first two).
 * Temp carries no factors here — it is special-cased in [UnitConverter].
 */
enum class UnitCategory(val factors: Map<String, Double>) {
    Length(
        linkedMapOf(
            "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
            "mi" to 1609.344, "ft" to 0.3048, "in" to 0.0254, "yd" to 0.9144,
        ),
    ),
    Weight(
        linkedMapOf(
            "kg" to 1.0, "g" to 0.001, "mg" to 0.000001,
            "lb" to 0.45359237, "oz" to 0.0283495, "t" to 1000.0,
        ),
    ),
    Temp(linkedMapOf("°C" to 1.0, "°F" to 1.0, "K" to 1.0)),
    Data(
        linkedMapOf(
            "B" to 1.0, "KB" to 1e3, "MB" to 1e6, "GB" to 1e9, "TB" to 1e12,
            "KiB" to 1024.0, "MiB" to 1048576.0, "GiB" to 1073741824.0,
        ),
    ),
    ;

    val units: List<String> get() = factors.keys.toList()
}
