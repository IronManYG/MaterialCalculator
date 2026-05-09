package dev.gaddal.sifr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.gaddal.sifr.feature.calculator.ui.CalculatorScreen
import dev.gaddal.sifr.core.ui.theme.SifrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SifrTheme {
                CalculatorScreen()
            }
        }
    }
}
