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
}
