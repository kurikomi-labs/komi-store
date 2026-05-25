package zed.rainxch.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.chrome.GhsHomeTopBar
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_topbar_discover

@Composable
fun HomeTopBar(modifier: Modifier = Modifier) {
    GhsHomeTopBar(
        title = stringResource(Res.string.home_topbar_discover),
        modifier = modifier,
        applyStatusBarPadding = false,
    )
}
