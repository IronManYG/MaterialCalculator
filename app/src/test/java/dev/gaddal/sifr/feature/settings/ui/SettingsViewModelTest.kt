package dev.gaddal.sifr.feature.settings.ui

import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppLanguage
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.ui.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `SetLanguage updates the persisted language`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SetLanguage(AppLanguage.Arabic))
        advanceUntilIdle()

        assertThat(viewModel.state.value.settings.language).isEqualTo(AppLanguage.Arabic)
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
