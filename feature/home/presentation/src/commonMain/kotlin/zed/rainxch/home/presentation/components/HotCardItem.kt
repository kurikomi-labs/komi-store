package zed.rainxch.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.home.presentation.model.HomeRepoCardUi
import zed.rainxch.home.presentation.model.toDiscoveryUi

@Composable
fun HotCardItem(
    card: HomeRepoCardUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiscoveryRepoCard(
        discoveryRepositoryUi = card.toDiscoveryUi(),
        onClick = onClick,
        onShareClick = {},
        onDeveloperClick = {},
        onLongPress = onLongClick,
        modifier = modifier,
        compact = true,
    )
}
