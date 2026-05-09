package dev.gaddal.sifr.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import dev.gaddal.sifr.feature.history.ui.components.HistoryItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryRoot(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HistoryEvent.NavigateBack -> onNavigateBack()
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
    Scaffold(
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
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { onAction(HistoryAction.ClearAllClicked) }) {
                            Icon(
                                imageVector = Icons.Outlined.PlaylistRemove,
                                contentDescription = stringResource(R.string.history_clear_all),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isLoading && state.entries.isEmpty()) {
            HistoryEmptyState(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = padding,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
            ) {
                items(items = state.entries, key = { it.id }) { entry ->
                    HistoryItem(
                        entry = entry,
                        onClick = { onAction(HistoryAction.EntryClicked(entry)) },
                        onDelete = { onAction(HistoryAction.DeleteEntry(entry.id)) },
                    )
                    HorizontalDivider()
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

@Composable
private fun HistoryEmptyState(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier
                .padding(contentPadding)
                .padding(bottom = 12.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
        )
    }
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    SifrTheme {
        HistoryScreen(
            state = HistoryState(
                isLoading = false,
                entries = listOf(
                    HistoryEntry(1, "12+5", "17", System.currentTimeMillis()),
                    HistoryEntry(2, "100x0.15", "15", System.currentTimeMillis() - 60_000),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun HistoryEmptyPreview() {
    SifrTheme {
        HistoryScreen(state = HistoryState(isLoading = false), onAction = {})
    }
}
