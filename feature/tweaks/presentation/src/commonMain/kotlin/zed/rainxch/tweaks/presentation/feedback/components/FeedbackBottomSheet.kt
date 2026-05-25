package zed.rainxch.tweaks.presentation.feedback.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.feedback_close
import zed.rainxch.githubstore.core.presentation.res.feedback_field_description
import zed.rainxch.githubstore.core.presentation.res.feedback_field_title
import zed.rainxch.githubstore.core.presentation.res.feedback_title
import zed.rainxch.tweaks.presentation.feedback.FeedbackAction
import zed.rainxch.tweaks.presentation.feedback.FeedbackEvent
import zed.rainxch.tweaks.presentation.feedback.FeedbackState
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

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FeedbackEvent.OnSent -> onSent(event.channel)
            is FeedbackEvent.OnSendError -> onError(event.message)
        }
    }

    val dismiss = {
        viewModel.onAction(FeedbackAction.OnDismiss)
        onDismiss()
    }

    if (getPlatform() == Platform.ANDROID) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = sheetState,
        ) {
            FeedbackContent(
                state = state,
                onAction = viewModel::onAction,
                onDismiss = dismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    } else {
        Dialog(
            onDismissRequest = dismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .heightIn(max = 720.dp)
                    .clip(WonkySquircleShape.Dialog)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                FeedbackContent(
                    state = state,
                    onAction = viewModel::onAction,
                    onDismiss = dismiss,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackContent(
    state: FeedbackState,
    onAction: (FeedbackAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FeedbackHeader(onDismiss = onDismiss)

        SectionLabel(text = stringResource(Res.string.feedback_field_title) + " *", topGap = 0.dp)
        GhsTextField(
            value = state.title,
            onValueChange = { onAction(FeedbackAction.OnTitleChange(it)) },
            label = stringResource(Res.string.feedback_field_title),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(text = stringResource(Res.string.feedback_field_description) + " *")
        GhsTextField(
            value = state.description,
            onValueChange = { onAction(FeedbackAction.OnDescriptionChange(it)) },
            label = stringResource(Res.string.feedback_field_description),
            singleLine = false,
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        CategorySelector(
            selected = state.category,
            onSelected = { onAction(FeedbackAction.OnCategoryChange(it)) },
        )

        TopicSelector(
            selected = state.topic,
            onSelected = { onAction(FeedbackAction.OnTopicChange(it)) },
        )

        ConditionalFields(state = state, onAction = onAction)

        DiagnosticsPreview(
            diagnostics = state.diagnostics,
            channel = FeedbackChannel.GITHUB,
            enabled = state.attachDiagnostics,
            onToggle = { onAction(FeedbackAction.OnAttachDiagnosticsToggle) },
        )

        SendActions(
            canSend = state.canSend,
            isSending = state.isSending,
            onSendEmail = { onAction(FeedbackAction.OnSendViaEmail) },
            onSendGithub = { onAction(FeedbackAction.OnSendViaGithub) },
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeedbackHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.feedback_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.feedback_close),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, topGap: androidx.compose.ui.unit.Dp = 4.dp) {
    Column {
        if (topGap > 0.dp) Spacer(Modifier.height(topGap))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
