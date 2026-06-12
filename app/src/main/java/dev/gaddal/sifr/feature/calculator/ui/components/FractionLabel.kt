package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.feature.calculator.domain.Fraction

// super/subscript glyph size relative to the whole-number size (design spec)
private const val FractionGlyphScale = 0.62f

@Composable
fun FractionLabel(
    fraction: Fraction,
    color: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
) {
    val small = SpanStyle(fontSize = fontSize * FractionGlyphScale)
    val text = buildAnnotatedString {
        if (fraction.sign < 0) append("−")
        if (fraction.whole != 0L) append("${fraction.whole} ")
        withStyle(small.copy(baselineShift = BaselineShift.Superscript)) { append("${fraction.n}") }
        append("⁄") // fraction slash ⁄
        withStyle(small.copy(baselineShift = BaselineShift.Subscript)) { append("${fraction.d}") }
    }
    BasicText(
        text = text,
        style = TextStyle(color = color, fontFamily = fontFamily, fontSize = fontSize),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101010)
@Composable
private fun PreviewFractionLabels() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        FractionLabel(Fraction(1, 0L, 3L, 4L), color = Color(0xFFE0E0E0), fontFamily = FontFamily.Default)   // 3⁄4
        FractionLabel(Fraction(1, 2L, 1L, 2L), color = Color(0xFFE0E0E0), fontFamily = FontFamily.Default)   // 2 1⁄2
        FractionLabel(Fraction(-1, 0L, 1L, 3L), color = Color(0xFFE0E0E0), fontFamily = FontFamily.Default)  // −1⁄3
    }
}
