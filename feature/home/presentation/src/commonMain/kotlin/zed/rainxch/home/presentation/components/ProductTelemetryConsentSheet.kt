package zed.rainxch.home.presentation.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onViewSchemaSource: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Scoped to the sheet's lifetime — closing and re-opening the sheet
    // resets to the consent panel, which matches what a "first launch"
    // consent dialog should do.
    var showSchema by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (showSchema) {
            PrivacyCollectedView(
                onBack = { showSchema = false },
                onViewSource = onViewSchemaSource,
            )
        } else {
            ConsentPanel(
                onGrant = onGrant,
                onDeny = onDeny,
                onViewSchema = { showSchema = true },
            )
        }
    }
}

@Composable
private fun ConsentPanel(
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onViewSchema: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.product_telemetry_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.product_telemetry_sheet_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.product_telemetry_sheet_grant))
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onDeny,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.product_telemetry_sheet_deny))
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onViewSchema,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.product_telemetry_sheet_view_schema))
        }
        Spacer(Modifier.height(8.dp))
    }
}
