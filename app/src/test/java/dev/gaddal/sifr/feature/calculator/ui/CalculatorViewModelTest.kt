package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import dev.gaddal.sifr.feature.calculator.domain.Operation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
        savedState: SavedStateHandle = SavedStateHandle(),
        writer: ExpressionWriter = ExpressionWriter(),
        settings: SettingsRepository = FakeSettingsRepository(),
    ) = CalculatorViewModel(
        writer = writer,
        historyRepository = history,
        inputBus = bus,
        savedStateHandle = savedState,
        settingsRepository = settings,
    )

    @Test
    fun `Initial state has empty expression and no error`() = runTest {
        val viewModel = newViewModel()

        viewModel.state.test {
            val initial = awaitItem()
            assertThat(initial.expression).isEqualTo("")
            assertThat(initial.error).isNull()
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
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `Divide by zero surfaces localized error in state`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)

        val s = viewModel.state.value
        assertThat(s.error).isEqualTo(
            UiText.StringResource(R.string.calc_error_division_by_zero)
        )
    }

    @Test
    fun `Calculate while in error state keeps error and does not crash`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)
        // Sanity
        assertThat(viewModel.state.value.error).isNotNull()

        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `Next digit after error clears error`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)
        assertThat(viewModel.state.value.error).isNotNull()

        viewModel.onAction(CalculatorAction.Number(7))

        val s = viewModel.state.value
        assertThat(s.error).isNull()
        assertThat(s.expression).isEqualTo("7")
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
    fun `Calculate on error does not write history`() = runTest {
        val history = FakeHistoryRepository()
        val viewModel = newViewModel(history = history)

        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)

        assertThat(history.added).isEmpty()
    }

    @Test
    fun `Successful Calculate emits CalculateSuccess feedback`() = runTest {
        val viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(CalculatorAction.Number(2))
            viewModel.onAction(CalculatorAction.Op(Operation.ADD))
            viewModel.onAction(CalculatorAction.Number(3))
            viewModel.onAction(CalculatorAction.Calculate)

            assertThat(awaitItem())
                .isEqualTo(CalculatorEvent.PlayFeedback(FeedbackIntent.CalculateSuccess))
        }
    }

    @Test
    fun `Failed Calculate emits Error feedback`() = runTest {
        val viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(CalculatorAction.Number(5))
            viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
            viewModel.onAction(CalculatorAction.Number(0))
            viewModel.onAction(CalculatorAction.Calculate)

            assertThat(awaitItem())
                .isEqualTo(CalculatorEvent.PlayFeedback(FeedbackIntent.Error))
        }
    }

    @Test
    fun `Clear with non-empty expression emits Destructive feedback`() = runTest {
        val viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(CalculatorAction.Number(7))
            viewModel.onAction(CalculatorAction.Clear)

            assertThat(awaitItem())
                .isEqualTo(CalculatorEvent.PlayFeedback(FeedbackIntent.Destructive))
        }
    }

    @Test
    fun `Clear with empty expression emits no feedback`() = runTest {
        val viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(CalculatorAction.Clear)
            expectNoEvents()
        }
    }

    @Test
    fun `Number action emits no feedback`() = runTest {
        val viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(CalculatorAction.Number(5))
            viewModel.onAction(CalculatorAction.Op(Operation.ADD))
            viewModel.onAction(CalculatorAction.Number(3))
            expectNoEvents()
        }
    }

    @Test
    fun `Initial state has no live preview`() = runTest {
        val viewModel = newViewModel()

        assertThat(viewModel.state.value.livePreview).isNull()
    }

    @Test
    fun `Live preview appears for evaluable expression`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))

        assertThat(viewModel.state.value.livePreview).isEqualTo("5")
    }

    @Test
    fun `Live preview is null after trailing operator`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))

        assertThat(viewModel.state.value.livePreview).isNull()
    }

    @Test
    fun `Live preview is null for division by zero in progress`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(1))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))

        // 1/0 evaluates to Infinity; preview must hide it rather than echo a fake answer
        assertThat(viewModel.state.value.livePreview).isNull()
    }

    @Test
    fun `Live preview clears after Calculate`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))
        assertThat(viewModel.state.value.livePreview).isEqualTo("5")

        viewModel.onAction(CalculatorAction.Calculate)

        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("5")
        // Preview must not echo the resolved expression back
        assertThat(s.livePreview).isNull()
    }

    @Test
    fun `Live preview clears when expression enters error state`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(1))
        viewModel.onAction(CalculatorAction.Op(Operation.DIVIDE))
        viewModel.onAction(CalculatorAction.Number(0))
        viewModel.onAction(CalculatorAction.Calculate)

        val s = viewModel.state.value
        assertThat(s.error).isNotNull()
        assertThat(s.livePreview).isNull()
    }

    @Test
    fun `Live preview is null for a single-number expression`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(5))

        // Nothing to evaluate — no operator, no parens
        assertThat(viewModel.state.value.livePreview).isNull()
    }

    @Test
    fun `Live preview is null for very long single-number entry`() = runTest {
        val viewModel = newViewModel()

        // 17 digits — exceeds Double's reliable decimal precision (~15-16).
        // Without the no-operator gate the preview would echo back a rounded
        // variant of the same number, which looks like a bug to the user.
        "12345678901234567".forEach { ch ->
            viewModel.onAction(CalculatorAction.Number(ch.digitToInt()))
        }

        assertThat(viewModel.state.value.expression).isEqualTo("12345678901234567")
        assertThat(viewModel.state.value.livePreview).isNull()
    }

    @Test
    fun `Live preview shows result for parenthesized expression`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Parentheses) // (
        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Parentheses) // )

        // ( ) still counts as "something to evaluate" — the preview shows.
        assertThat(viewModel.state.value.livePreview).isEqualTo("5")
    }

    // -------- cursor in state (Phase 2.9) --------

    @Test
    fun `Initial state cursor is zero`() = runTest {
        val viewModel = newViewModel()
        assertThat(viewModel.state.value.cursor).isEqualTo(0)
    }

    @Test
    fun `Cursor advances as digits are typed`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(1))
        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Number(3))

        assertThat(viewModel.state.value.cursor).isEqualTo(3)
    }

    @Test
    fun `CursorChanged action moves the cursor in state`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(CalculatorAction.Number(1))
        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Number(3))

        viewModel.onAction(CalculatorAction.CursorChanged(1))

        assertThat(viewModel.state.value.cursor).isEqualTo(1)
        // and a subsequent digit inserts at that cursor
        viewModel.onAction(CalculatorAction.Number(9))
        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("1923")
        assertThat(s.cursor).isEqualTo(2)
    }

    // -------- process-death persistence (Phase 2.9) --------

    @Test
    fun `Initial state restores expression and cursor from SavedStateHandle`() = runTest {
        val saved = SavedStateHandle(mapOf("expression" to "12+34", "cursor" to 3))

        val viewModel = newViewModel(savedState = saved)

        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("12+34")
        assertThat(s.cursor).isEqualTo(3)
        // Live preview is recomputed from the restored expression, not saved.
        assertThat(s.livePreview).isEqualTo("46")
    }

    @Test
    fun `Restored cursor is clamped to expression bounds`() = runTest {
        // Defensive: a stale bundle from a different app version might
        // contain a cursor that no longer fits.
        val saved = SavedStateHandle(mapOf("expression" to "7", "cursor" to 99))

        val viewModel = newViewModel(savedState = saved)

        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("7")
        assertThat(s.cursor).isEqualTo(1)
    }

    @Test
    fun `Empty SavedStateHandle starts with empty state`() = runTest {
        val viewModel = newViewModel(savedState = SavedStateHandle())

        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("")
        assertThat(s.cursor).isEqualTo(0)
    }

    @Test
    fun `Every successful action persists expression and cursor`() = runTest {
        val saved = SavedStateHandle()
        val viewModel = newViewModel(savedState = saved)

        viewModel.onAction(CalculatorAction.Number(1))
        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))

        assertThat(saved.get<String>("expression")).isEqualTo("12+3")
        assertThat(saved.get<Int>("cursor")).isEqualTo(4)
    }

    @Test
    fun `A new ViewModel sharing the same SavedStateHandle replays the writer state`() = runTest {
        // Simulates the process-death cycle: type something, kill the
        // process, recreate the VM with the persisted SavedStateHandle.
        val saved = SavedStateHandle()
        val first = newViewModel(savedState = saved)
        first.onAction(CalculatorAction.Number(9))
        first.onAction(CalculatorAction.Number(9))
        first.onAction(CalculatorAction.Op(Operation.MULTIPLY))
        first.onAction(CalculatorAction.Number(2))

        // Same SavedStateHandle, but a fresh writer (since DI gives factories)
        val second = newViewModel(savedState = saved, writer = ExpressionWriter())

        val s = second.state.value
        assertThat(s.expression).isEqualTo("99x2")
        assertThat(s.cursor).isEqualTo(4)
        assertThat(s.livePreview).isEqualTo("198")
    }

    @Test
    fun `Calculate moves cursor to end of result in state`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(CalculatorAction.Number(2))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))
        viewModel.onAction(CalculatorAction.CursorChanged(1))

        viewModel.onAction(CalculatorAction.Calculate)

        val s = viewModel.state.value
        assertThat(s.expression).isEqualTo("5")
        assertThat(s.cursor).isEqualTo(1)
    }

    @Test
    fun `ToggleMode flips settings between Basic and Scientific`() = runTest {
        val settings = FakeSettingsRepository()
        val viewModel = newViewModel(settings = settings)

        viewModel.onAction(CalculatorAction.ToggleMode)
        assertThat(settings.observe().first().calculatorMode).isEqualTo(CalculatorMode.Scientific)

        viewModel.onAction(CalculatorAction.ToggleMode)
        assertThat(settings.observe().first().calculatorMode).isEqualTo(CalculatorMode.Basic)
    }

    @Test
    fun `M plus stores current evaluated value in memory`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(CalculatorAction.Number(5))
        viewModel.onAction(CalculatorAction.Op(Operation.ADD))
        viewModel.onAction(CalculatorAction.Number(3))

        viewModel.onAction(CalculatorAction.MemoryAdd)

        assertThat(viewModel.state.value.memoryValue).isEqualTo(8.0)
    }

    @Test
    fun `MR inserts memory value at cursor`() = runTest {
        val viewModel = newViewModel()
        // seed memory via M+
        viewModel.onAction(CalculatorAction.Number(7))
        viewModel.onAction(CalculatorAction.MemoryAdd)
        viewModel.onAction(CalculatorAction.Clear)

        viewModel.onAction(CalculatorAction.MemoryRecall)

        assertThat(viewModel.state.value.expression).isEqualTo("7")
    }

    @Test
    fun `MC clears memory to null`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(CalculatorAction.Number(4))
        viewModel.onAction(CalculatorAction.MemoryAdd)
        assertThat(viewModel.state.value.memoryValue).isEqualTo(4.0)

        viewModel.onAction(CalculatorAction.MemoryClear)

        assertThat(viewModel.state.value.memoryValue).isNull()
    }

    @Test
    fun `Memory value survives process death via SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val first = newViewModel(savedState = handle)
        first.onAction(CalculatorAction.Number(9))
        first.onAction(CalculatorAction.MemoryAdd)

        // Simulate process death — create a fresh ViewModel with the same handle.
        val restored = newViewModel(savedState = handle)
        assertThat(restored.state.value.memoryValue).isEqualTo(9.0)
    }
}

private class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val flow = MutableStateFlow(initial)
    override fun observe(): Flow<AppSettings> = flow
    override suspend fun update(transform: AppSettings.() -> AppSettings) {
        flow.update { it.transform() }
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
