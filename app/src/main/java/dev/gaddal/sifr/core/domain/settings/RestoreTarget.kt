package dev.gaddal.sifr.core.domain.settings

/**
 * What tapping a past calculation — on the History page or a Tape receipt row —
 * inserts back into the calculator. PascalCase to match [SifrPalette] /
 * [KeypadLayout] / ThemeMode.
 *
 * - [Result] (default): insert the result only, e.g. `17` — matches what shipped
 *   in v1.7 before this setting existed.
 * - [Expression]: insert the original expression, e.g. `12+5`, as the editable
 *   line; the live preview then re-shows `= 17`.
 *
 * DEFERRED third option (backlog, 2026-06-14): a `FrozenState` mode that restores
 * the full post-`=` headline view (input on the big line, `= result` below, with
 * COPY · SHARE · ANS). That needs a richer restore payload carrying BOTH strings
 * plus the `justEvaluated` flag — the [dev.gaddal.sifr.core.data.calculator.CalculatorInputBus]
 * carries only a single String today — so it is intentionally out of scope here.
 */
enum class RestoreTarget { Result, Expression }
