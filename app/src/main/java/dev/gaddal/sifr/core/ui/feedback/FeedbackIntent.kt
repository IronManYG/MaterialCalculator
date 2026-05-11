package dev.gaddal.sifr.core.ui.feedback

/**
 * Semantic feedback intents. Each maps to a different Pulsar preset (and
 * optionally a sound) inside [FeedbackController]. Use these instead of a
 * generic click() to keep feedback signal-bearing, not noise.
 *
 * - [Error]: top-priority signal (÷0, invalid expression). Strong haptic + tone.
 * - [CalculateSuccess]: subtle confirmation when `=` produces a result.
 * - [Destructive]: state-erasing actions (AC, history delete, clear-all).
 * - [Selection]: light tap-confirmation (history restore, settings toggle demo).
 */
enum class FeedbackIntent { Error, CalculateSuccess, Destructive, Selection }
