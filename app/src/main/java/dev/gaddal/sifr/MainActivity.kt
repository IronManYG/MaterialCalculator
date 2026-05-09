package dev.gaddal.sifr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SifrTheme {
                CalculatorRoot()
            }
        }
    }
}
