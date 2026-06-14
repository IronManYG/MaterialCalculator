package dev.gaddal.sifr.feature.history.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val entry = HistoryEntry(1, "12+5", "17", 100L)

    private fun newViewModel(
        history: HistoryRepository = FakeHistoryRepository(listOf(entry)),
        bus: CalculatorInputBus = CalculatorInputBus(),
        settings: SettingsRepository = FakeSettingsRepository(),
    ) = HistoryViewModel(repository = history, bus = bus, settingsRepository = settings)

    @Test
    fun `EntryClicked restores the result by default`() = runTest {
        val bus = CalculatorInputBus()
        val viewModel = newViewModel(bus = bus, settings = FakeSettingsRepository())
        advanceUntilIdle()

        viewModel.onAction(HistoryAction.EntryClicked(entry))

        assertThat(bus.events.first()).isEqualTo("17")
    }

    @Test
    fun `EntryClicked restores the expression when the setting is Expression`() = runTest {
        val bus = CalculatorInputBus()
        val viewModel = newViewModel(
            bus = bus,
            settings = FakeSettingsRepository(AppSettings(restoreTarget = RestoreTarget.Expression)),
        )
        advanceUntilIdle()

        viewModel.onAction(HistoryAction.EntryClicked(entry))

        assertThat(bus.events.first()).isEqualTo("12+5")
    }

    @Test
    fun `restoreTarget mirrors the persisted setting in state`() = runTest {
        val settings = FakeSettingsRepository(AppSettings(restoreTarget = RestoreTarget.Expression))
        val viewModel = newViewModel(settings = settings)

        viewModel.state.test {
            // Skip the initial default, await the collected setting.
            var current = awaitItem()
            while (current.restoreTarget != RestoreTarget.Expression) current = awaitItem()
            assertThat(current.restoreTarget).isEqualTo(RestoreTarget.Expression)
            cancelAndIgnoreRemainingEvents()
        }
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

private class FakeHistoryRepository(
    initial: List<HistoryEntry> = emptyList(),
) : HistoryRepository {
    private val flow = MutableStateFlow(initial)
    override fun observe(): Flow<List<HistoryEntry>> = flow
    override suspend fun add(expression: String, result: String) {
        flow.update { listOf(HistoryEntry(it.size + 1L, expression, result, 0L)) + it }
    }
    override suspend fun delete(id: Long) {
        flow.update { entries -> entries.filterNot { it.id == id } }
    }
    override suspend fun clear() {
        flow.value = emptyList()
    }
}
