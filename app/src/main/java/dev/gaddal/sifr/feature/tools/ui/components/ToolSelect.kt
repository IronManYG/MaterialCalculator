package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Dropdown selector for units and currencies. Shows [selected]; tapping expands [options].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSelect(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val sifr = SifrTokens.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it, fontSize = 11.sp, color = sifr.dim) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(
                fontFamily = sifr.uiFamily,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = sifr.text,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTrailingIconColor = sifr.dim,
                unfocusedTrailingIconColor = sifr.dim,
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .border(1.dp, sifr.hairline, shape),
            shape = shape,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = sifr.surface,
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontFamily = sifr.uiFamily,
                            fontSize = 13.sp,
                            color = if (opt == selected) sifr.accent else sifr.text,
                        )
                    },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}
