package zed.rainxch.tweaks.presentation.feedback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.feedback_close
import zed.rainxch.githubstore.core.presentation.res.feedback_field_description
import zed.rainxch.githubstore.core.presentation.res.feedback_field_title
import zed.rainxch.githubstore.core.presentation.res.feedback_title
import zed.rainxch.tweaks.presentation.feedback.FeedbackAction
import zed.rainxch.tweaks.presentation.feedback.FeedbackEvent
import zed.rainxch.tweaks.presentation.feedback.FeedbackViewModel
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    onDismiss: () -> Unit,
    onSent: (FeedbackChannel) -> Unit,
    onError: (String) -> Unit,
    viewModel: FeedbackViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FeedbackEvent.OnSent -> onSent(event.channel)
            is FeedbackEvent.OnSendError -> onError(event.message)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onAction(FeedbackAction.OnDismiss)
            onDismiss()
        },
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.feedback_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = {
                    viewModel.onAction(FeedbackAction.OnDismiss)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.feedback_close),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            CategorySelector(
                selected = state.category,
                onSelected = { viewModel.onAction(FeedbackAction.OnCategoryChange(it)) },
            )

            TopicSelector(
                selected = state.topic,
                onSelected = { viewModel.onAction(FeedbackAction.OnTopicChange(it)) },
            )

            GhsTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(FeedbackAction.OnTitleChange(it)) },
                label = stringResource(Res.string.feedback_field_title) + " *",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            GhsTextField(
                value = state.description,
                onValueChange = { viewModel.onAction(FeedbackAction.OnDescriptionChange(it)) },
                label = stringResource(Res.string.feedback_field_description) + " *",
                singleLine = false,
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            ConditionalFields(
                state = state,
                onAction = viewModel::onAction,
            )

            DiagnosticsPreview(
                diagnostics = state.diagnostics,
                channel = FeedbackChannel.GITHUB,
                enabled = state.attachDiagnostics,
                onToggle = { viewModel.onAction(FeedbackAction.OnAttachDiagnosticsToggle) },
            )

            SendActions(
                canSend = state.canSend,
                isSending = state.isSending,
                onSendEmail = { viewModel.onAction(FeedbackAction.OnSendViaEmail) },
                onSendGithub = { viewModel.onAction(FeedbackAction.OnSendViaGithub) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
