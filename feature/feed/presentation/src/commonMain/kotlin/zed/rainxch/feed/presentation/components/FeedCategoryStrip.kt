package zed.rainxch.feed.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import zed.rainxch.core.domain.model.repository.FeedCategory
import zed.rainxch.core.presentation.components.chips.KomiChip
import zed.rainxch.core.presentation.components.chips.KomiChipKind
import zed.rainxch.core.presentation.utils.toLabel

@Composable
fun FeedCategoryStrip(
    categories: ImmutableList<FeedCategory>,
    selected: FeedCategory,
    onSelect: (FeedCategory) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, category -> category.name }) { index, category ->
            KomiChip(
                label = category.toLabel(),
                kind = KomiChipKind.Filter,
                selected = category == selected,
                index = index,
                onClick = { onSelect(category) },
            )
        }
    }
}
