package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import zed.rainxch.core.presentation.components.lists.KomiListContainer

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    KomiListContainer(modifier = modifier, content = content)
}
