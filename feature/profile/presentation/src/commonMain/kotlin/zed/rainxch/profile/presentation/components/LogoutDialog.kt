package zed.rainxch.profile.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.overlays.GhsConfirmDialog
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.close
import zed.rainxch.githubstore.core.presentation.res.logout
import zed.rainxch.githubstore.core.presentation.res.logout_confirmation
import zed.rainxch.githubstore.core.presentation.res.logout_revocation_note
import zed.rainxch.githubstore.core.presentation.res.warning

@Composable
fun LogoutDialog(
    onDismissRequest: () -> Unit,
    onLogout: () -> Unit,
) {
    GhsConfirmDialog(
        title = stringResource(Res.string.warning),
        body = stringResource(Res.string.logout_confirmation),
        note = stringResource(Res.string.logout_revocation_note),
        confirmLabel = stringResource(Res.string.logout),
        cancelLabel = stringResource(Res.string.close),
        destructive = true,
        onConfirm = onLogout,
        onDismiss = onDismissRequest,
    )
}
