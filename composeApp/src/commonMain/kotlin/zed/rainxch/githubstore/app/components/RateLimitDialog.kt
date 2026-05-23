package zed.rainxch.githubstore.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.RateLimitInfo
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.rate_limit_close
import zed.rainxch.githubstore.core.presentation.res.rate_limit_exceeded
import zed.rainxch.githubstore.core.presentation.res.rate_limit_ok
import zed.rainxch.githubstore.core.presentation.res.rate_limit_resets_in_minutes
import zed.rainxch.githubstore.core.presentation.res.rate_limit_sign_in
import zed.rainxch.githubstore.core.presentation.res.rate_limit_tip_sign_in
import zed.rainxch.githubstore.core.presentation.res.rate_limit_used_all
import zed.rainxch.githubstore.core.presentation.res.rate_limit_used_all_free

@Composable
fun RateLimitDialog(
    rateLimitInfo: RateLimitInfo,
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
) {
    val timeUntilReset =
        remember(rateLimitInfo) {
            rateLimitInfo.timeUntilReset().inWholeMinutes.toInt()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.rate_limit_exceeded),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text =
                        if (isAuthenticated) {
                            stringResource(
                                Res.string.rate_limit_used_all,
                                rateLimitInfo.limit,
                            )
                        } else {
                            stringResource(
                                Res.string.rate_limit_used_all_free,
                                60,
                            )
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

                Text(
                    text =
                        stringResource(
                            Res.string.rate_limit_resets_in_minutes,
                            timeUntilReset,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!isAuthenticated) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.rate_limit_tip_sign_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            if (!isAuthenticated) {
                GhsButton(
                    onClick = onSignIn,
                    label = stringResource(Res.string.rate_limit_sign_in),
                    variant = GhsButtonVariant.Primary,
                    size = GhsButtonSize.Sm,
                )
            } else {
                GhsButton(
                    onClick = onDismiss,
                    label = stringResource(Res.string.rate_limit_ok),
                    variant = GhsButtonVariant.Primary,
                    size = GhsButtonSize.Sm,
                )
            }
        },
        dismissButton = {
            GhsButton(
                onClick = onDismiss,
                label = stringResource(Res.string.rate_limit_close),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        },
    )
}

@Preview
@Composable
fun RateLimitDialogPreview() {
    GithubStoreTheme {
        RateLimitDialog(
            rateLimitInfo =
                RateLimitInfo(
                    limit = 1000,
                    remaining = 2000,
                    resetTimestamp = 0L,
                ),
            isAuthenticated = false,
            onDismiss = {
            },
            onSignIn = {
            },
        )
    }
}
