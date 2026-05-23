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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
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
                GhsButton(
                    onClick = { onAction(ExternalImportAction.OnPermissionDenied(sdkInt)) },
                    label = stringResource(Res.string.external_import_permission_not_now),
                    variant = GhsButtonVariant.Outline,
                )
                GhsButton(
                    onClick = {
                        scope.launch {
                            onAction(ExternalImportAction.OnRequestPermission)

                            val action = if (requester.isGranted()) {
                                ExternalImportAction.OnPermissionGranted(sdkInt)
                            } else {
                                ExternalImportAction.OnPermissionDenied(sdkInt)
                            }
                            onAction(action)
                        }
                    },
                    label = stringResource(Res.string.external_import_permission_continue),
                    variant = GhsButtonVariant.Primary,
                )
            }
        }
    }
}
