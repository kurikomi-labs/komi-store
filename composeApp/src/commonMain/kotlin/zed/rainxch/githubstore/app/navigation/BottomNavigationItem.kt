package zed.rainxch.githubstore.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

data class BottomNavigationItem(
    val titleRes: StringResource,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val screen: GithubStoreGraph,
)
