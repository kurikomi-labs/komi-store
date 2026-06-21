package zed.rainxch.tweaks.presentation.skipped

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.surfaces.KomiSurface
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.navigate_back
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_empty_description
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_empty_title
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_installed_label
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_skipped_label
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_title
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_unskip_action
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_unskip_failure
import zed.rainxch.githubstore.core.presentation.res.skipped_updates_unskipped_snackbar

@Composable
fun SkippedUpdatesRoot(
    onNavigateBack: () -> Unit,
    viewModel: SkippedUpdatesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SkippedUpdatesEvent.Unskipped ->
                coroutineScope.launch {
                    toastState.info(
                        getString(Res.string.skipped_updates_unskipped_snackbar, event.appName),
                    )
                }
            is SkippedUpdatesEvent.Failure ->
                coroutineScope.launch {
                    toastState.danger(
                        getString(Res.string.skipped_updates_unskip_failure),
                    )
                }
        }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.skipped_updates_title),
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
            )
        },
        toastState = toastState,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            val colors = LocalPersonality.current.colors
            when {
                state.isLoading -> {
                    KomiCircularProgress(modifier = Modifier.align(Alignment.Center))
                }

                state.items.isEmpty() -> {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        KomiText(
                            text = stringResource(Res.string.skipped_updates_empty_title),
                            role = KomiTextRole.Title,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.size(8.dp))
                        KomiText(
                            text = stringResource(Res.string.skipped_updates_empty_description),
                            role = KomiTextRole.Body,
                            color = colors.onSurfaceVariant,
                            uppercase = false,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.packageName }) { item ->
                            SkippedAppRow(
                                item = item,
                                onUnskip = {
                                    viewModel.onAction(SkippedUpdatesAction.OnUnskip(item.packageName))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkippedAppRow(
    item: SkippedAppUi,
    onUnskip: () -> Unit,
) {
    val colors = LocalPersonality.current.colors
    KomiSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                KomiText(
                    text = item.appName,
                    role = KomiTextRole.Title,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    uppercase = false,
                )
                KomiText(
                    text =
                        stringResource(
                            Res.string.skipped_updates_skipped_label,
                            item.skippedTag,
                        ),
                    role = KomiTextRole.Body,
                    fontSize = 13.sp,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    uppercase = false,
                )
                if (item.installedVersion.isNotBlank()) {
                    KomiText(
                        text =
                            stringResource(
                                Res.string.skipped_updates_installed_label,
                                item.installedVersion,
                            ),
                        role = KomiTextRole.Body,
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        uppercase = false,
                    )
                }
            }

            KomiButton(
                onClick = onUnskip,
                label = stringResource(Res.string.skipped_updates_unskip_action),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        }
    }
}
