package dev.gaddal.sifr.feature.calculator.domain

class ExpressionParser(
    private val calculation: String
) {

    fun parse(): List<ExpressionPart> {
        val result = mutableListOf<ExpressionPart>()

        var i = 0
        while (i < calculation.length) {
            val curChar = calculation[i]
            when {
                curChar.isDigit() -> {
                    i = parseNumber(i, result)
                    continue
                }
                curChar in "()" -> {
                    parseParentheses(curChar, result)
                }
                curChar == ConstantSymbol.PI.symbol -> {
                    result.add(ExpressionPart.Constant(ConstantSymbol.PI))
                }
                curChar == PostfixOp.FACTORIAL.symbol -> {
                    result.add(ExpressionPart.Postfix(PostfixOp.FACTORIAL))
                }
                // Operation symbols must be checked before isLetter() because
                // MULTIPLY uses 'x', which isLetter() would otherwise consume.
                curChar in operationSymbols -> {
                    result.add(ExpressionPart.Op(operationFromSymbol(curChar)))
                }
                curChar.isLetter() -> {
                    i = parseIdentifier(i, result)
                    continue
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
            // Optional scientific-notation suffix: E[+-]?digits. Mirrors the
            // pre-Phase-2.10 behavior — only consumed when followed by at
            // least one exponent digit, otherwise `e` is left for the outer
            // loop, which now treats it as Constant.E.
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

    // Letter run starting at `start`. If the run is the single char 'e' AND
    // it is NOT immediately followed by '(' it is the constant `e`. Otherwise
    // the run is a function name (must be followed by '('). Unknown identifier
    // without a following '(' falls through silently (parser leniency mirrors
    // how unknown chars are skipped elsewhere).
    private fun parseIdentifier(start: Int, result: MutableList<ExpressionPart>): Int {
        var j = start
        while (j < calculation.length && calculation[j].isLetter()) j++
        val name = calculation.substring(start, j)
        val nextChar = calculation.getOrNull(j)
        return when {
            name == "e" && nextChar != '(' -> {
                result.add(ExpressionPart.Constant(ConstantSymbol.E))
                j
            }
            nextChar == '(' -> {
                result.add(ExpressionPart.Function(name.lowercase()))
                j
            }
            else -> j // unknown identifier, skip
        }
    }

    private fun parseParentheses(curChar: Char, result: MutableList<ExpressionPart>) {
        result.add(
            ExpressionPart.Parentheses(
                type = when (curChar) {
                    '(' -> ParenthesesType.Opening
                    ')' -> ParenthesesType.Closing
                    else -> throw IllegalArgumentException("Invalid parentheses type")
                }
            )
        )
    }
}
