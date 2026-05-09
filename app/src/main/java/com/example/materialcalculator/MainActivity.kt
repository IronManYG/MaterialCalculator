package com.example.materialcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.materialcalculator.presentation.CalculatorScreen
import com.example.materialcalculator.ui.theme.MaterialCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialCalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}
