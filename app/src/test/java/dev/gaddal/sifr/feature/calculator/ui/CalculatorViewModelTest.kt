package dev.gaddal.sifr.feature.calculator.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import dev.gaddal.sifr.feature.calculator.domain.Operation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(
        history: HistoryRepository = FakeHistoryRepository(),
        bus: CalculatorInputBus = CalculatorInputBus(),
    ) = CalculatorViewModel(
        writer = ExpressionWriter(),
        historyRepository = history,
        inputBus = bus,
    )

    @Test
    fun `Initial state has empty expression`() = runTest {
        val viewModel = newViewModel()

        viewModel.state.test {
            assertThat(awaitItem().expression).isEqualTo("")
        }
    }

    @Test
    fun `Number action appends digit to expression`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(5))

        assertThat(viewModel.state.value.expression).isEqualTo("5")
    }

    @Test
    fun `Calculate produces result`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(viewModel.state.value.expression).isEqualTo("5")
    }

    @Test
    fun `Calculate while in Error state stays Error`() = runTest {
        val viewModel = newViewModel()

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

    @Test
    fun `Calculate writes a history entry on success`() = runTest {
        val history = FakeHistoryRepository()
        val viewModel = newViewModel(history = history)

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(history.added).containsExactly("2+3" to "5")
    }

    @Test
    fun `Calculate on Error does not write history`() = runTest {
        val history = FakeHistoryRepository()
        val viewModel = newViewModel(history = history)

        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(history.added).isEmpty()
    }
}

private class FakeHistoryRepository : HistoryRepository {
    val added = mutableListOf<Pair<String, String>>()
    private val flow = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observe(): Flow<List<HistoryEntry>> = flow

    override suspend fun add(expression: String, result: String) {
        added += expression to result
        flow.update { it + HistoryEntry(it.size + 1L, expression, result, 0L) }
    }

    override suspend fun delete(id: Long) {
        flow.update { entries -> entries.filterNot { it.id == id } }
    }

    override suspend fun clear() {
        flow.value = emptyList()
    }
}
