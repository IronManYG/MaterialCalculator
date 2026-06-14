package dev.gaddal.sifr.core.ui.util

/**
 * Swaps the raw division operator `/` (the underlying char in the expression
 * string, per the CLAUDE.md operator convention) for the `÷` glyph the divide
 * key actually shows — so the displayed input, tape rows, and history rows read
 * with the same symbol the user pressed.
 *
 * Display-only and length-preserving (a 1:1 char swap), so any cursor/selection
 * offsets that index the raw expression still map identically over the displayed
 * text — safe to apply when building an editable field's [androidx.compose.ui.text.input.TextFieldValue].
 *
 * Multiply already shows `x` in both the key and the expression string, so only
 * divide is mismatched. The parser/writer/tests key off the raw `/`, and
 * COPY/SHARE intentionally copy the raw form, so do NOT run this on text headed
 * back into the writer or onto the clipboard — it is purely for rendering.
 */
fun String.toDisplayExpression(): String = replace('/', '÷')
