package zed.rainxch.tweaks.presentation.appinfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.tweaks.presentation.components.TweaksAccents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_app_name
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_licenses_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_licenses_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_privacy_policy_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_privacy_policy_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_source_code_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_source_code_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_tagline
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_whats_new_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_whats_new_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_app_info
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

private const val PRIVACY_POLICY_URL = "https://github-store.org/privacy"
private const val SOURCE_CODE_URL = "https://github.com/OpenHub-Store/GitHub-Store"

@Composable
fun TweaksAppInfoRoot(
    onNavigateBack: () -> Unit,
    onNavigateToWhatsNewHistory: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_app_info),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        item(key = "app_identity") {
            AppIdentityCard(versionName = state.versionName)
            Spacer(Modifier.height(16.dp))
        }

        item(key = "action_whats_new") {
            ActionRow(
                icon = Icons.Outlined.NewReleases,
                title = stringResource(Res.string.tweaks_app_info_whats_new_title),
                subtitle = stringResource(Res.string.tweaks_app_info_whats_new_subtitle),
                accent = TweaksAccents.Peach,
                onClick = onNavigateToWhatsNewHistory,
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "action_licenses") {
            ActionRow(
                icon = Icons.Outlined.Code,
                title = stringResource(Res.string.tweaks_app_info_licenses_title),
                subtitle = stringResource(Res.string.tweaks_app_info_licenses_subtitle),
                accent = TweaksAccents.Sage,
                onClick = onNavigateToLicenses,
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "action_privacy") {
            ActionRow(
                icon = Icons.Outlined.Description,
                title = stringResource(Res.string.tweaks_app_info_privacy_policy_title),
                subtitle = stringResource(Res.string.tweaks_app_info_privacy_policy_subtitle),
                accent = TweaksAccents.Rose,
                onClick = {
                    runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) }
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "action_source") {
            ActionRow(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                title = stringResource(Res.string.tweaks_app_info_source_code_title),
                subtitle = stringResource(Res.string.tweaks_app_info_source_code_subtitle),
                accent = TweaksAccents.Aqua,
                onClick = {
                    runCatching { uriHandler.openUri(SOURCE_CODE_URL) }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppIdentityCard(versionName: String) {
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(Radii.cardSm)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.tweaks_app_info_app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = versionName.ifBlank { "—" },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.tweaks_app_info_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = Color.Unspecified,
) {
    val tileBg = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        accent.copy(alpha = 0.14f)
    }
    val tint = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        accent
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(Radii.chip)
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
