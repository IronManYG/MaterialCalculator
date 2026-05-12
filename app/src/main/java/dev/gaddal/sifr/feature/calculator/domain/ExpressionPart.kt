package dev.gaddal.sifr.feature.calculator.domain

sealed interface ExpressionPart {
    data class Number(val number: Double) : ExpressionPart
    data class Op(val operator: Operation) : ExpressionPart
    data class Parentheses(val type: ParenthesesType) : ExpressionPart
    data class Function(val name: String) : ExpressionPart
    data class Constant(val symbol: ConstantSymbol) : ExpressionPart
    data class Postfix(val op: PostfixOp) : ExpressionPart
}

sealed interface ParenthesesType {
    object Opening : ParenthesesType
    object Closing : ParenthesesType
}
