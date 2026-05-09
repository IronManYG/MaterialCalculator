package dev.gaddal.sifr.feature.calculator.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import dev.gaddal.sifr.feature.calculator.domain.Operation
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Initial state has empty expression`() = runTest {
        val viewModel = CalculatorViewModel(ExpressionWriter())

        viewModel.state.test {
            assertThat(awaitItem().expression).isEqualTo("")
        }
    }

    @Test
    fun `Number action appends digit to expression`() = runTest {
        val viewModel = CalculatorViewModel(ExpressionWriter())

        viewModel.onAction(CalculatorAction.Number(5))

        assertThat(viewModel.state.value.expression).isEqualTo("5")
    }

    @Test
    fun `Calculate produces result`() = runTest {
        val viewModel = CalculatorViewModel(ExpressionWriter())

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(viewModel.state.value.expression).isEqualTo("5")
    }

    @Test
    fun `Calculate while in Error state stays Error`() = runTest {
        val viewModel = CalculatorViewModel(ExpressionWriter())

        // Trigger Error state via division by zero
        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)
        assertThat(viewModel.state.value.expression).isEqualTo("Error")

        // Pressing = again should not crash and should remain in Error state
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(viewModel.state.value.expression).isEqualTo("Error")
    }
}
