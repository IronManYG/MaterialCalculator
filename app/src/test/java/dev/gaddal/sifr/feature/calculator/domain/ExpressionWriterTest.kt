package dev.gaddal.sifr.feature.calculator.domain

import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.domain.util.Result
import org.junit.Before
import org.junit.Test

class ExpressionWriterTest {

    private lateinit var writer: ExpressionWriter

    @Before
    fun setUp() {
        writer = ExpressionWriter()
    }

    @Test
    fun `Initial parentheses parsed`() {
        writer.processAction(CalculatorAction.Parentheses)
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.Parentheses)

        assertThat(writer.expression).isEqualTo("(5+4)")
    }

    @Test
    fun `Closing parentheses at the start not parsed`() {
        writer.processAction(CalculatorAction.Parentheses)
        writer.processAction(CalculatorAction.Parentheses)

        assertThat(writer.expression).isEqualTo("((")
    }

    @Test
    fun `Parentheses around a number are parsed`() {
        writer.processAction(CalculatorAction.Parentheses)
        writer.processAction(CalculatorAction.Number(6))
        writer.processAction(CalculatorAction.Parentheses)

        assertThat(writer.expression).isEqualTo("(6)")
    }

    @Test
    fun `Cannot start with multiply`() {
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))

        assertThat(writer.expression).isEqualTo("")
    }

    @Test
    fun `Integer result drops decimal`() {
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("5")
    }

    @Test
    fun `Divide by zero returns DIVISION_BY_ZERO error`() {
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        val result = writer.processAction(CalculatorAction.Calculate)

        assertThat(result).isEqualTo(Result.Error(CalcError.DIVISION_BY_ZERO))
    }

    @Test
    fun `Error clears on next non-Calculate input`() {
        // Trigger error
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        val errorResult = writer.processAction(CalculatorAction.Calculate)
        assertThat(errorResult).isInstanceOf(Result.Error::class.java)

        // Pressing a digit should start a fresh expression
        val nextResult = writer.processAction(CalculatorAction.Number(7))

        assertThat(nextResult).isEqualTo(Result.Success(Unit))
        assertThat(writer.expression).isEqualTo("7")
    }

    @Test
    fun `Calculate after error stays in error state`() {
        // Trigger error
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Calculate)

        // Pressing = again should yield another error, not crash
        val again = writer.processAction(CalculatorAction.Calculate)

        assertThat(again).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun `tryEvaluate returns formatted result for valid expression`() {
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(3))

        assertThat(writer.tryEvaluate()).isEqualTo(Result.Success("5"))
    }

    @Test
    fun `tryEvaluate returns DIVISION_BY_ZERO without mutating state`() {
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))

        val preview = writer.tryEvaluate()

        assertThat(preview).isEqualTo(Result.Error(CalcError.DIVISION_BY_ZERO))
        // tryEvaluate must NOT trip the writer into error state
        assertThat(writer.expression).isEqualTo("5/0")
        // And subsequent Calculate must still produce the genuine error
        val real = writer.processAction(CalculatorAction.Calculate)
        assertThat(real).isEqualTo(Result.Error(CalcError.DIVISION_BY_ZERO))
    }

    @Test
    fun `tryEvaluate on empty expression returns Success of zero`() {
        // prepareForCalculation maps empty to "0", so the preview pipeline
        // succeeds with a zero result. The VM gates this externally so the
        // user does not see a spurious "= 0" on a blank screen.
        assertThat(writer.tryEvaluate()).isEqualTo(Result.Success("0"))
    }

    @Test
    fun `tryEvaluate does not mutate expression for a complex case`() {
        writer.processAction(CalculatorAction.Parentheses) // (
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Parentheses) // )
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))
        writer.processAction(CalculatorAction.Number(4))
        val before = writer.expression

        val preview = writer.tryEvaluate()

        assertThat(preview).isEqualTo(Result.Success("20"))
        assertThat(writer.expression).isEqualTo(before)
    }

    // -------- cursor-aware behavior (Phase 2.9) --------

    @Test
    fun `Cursor tracks tail-append default`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))

        assertThat(writer.expression).isEqualTo("123")
        assertThat(writer.cursor).isEqualTo(3)
    }

    @Test
    fun `Number inserted at mid-cursor splits the expression`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(3))
        // Move cursor between 1 and 3
        writer.processAction(CalculatorAction.CursorChanged(1))

        writer.processAction(CalculatorAction.Number(2))

        assertThat(writer.expression).isEqualTo("123")
        assertThat(writer.cursor).isEqualTo(2)
    }

    @Test
    fun `Delete at cursor removes char before cursor and moves it back`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.CursorChanged(2)) // between 2 and 3

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("13")
        assertThat(writer.cursor).isEqualTo(1)
    }

    @Test
    fun `Delete at cursor=0 is a no-op`() {
        writer.processAction(CalculatorAction.Number(7))
        writer.processAction(CalculatorAction.CursorChanged(0))

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("7")
        assertThat(writer.cursor).isEqualTo(0)
    }

    @Test
    fun `Cursor clamps to expression bounds`() {
        writer.processAction(CalculatorAction.Number(5))

        writer.processAction(CalculatorAction.CursorChanged(99))
        assertThat(writer.cursor).isEqualTo(1)

        writer.processAction(CalculatorAction.CursorChanged(-3))
        assertThat(writer.cursor).isEqualTo(0)
    }

    @Test
    fun `Decimal rejected when current number already has a decimal point`() {
        // 1.5+2 — cursor between 1 and . should reject another decimal
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Decimal)
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.CursorChanged(1)) // between 1 and .

        writer.processAction(CalculatorAction.Decimal)

        assertThat(writer.expression).isEqualTo("1.5+2")
    }

    @Test
    fun `Decimal allowed in a separate number group`() {
        // 12+34 — cursor between 3 and 4 should accept a decimal in the second group
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.CursorChanged(4)) // between 3 and 4

        writer.processAction(CalculatorAction.Decimal)

        assertThat(writer.expression).isEqualTo("12+3.4")
        assertThat(writer.cursor).isEqualTo(5)
    }

    @Test
    fun `Operator rejected when char-before-cursor is an operator`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.CursorChanged(2)) // between + and 2

        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))

        // Multiply needs a number/`)` before it; the prevChar is `+` so the action is rejected
        assertThat(writer.expression).isEqualTo("1+2")
    }

    @Test
    fun `Calculate moves cursor to end of result`() {
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.CursorChanged(1)) // mid-expression cursor

        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("5")
        assertThat(writer.cursor).isEqualTo(1)
    }

    // -------- range-aware selection (Phase 2.9 polish, gated by setting) --------

    @Test
    fun `SelectionChanged sets selectionStart and cursor independently`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(4))

        writer.processAction(CalculatorAction.SelectionChanged(start = 1, end = 3))

        assertThat(writer.selectionStart).isEqualTo(1)
        assertThat(writer.cursor).isEqualTo(3)
    }

    @Test
    fun `Number action replaces selected range and collapses cursor`() {
        // "1234" with range "23" selected — typing 9 should yield "194"
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.SelectionChanged(start = 1, end = 3))

        writer.processAction(CalculatorAction.Number(9))

        assertThat(writer.expression).isEqualTo("194")
        assertThat(writer.cursor).isEqualTo(2)
        assertThat(writer.selectionStart).isEqualTo(2)
    }

    @Test
    fun `Delete with a range removes the range without backspacing further`() {
        // "1234" with range "23" selected — Delete removes "23", cursor lands at 1
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.SelectionChanged(start = 1, end = 3))

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("14")
        assertThat(writer.cursor).isEqualTo(1)
        assertThat(writer.selectionStart).isEqualTo(1)
    }

    @Test
    fun `CursorChanged collapses the selection`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.SelectionChanged(start = 0, end = 2))

        writer.processAction(CalculatorAction.CursorChanged(1))

        assertThat(writer.cursor).isEqualTo(1)
        assertThat(writer.selectionStart).isEqualTo(1)
    }

    @Test
    fun `Reversed selection (start greater than end) still replaces the range`() {
        // Compose's TextRange can have selection.start > selection.end when
        // the user drags right-to-left. The writer treats min/max symmetrically.
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.SelectionChanged(start = 3, end = 1))

        writer.processAction(CalculatorAction.Number(9))

        assertThat(writer.expression).isEqualTo("194")
        assertThat(writer.cursor).isEqualTo(2)
    }

    @Test
    fun `Validity checks honor the post-replacement context`() {
        // "1.5" with range "1.5" fully selected — pressing Decimal should be
        // rejected (prevChar would be null after replacement, no number for it).
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Decimal)
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.SelectionChanged(start = 0, end = 3))

        writer.processAction(CalculatorAction.Decimal)

        // Decimal rejected; range stays selected and unchanged.
        assertThat(writer.expression).isEqualTo("1.5")
        assertThat(writer.selectionStart).isEqualTo(0)
        assertThat(writer.cursor).isEqualTo(3)
    }

    // -------- result formatting (Phase 2.9 polish) --------

    @Test
    fun `One divided by three keeps 16 significant digits`() {
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Calculate)

        // 16 threes after the decimal point — IEEE 754 double's honest precision
        assertThat(writer.expression).isEqualTo("0.3333333333333333")
    }

    @Test
    fun `Result at or above 1e16 switches to scientific notation`() {
        // 999999999 * 999999999 = 999999998000000001 ≈ 9.99999998e17 — well past
        // the 1e16 fixed-point threshold, exercising the scientific-notation path.
        repeat(9) { writer.processAction(CalculatorAction.Number(9)) }
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))
        repeat(9) { writer.processAction(CalculatorAction.Number(9)) }
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).startsWith("9.99999998")
        assertThat(writer.expression).contains("E17")
    }

    @Test
    fun `Result below 1e-9 switches to scientific notation`() {
        // 1 / 10000000000 = 1e-10 — just under the 1e-9 lower threshold
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(1))
        repeat(10) { writer.processAction(CalculatorAction.Number(0)) }
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("1E-10")
    }

    @Test
    fun `Large but sub-threshold result stays in fixed notation`() {
        // 9999999999 (10 nines = 9.999999999e9) is well below 1e16, so it stays
        // in fixed-point and round-trips exactly.
        repeat(10) { writer.processAction(CalculatorAction.Number(9)) }
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("9999999999")
    }

    @Test
    fun `Scientific result is idempotent under repeated Calculate`() {
        // Regression: before the parser learned scientific notation, pressing `=`
        // on a result like `1E-10` re-parsed it as `1 - 10 = -9` (the `E` was
        // silently skipped and `-` was treated as subtraction). Now `=` is a
        // no-op on a scientific-form result, matching every other single-number
        // expression.
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(1))
        repeat(10) { writer.processAction(CalculatorAction.Number(0)) }
        writer.processAction(CalculatorAction.Calculate) // -> "1E-10"

        writer.processAction(CalculatorAction.Calculate) // pressing = again

        assertThat(writer.expression).isEqualTo("1E-10")
    }

    @Test
    fun `Scientific result can be operated on as a number`() {
        // After "1E-10" lands as the expression, typing "+ 5 =" should evaluate
        // 1e-10 + 5, not parse the result as "1 - 10 + 5".
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(1))
        repeat(10) { writer.processAction(CalculatorAction.Number(0)) }
        writer.processAction(CalculatorAction.Calculate) // -> "1E-10"
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Calculate)

        // 5 + 1e-10 = 5.0000000001 (Double can represent this exactly)
        assertThat(writer.expression).isEqualTo("5.0000000001")
    }

    @Test
    fun `Small floating-point noise is hidden by the sig-digit cap`() {
        // 0.1 + 0.2 = 0.30000000000000004 in Double; the trailing 4 is rounding
        // noise past the 16-digit budget and should be hidden.
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Decimal)
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Decimal)
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("0.3")
    }

    @Test
    fun `Error-recovery typing resets expression and cursor`() {
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Calculate) // -> error
        writer.processAction(CalculatorAction.CursorChanged(3))

        writer.processAction(CalculatorAction.Number(7))

        assertThat(writer.expression).isEqualTo("7")
        assertThat(writer.cursor).isEqualTo(1)
    }

    // ---- Smart Delete: only the funcname-and-open-paren token chunks ----

    @Test
    fun `Delete after function open-paren removes the whole funcname-paren token`() {
        writer.processAction(CalculatorAction.Function("sin"))
        // Inserts "sin(" with cursor at 4.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("")
        assertThat(writer.cursor).isEqualTo(0)
    }

    @Test
    fun `Delete after function open-paren respects multiply operator before name`() {
        // 5xsin( — "x" is the multiply glyph; the suffix-match must pick
        // "sin" (3 chars) out of the letter run "xsin" so the 'x' is kept.
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))
        writer.processAction(CalculatorAction.Function("sin"))
        // Expression: "5xsin(", cursor at 6.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("5x")
        assertThat(writer.cursor).isEqualTo(2)
    }

    @Test
    fun `Delete after exp open-paren removes the full token (x inside name is not multiply)`() {
        // The 'x' in "exp" sits between letters, so it's part of the name,
        // not a multiply operator. Suffix match must pick "exp" not "p".
        writer.processAction(CalculatorAction.Function("exp"))
        // Expression: "exp(", cursor at 4.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("")
        assertThat(writer.cursor).isEqualTo(0)
    }

    @Test
    fun `Delete after exp open-paren preceded by multiply preserves the multiplier`() {
        // 5xexp( — letter run is "xexp", suffix "exp" matches, leading 'x'
        // must remain as the multiply operator.
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))
        writer.processAction(CalculatorAction.Function("exp"))
        // Expression: "5xexp(", cursor at 6.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("5x")
        assertThat(writer.cursor).isEqualTo(2)
    }

    @Test
    fun `Delete recognises every supported function name`() {
        val names = listOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "exp")
        for (name in names) {
            writer.processAction(CalculatorAction.Clear)
            writer.processAction(CalculatorAction.Function(name))

            writer.processAction(CalculatorAction.Delete)

            assertThat(writer.expression).isEqualTo("")
            assertThat(writer.cursor).isEqualTo(0)
        }
    }

    @Test
    fun `Delete after unknown identifier with open paren falls back to one-char`() {
        // "abc(" — "abc" is not a known function, so smart-delete declines.
        // We can't type this through the buttons, but the writer should still
        // behave deterministically if anyone feeds it via RestoreExpression.
        writer.processAction(CalculatorAction.RestoreExpression("abc("))

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("abc")
        assertThat(writer.cursor).isEqualTo(3)
    }

    @Test
    fun `Delete after closing paren of function call removes only the close paren`() {
        writer.processAction(CalculatorAction.Function("cos"))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Parentheses) // closes with ")"
        // Expression: "cos(30)", cursor at 7.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("cos(30")
        assertThat(writer.cursor).isEqualTo(6)
    }

    @Test
    fun `Delete inside function args removes just one character`() {
        writer.processAction(CalculatorAction.Function("sin"))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Number(0))
        // Expression: "sin(30", cursor at 6.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("sin(3")
        assertThat(writer.cursor).isEqualTo(5)
    }

    @Test
    fun `Delete after plain open paren without function removes just one character`() {
        writer.processAction(CalculatorAction.Parentheses) // "("
        // Expression: "(", cursor at 1.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("")
        assertThat(writer.cursor).isEqualTo(0)
    }

    @Test
    fun `Delete after plain closing paren without function removes just one character`() {
        writer.processAction(CalculatorAction.Parentheses) // "("
        writer.processAction(CalculatorAction.Number(2))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(3))
        writer.processAction(CalculatorAction.Parentheses) // ")"
        // Expression: "(2+3)", cursor at 5.

        writer.processAction(CalculatorAction.Delete)

        assertThat(writer.expression).isEqualTo("(2+3")
        assertThat(writer.cursor).isEqualTo(4)
    }
}
