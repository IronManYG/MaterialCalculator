package dev.gaddal.sifr.feature.calculator.domain

class ExpressionParser(
    private val calculation: String
) {

    fun parse(): List<ExpressionPart> {
        val result = mutableListOf<ExpressionPart>()

        var i = 0
        while(i < calculation.length) {
            val curChar = calculation[i]
            when {
                curChar in operationSymbols -> {
                    result.add(
                        ExpressionPart.Op(operationFromSymbol(curChar))
                    )
                }
                curChar.isDigit() -> {
                    i = parseNumber(i, result)
                    continue
                }
                curChar in "()" -> {
                    parseParentheses(curChar, result)
                }
            }
            i++
        }
        return result
    }

    private fun parseNumber(startingIndex: Int, result: MutableList<ExpressionPart>): Int {
        var i = startingIndex
        val numberAsString = buildString {
            while (i < calculation.length && calculation[i] in "0123456789.") {
                append(calculation[i])
                i++
            }
            // Optional scientific-notation suffix: E[+-]?digits. The marker is
            // only consumed when followed by at least one exponent digit (after
            // an optional sign) — a dangling `E` is left for the outer loop to
            // skip, mirroring how the parser treats other unknown characters.
            // Lets `formatResult`-produced strings like "1E-10" round-trip when
            // pressed `=` again or used as operands in further expressions.
            if (i < calculation.length && (calculation[i] == 'E' || calculation[i] == 'e')) {
                var j = i + 1
                if (j < calculation.length && (calculation[j] == '+' || calculation[j] == '-')) {
                    j++
                }
                val digitStart = j
                while (j < calculation.length && calculation[j].isDigit()) {
                    j++
                }
                if (j > digitStart) {
                    append(calculation, i, j)
                    i = j
                }
            }
        }
        result.add(ExpressionPart.Number(numberAsString.toDouble()))
        return i
    }

    private fun parseParentheses(curChar: Char, result: MutableList<ExpressionPart>) {
        result.add(
            ExpressionPart.Parentheses(
                type = when(curChar) {
                    '(' -> ParenthesesType.Opening
                    ')' -> ParenthesesType.Closing
                    else -> throw IllegalArgumentException("Invalid parentheses type")
                }
            )
        )
    }
}