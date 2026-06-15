package dev.gaddal.sifr.feature.tools.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val seedSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.75, "EUR" to 0.87),
        asOf = LocalDate.of(2026, 6, 10),
    )

    private inner class FakeRepo : CurrencyRepository {
        val flow = MutableStateFlow<RatesResource>(RatesResource.Loading)
        override val rates: Flow<RatesResource> = flow
        override suspend fun refresh(force: Boolean) {
            flow.value = RatesResource.SeedFallback(seedSnapshot)
        }
    }

    private fun vm(
        repo: CurrencyRepository = FakeRepo(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = ToolsViewModel(repo, handle)

    @Test
    fun `initial tab is Units with UVal focused`() = runTest {
        val v = vm()
        v.state.test {
            val s = awaitItem()
            assertThat(s.activeTab).isEqualTo(ToolTab.Units)
            assertThat(s.focusedField).isEqualTo(FocusedField.UVal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectTab changes tab and resets focus to tab default`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SelectTab(ToolTab.Tip))
        val s = v.state.value
        assertThat(s.activeTab).isEqualTo(ToolTab.Tip)
        assertThat(s.focusedField).isEqualTo(FocusedField.Bill)
    }

    @Test
    fun `Date tab sets focusedField to null`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SelectTab(ToolTab.Date))
        assertThat(v.state.value.focusedField).isNull()
    }

    @Test
    fun `numpad builds uVal and recomputes uResult`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.NumKey('1'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.NumKey('0'))
        val s = v.state.value
        assertThat(s.uVal).isEqualTo("100")
        assertThat(s.uResult).isEqualTo("0.1") // 100 m → km
    }

    @Test
    fun `Backspace removes last char`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.NumKey('5'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.Backspace)
        assertThat(v.state.value.uVal).isEqualTo("5")
    }

    @Test
    fun `SelectCategory resets from and to to category defaults`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SelectCategory(UnitCategory.Weight))
        val s = v.state.value
        assertThat(s.unitsCat).isEqualTo(UnitCategory.Weight)
        assertThat(s.uFrom).isEqualTo("kg")
        assertThat(s.uTo).isEqualTo("g")
    }

    @Test
    fun `IncrementSplit increases split and recomputes tip each`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SelectTab(ToolTab.Tip))
        v.onAction(ToolsAction.NumKey('1'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.IncrementSplit)
        val s = v.state.value
        assertThat(s.split).isEqualTo(2)
        assertThat(s.eachOut).isEqualTo("57.5") // bill=100, tip 15% → total 115, /2 = 57.5
    }

    @Test
    fun `SavedStateHandle restores tab and field values`() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                "tools_tab" to ToolTab.Currency.ordinal,
                "tools_c_from" to "EUR",
                "tools_c_to" to "USD",
                "tools_c_val" to "50",
            )
        )
        val v = vm(handle = handle)
        advanceUntilIdle()
        val s = v.state.value
        assertThat(s.activeTab).isEqualTo(ToolTab.Currency)
        assertThat(s.cFrom).isEqualTo("EUR")
        assertThat(s.cVal).isEqualTo("50")
    }

    @Test
    fun `currency conversion uses snapshot rates`() = runTest {
        val v = vm()
        advanceUntilIdle() // FakeRepo.refresh() pushes SeedFallback (USD=1.0, SAR=3.75)
        v.onAction(ToolsAction.SelectTab(ToolTab.Currency))
        // defaults cFrom=USD, cTo=SAR
        v.onAction(ToolsAction.NumKey('1'))
        v.onAction(ToolsAction.NumKey('0'))
        v.onAction(ToolsAction.NumKey('0'))
        // 100 USD / 1.0 * 3.75 = 375
        assertThat(v.state.value.cResult).isEqualTo("375")
    }

    @Test
    fun `second decimal point is rejected`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.NumKey('1'))
        v.onAction(ToolsAction.NumKey('.'))
        v.onAction(ToolsAction.NumKey('5'))
        v.onAction(ToolsAction.NumKey('.')) // rejected
        v.onAction(ToolsAction.NumKey('2'))
        assertThat(v.state.value.uVal).isEqualTo("1.52")
    }

    @Test
    fun `leading decimal yields 0 dot`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.NumKey('.'))
        v.onAction(ToolsAction.NumKey('5'))
        assertThat(v.state.value.uVal).isEqualTo("0.5")
    }

    @Test
    fun `field length is capped at 12 chars`() = runTest {
        val v = vm()
        advanceUntilIdle()
        "1234567890123".forEach { v.onAction(ToolsAction.NumKey(it)) } // 13 digits
        assertThat(v.state.value.uVal).isEqualTo("123456789012")
    }

    @Test
    fun `SetDate1 recomputes diff days and weeks`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SetDate1(LocalDate.of(2026, 1, 1)))
        v.onAction(ToolsAction.SetDate2(LocalDate.of(2026, 1, 22)))
        val s = v.state.value
        assertThat(s.diffDays).isEqualTo(21L)
        assertThat(s.diffWeeks).isEqualTo(3)
        assertThat(s.diffRemainingDays).isEqualTo(0)
    }

    @Test
    fun `DecrementSplit floors at 1`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onAction(ToolsAction.SelectTab(ToolTab.Tip))
        v.onAction(ToolsAction.DecrementSplit) // already at 1, must stay 1
        assertThat(v.state.value.split).isEqualTo(1)
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.events.test {
            v.onAction(ToolsAction.BackClicked)
            assertThat(awaitItem()).isEqualTo(ToolsEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
