package zed.rainxch.apps.presentation.import.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_add_manually
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_all_matched
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_done
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_grant_permission
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_no_apps_body
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_no_apps_title
import zed.rainxch.githubstore.core.presentation.res.external_import_empty_ok

@Composable
fun EmptyStateScreen(
    isPermissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onExit: () -> Unit,
    onAddManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!isPermissionDenied) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = stringResource(Res.string.external_import_empty_all_matched),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onExit) {
                    Text(stringResource(Res.string.external_import_empty_done))
                }
                TextButton(onClick = onAddManually) {
                    Text(stringResource(Res.string.external_import_empty_add_manually))
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = stringResource(Res.string.external_import_empty_no_apps_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.external_import_empty_no_apps_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onExit) {
                        Text(stringResource(Res.string.external_import_empty_ok))
                    }
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(Res.string.external_import_empty_grant_permission))
                    }
                }
                TextButton(onClick = onAddManually) {
                    Text(stringResource(Res.string.external_import_empty_add_manually))
                }
            }
        }
    }
}
