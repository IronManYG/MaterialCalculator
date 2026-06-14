package dev.gaddal.sifr.feature.history.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrCard
import dev.gaddal.sifr.core.ui.components.SifrRowDivider
import dev.gaddal.sifr.core.ui.feedback.rememberFeedbackController
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import dev.gaddal.sifr.feature.history.ui.components.HistoryItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HistoryRoot(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
    settingsRepository: SettingsRepository = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsRepository.observe()
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val feedback = rememberFeedbackController(
        hapticsEnabled = settings.hapticsEnabled,
        soundEnabled = settings.soundEnabled,
    )
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HistoryEvent.NavigateBack -> onNavigateBack()
            is HistoryEvent.PlayFeedback -> feedback.play(event.intent)
        }
    }
    HistoryScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onAction: (HistoryAction) -> Unit,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(HistoryAction.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.history_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = sifr.text,
                    navigationIconContentColor = sifr.text,
                ),
            )
        },
    ) { padding ->
        if (!state.isLoading && state.entries.isEmpty()) {
            HistoryEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Scroll so the card stays usable in short landscape / split-screen heights.
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        // Reserve room so the last row clears the pinned Clear-all pill.
                        .padding(top = 8.dp, bottom = 84.dp),
                ) {
                    if (state.entries.isNotEmpty()) {
                        SifrCard {
                            state.entries.forEachIndexed { index, entry ->
                                HistoryItem(
                                    entry = entry,
                                    onClick = { onAction(HistoryAction.EntryClicked(entry)) },
                                    onDelete = { onAction(HistoryAction.DeleteEntry(entry.id)) },
                                )
                                if (index != state.entries.lastIndex) SifrRowDivider()
                            }
                        }
                    }
                }
                if (state.entries.isNotEmpty()) {
                    // Fade the scrolling list into the page behind the pinned pill.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, sifr.backgroundFlat)),
                            ),
                    )
                    ClearAllPill(
                        onClick = { onAction(HistoryAction.ClearAllClicked) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                    )
                }
            }
        }
    }

    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(HistoryAction.DismissClearConfirm) },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(HistoryAction.ConfirmClearAll) }) {
                    Text(stringResource(R.string.history_clear_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(HistoryAction.DismissClearConfirm) }) {
                    Text(stringResource(R.string.history_clear_confirm_cancel))
                }
            },
        )
    }
}

/** Centered outline pill that triggers the clear-all confirm dialog (prototype HistoryScreen footer). */
@Composable
private fun ClearAllPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    // Positive tracking breaks Arabic letter-joins — apply the wide tracking in LTR only.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = stringResource(R.string.history_clear_all),
        color = sifr.dim,
        fontFamily = sifr.uiFamily,
        fontSize = 12.sp,
        letterSpacing = if (rtl) TextUnit.Unspecified else 0.08.em,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .border(BorderStroke(1.dp, sifr.hairline), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

@Composable
private fun HistoryEmptyState(
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Sifr's "٠" (zero) wordmark glyph — the empty-state hero (prototype noCalcs state).
        Text(
            text = "٠",
            color = sifr.dim,
            fontFamily = sifr.displayFamily,
            fontSize = 44.sp,
            modifier = Modifier
                .alpha(0.4f)
                .padding(bottom = 10.dp),
        )
        Text(
            text = stringResource(R.string.history_empty_title),
            color = sifr.text,
            fontFamily = sifr.uiFamily,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_empty_body),
            color = sifr.dim,
            fontFamily = sifr.uiFamily,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .padding(horizontal = 40.dp),
        )
    }
}

private val previewEntries = listOf(
    HistoryEntry(1, "12+5", "17", 1_700_000_000_000),
    HistoryEntry(2, "100x0.15", "15", 1_699_999_940_000),
    HistoryEntry(3, "(8-3)x6", "30", 1_699_996_400_000),
)

@Preview(name = "History — Farah light", showBackground = true)
@PreviewFontScale
@Composable
private fun HistoryPreviewFarahLight() =
    SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = previewEntries),
            onAction = {},
        )
    }

@Preview(name = "History — Layl dark", showBackground = true)
@Composable
private fun HistoryPreviewLaylDark() =
    SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = previewEntries),
            onAction = {},
        )
    }

@Preview(name = "History — overflow", showBackground = true)
@Composable
private fun HistoryPreviewOverflow() =
    SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
        HistoryScreen(
            state = HistoryState(
                isLoading = false,
                entries = listOf(
                    HistoryEntry(1, "1234567+89012345x67890-11111÷222", "5000000.5", 1_700_000_000_000),
                ),
            ),
            onAction = {},
        )
    }

@Preview(name = "History — Arabic / RTL", showBackground = true, locale = "ar")
@Composable
private fun HistoryPreviewArabic() =
    SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = previewEntries),
            onAction = {},
        )
    }

@Preview(name = "History — empty", showBackground = true)
@Composable
private fun HistoryEmptyPreview() =
    SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
        HistoryScreen(state = HistoryState(isLoading = false), onAction = {})
    }
