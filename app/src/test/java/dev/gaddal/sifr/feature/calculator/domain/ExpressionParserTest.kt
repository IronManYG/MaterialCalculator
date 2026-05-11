package dev.gaddal.sifr.feature.calculator.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExpressionParserTest {

    private lateinit var parser: ExpressionParser

    @Test
    fun `Simple expression is properly parsed`() {
        // 1. GIVEN
        parser = ExpressionParser("3+5-3x4/3")

        // 2. DO SOMETHING WITH WHAT'S GIVEN
        val actual = parser.parse()

        // 3. ASSERT EXPECTED == ACTUAL
        val expected = listOf(
            ExpressionPart.Number(3.0),
            ExpressionPart.Op(Operation.ADD),
            ExpressionPart.Number(5.0),
            ExpressionPart.Op(Operation.SUBTRACT),
            ExpressionPart.Number(3.0),
            ExpressionPart.Op(Operation.MULTIPLY),
            ExpressionPart.Number(4.0),
            ExpressionPart.Op(Operation.DIVIDE),
            ExpressionPart.Number(3.0),
        )

        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `Scientific notation is parsed as a single number`() {
        // 1E-10 must be a single number token, not "1 - 10". Without this
        // formatResult outputs containing `E` round-trip to the wrong value
        // when re-evaluated (pressing `=` again on the displayed result).
        parser = ExpressionParser("1E-10")

        val actual = parser.parse()

        assertThat(actual).isEqualTo(listOf(ExpressionPart.Number(1e-10)))
    }

    @Test
    fun `Scientific notation with positive exponent is parsed as a single number`() {
        parser = ExpressionParser("9.99999998E17")

        val actual = parser.parse()

        assertThat(actual).isEqualTo(listOf(ExpressionPart.Number(9.99999998e17)))
    }

    @Test
    fun `Scientific notation inside a larger expression is parsed correctly`() {
        // 1E-10 + 5 — the operator after the exponent digits must still register.
        parser = ExpressionParser("1E-10+5")

        val actual = parser.parse()

        assertThat(actual).isEqualTo(
            listOf(
                ExpressionPart.Number(1e-10),
                ExpressionPart.Op(Operation.ADD),
                ExpressionPart.Number(5.0),
            )
        )
    }

    @Test
    fun `Dangling exponent marker is skipped`() {
        // 1E with no digits afterwards is partial input (e.g. mid-deletion); the
        // 'E' is not consumed as part of the number, so the parser just yields 1.
        parser = ExpressionParser("1E")

        val actual = parser.parse()

        assertThat(actual).isEqualTo(listOf(ExpressionPart.Number(1.0)))
    }

    @Test
    fun`Expression with parentheses is properly parsed`() {
        // 1. GIVEN
        parser = ExpressionParser("4-(4x5)")

        // 2. DO SOMETHING WITH WHAT'S GIVEN
        val actual = parser.parse()

        // 3. ASSERT EXPECTED == ACTUAL
        val expected = listOf(
            ExpressionPart.Number(4.0),
            ExpressionPart.Op(Operation.SUBTRACT),
            ExpressionPart.Parentheses(ParenthesesType.Opening),
            ExpressionPart.Number(4.0),
            ExpressionPart.Op(Operation.MULTIPLY),
            ExpressionPart.Number(5.0),
            ExpressionPart.Parentheses(ParenthesesType.Closing),
        )

        assertThat(expected).isEqualTo(actual)
    }
}