package zed.rainxch.tweaks.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import zed.rainxch.core.presentation.components.overlays.KomiToastState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.tweaks.presentation.components.shell.TweaksDecorSlot
import zed.rainxch.tweaks.presentation.components.shell.TweaksMangaHeader

@Composable
fun TweaksSubScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    toastState: KomiToastState,
    slot: TweaksDecorSlot? = null,
    content: LazyListScope.() -> Unit,
) {
    KomiScaffold(
        grid = true,
        screentone = true,
        topBar = {
            TweaksMangaHeader(title = title, slot = slot, onNavigateBack = onNavigateBack)
        },
        toastState = toastState,
    ) { innerPadding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .constrainedContentWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                        .arrowKeyScroll(listState, autoFocus = true),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            ) {
                content()
            }
        }
    }
}
