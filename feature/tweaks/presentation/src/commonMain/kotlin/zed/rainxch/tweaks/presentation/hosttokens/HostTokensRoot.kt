package zed.rainxch.tweaks.presentation.hosttokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.HostToken
import zed.rainxch.core.presentation.utils.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostTokensRoot(
    onNavigateBack: () -> Unit,
    viewModel: HostTokensViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HostTokensEvent.Message ->
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.text)
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentication tokens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAction(HostTokensAction.OnAddClicked) }) {
                Icon(Icons.Default.Add, contentDescription = "Add token")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Personal access tokens stored encrypted per forge host. " +
                    "Used to authenticate direct GitHub / Codeberg / Forgejo API calls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            state.validationMessage?.let { msg ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.tokens.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No tokens stored",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Tap + to add a personal access token",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.tokens, key = { it.host }) { token ->
                            TokenRow(
                                token = token,
                                isValidating = state.isValidating && state.pendingValidationFor == token.host,
                                onValidate = { viewModel.onAction(HostTokensAction.OnValidate(token.host)) },
                                onDelete = { viewModel.onAction(HostTokensAction.OnDelete(token.host)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isAddDialogVisible) {
        AddTokenDialog(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun TokenRow(
    token: HostToken,
    isValidating: Boolean,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.host,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = token.displayName ?: maskedToken(token.token),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                IconButton(onClick = onValidate) {
                    Icon(Icons.Default.Check, contentDescription = "Validate")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun maskedToken(token: String): String {
    if (token.length <= 8) return "•".repeat(token.length)
    return token.take(4) + "•".repeat(8) + token.takeLast(4)
}

@Composable
private fun AddTokenDialog(
    state: HostTokensState,
    onAction: (HostTokensAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(HostTokensAction.OnAddDismiss) },
        title = { Text("Add token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.draftHost,
                    onValueChange = { onAction(HostTokensAction.OnDraftHostChanged(it)) },
                    label = { Text("Host (e.g. github.com)") },
                    singleLine = true,
                    isError = state.draftHostError != null,
                    supportingText = state.draftHostError?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = state.draftToken,
                    onValueChange = { onAction(HostTokensAction.OnDraftTokenChanged(it)) },
                    label = { Text("Personal access token") },
                    singleLine = true,
                    isError = state.draftTokenError != null,
                    supportingText = state.draftTokenError?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = state.draftDisplayName,
                    onValueChange = { onAction(HostTokensAction.OnDraftDisplayNameChanged(it)) },
                    label = { Text("Display name (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(HostTokensAction.OnAddConfirm) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(HostTokensAction.OnAddDismiss) }) {
                Text("Cancel")
            }
        },
    )
}
