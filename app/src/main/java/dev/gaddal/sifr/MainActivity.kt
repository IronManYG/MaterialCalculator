package dev.gaddal.sifr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.navigation.NavRoot
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepo: SettingsRepository = koinInject()
            val settings by settingsRepo.observe()
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            SifrTheme(themeMode = settings.themeMode) {
                NavRoot()
            }
        }
    }
}
