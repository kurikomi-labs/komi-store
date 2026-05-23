package zed.rainxch.tweaks.presentation.storage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.downloads_cleared
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_storage
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksEvent
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.ClearDownloadsDialog
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

@Composable
fun TweaksStorageRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TweaksEvent.OnCacheCleared -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.downloads_cleared))
                }
            }
            is TweaksEvent.OnCacheClearError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }
            else -> Unit
        }
    }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_storage),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        item(key = "storage_card") {
            DownloadsCard(
                cacheSize = state.cacheSize,
                onClearClick = { viewModel.onAction(TweaksAction.OnClearCacheClick) },
            )
        }
    }

    if (state.isClearDownloadsDialogVisible) {
        ClearDownloadsDialog(
            cacheSize = state.cacheSize,
            onDismissRequest = { viewModel.onAction(TweaksAction.OnClearDownloadsDismiss) },
            onConfirm = { viewModel.onAction(TweaksAction.OnClearDownloadsConfirm) },
        )
    }
}

@Composable
private fun DownloadsCard(
    cacheSize: String,
    onClearClick: () -> Unit,
) {
    val sizeDisplay = cacheSize.ifBlank { "0 B" }
    val isEmpty = sizeDisplay == "0 B"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(Radii.chip)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Downloaded APKs",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "We keep installers around so updates resume fast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Using: $sizeDisplay",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GhsButton(
                onClick = onClearClick,
                label = "Clear",
                variant = GhsButtonVariant.Destructive,
                enabled = !isEmpty,
                leadingIcon = Icons.Outlined.DeleteOutline,
            )
        }
    }
}
