package zed.rainxch.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.product_telemetry_sheet_body
import zed.rainxch.githubstore.core.presentation.res.product_telemetry_sheet_deny
import zed.rainxch.githubstore.core.presentation.res.product_telemetry_sheet_grant
import zed.rainxch.githubstore.core.presentation.res.product_telemetry_sheet_title
import zed.rainxch.githubstore.core.presentation.res.product_telemetry_sheet_view_schema

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTelemetryConsentSheet(
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onViewSchema: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.product_telemetry_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.product_telemetry_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.product_telemetry_sheet_grant))
            }
            TextButton(onClick = onDeny, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.product_telemetry_sheet_deny))
            }
            TextButton(onClick = onViewSchema, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.product_telemetry_sheet_view_schema))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
