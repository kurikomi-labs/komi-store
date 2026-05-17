package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.AppsAction
import zed.rainxch.apps.presentation.AppsState
import zed.rainxch.githubstore.core.presentation.res.*

private const val URL_EXAMPLE = "https://example.com/app.apk"
private const val VERSION_EXAMPLE = "1.0.0"
private const val ICON_EXAMPLE = "https://example.com/icon.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectUrlBottomSheet(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(AppsAction.OnDismissDirectUrlSheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.direct_url_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.direct_url_sheet_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.directUrlDraft,
                onValueChange = { onAction(AppsAction.OnDirectUrlChanged(it)) },
                label = { Text(stringResource(Res.string.direct_url_field_url)) },
                placeholder = { Text(URL_EXAMPLE) },
                singleLine = true,
                isError = state.directUrlError != null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.directUrlNameDraft,
                onValueChange = { onAction(AppsAction.OnDirectUrlNameChanged(it)) },
                label = { Text(stringResource(Res.string.direct_url_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.directUrlVersionDraft,
                onValueChange = { onAction(AppsAction.OnDirectUrlVersionChanged(it)) },
                label = { Text(stringResource(Res.string.direct_url_field_version)) },
                placeholder = { Text(VERSION_EXAMPLE) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.directUrlIconDraft,
                onValueChange = { onAction(AppsAction.OnDirectUrlIconChanged(it)) },
                label = { Text(stringResource(Res.string.direct_url_field_icon)) },
                placeholder = { Text(ICON_EXAMPLE) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.directUrlError != null) {
                Text(
                    text = state.directUrlError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = androidx.compose.ui.Alignment.End),
            ) {
                TextButton(
                    onClick = { onAction(AppsAction.OnDismissDirectUrlSheet) },
                    enabled = !state.directUrlSaving,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = { onAction(AppsAction.OnConfirmAddDirectUrl) },
                    enabled = !state.directUrlSaving,
                ) {
                    if (state.directUrlSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(stringResource(Res.string.direct_url_add_button))
                }
            }
        }
    }
}
