package zed.rainxch.apps.presentation.import.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.import.model.ImportPhase
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_progress_auto_importing
import zed.rainxch.githubstore.core.presentation.res.external_import_progress_scanning
import zed.rainxch.githubstore.core.presentation.res.external_import_progress_skip
import zed.rainxch.githubstore.core.presentation.res.external_import_progress_subtitle_count
import zed.rainxch.githubstore.core.presentation.res.external_import_progress_working

@Composable
fun ImportProgressScreen(
    phase: ImportPhase,
    totalCandidates: Int,
    modifier: Modifier = Modifier,
    canSkip: Boolean = false,
    onSkip: () -> Unit = {},
) {
    val headline =
        when (phase) {
            ImportPhase.Scanning -> stringResource(Res.string.external_import_progress_scanning)
            ImportPhase.AutoImporting -> stringResource(Res.string.external_import_progress_auto_importing)
            else -> stringResource(Res.string.external_import_progress_working)
        }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
            )

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            Text(
                text =
                    pluralStringResource(
                        Res.plurals.external_import_progress_subtitle_count,
                        totalCandidates,
                        totalCandidates,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Skip affordance fades in once the scan crosses the
            // long-running threshold (the VM flips `canSkip` after
            // SKIP_REVEAL_DELAY_MS). Hidden during a fast scan so it
            // doesn't add a flicker the user has no time to notice.
            AnimatedVisibility(
                visible = canSkip,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(Res.string.external_import_progress_skip),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
