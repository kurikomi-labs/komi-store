package zed.rainxch.apps.presentation.import.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.import.ExternalImportAction
import zed.rainxch.apps.presentation.import.util.rememberPackageVisibilityRequester
import zed.rainxch.apps.presentation.import.util.rememberSdkInt
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_permission_body
import zed.rainxch.githubstore.core.presentation.res.external_import_permission_continue
import zed.rainxch.githubstore.core.presentation.res.external_import_permission_not_now
import zed.rainxch.githubstore.core.presentation.res.external_import_permission_title

@Composable
fun PermissionRationaleScreen(
    onAction: (ExternalImportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sdkInt = rememberSdkInt()
    val requester = rememberPackageVisibilityRequester()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )

            Text(
                text = stringResource(Res.string.external_import_permission_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(Res.string.external_import_permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAction(ExternalImportAction.OnPermissionDenied(sdkInt)) },
                ) {
                    Text(stringResource(Res.string.external_import_permission_not_now))
                }
                Button(onClick = {
                    scope.launch {
                        onAction(ExternalImportAction.OnRequestPermission)
                        // QUERY_ALL_PACKAGES is install-time only on stock Android.
                        // Either we already have visibility (manifest grant honoured)
                        // → proceed Granted; or we don't → proceed Denied and let the
                        // scanner's heuristic-degraded path handle it. We never
                        // dispatch Granted optimistically — that would lie to
                        // telemetry and skip the degraded-path UX in EmptyStateScreen.
                        val action = if (requester.isGranted()) {
                            ExternalImportAction.OnPermissionGranted(sdkInt)
                        } else {
                            ExternalImportAction.OnPermissionDenied(sdkInt)
                        }
                        onAction(action)
                    }
                }) {
                    Text(stringResource(Res.string.external_import_permission_continue))
                }
            }
        }
    }
}
