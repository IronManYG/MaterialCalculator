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
}
