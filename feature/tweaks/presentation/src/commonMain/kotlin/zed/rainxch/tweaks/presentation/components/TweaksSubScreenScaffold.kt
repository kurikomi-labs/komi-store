package zed.rainxch.tweaks.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zed.rainxch.core.presentation.components.overlays.KomiToastState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.back_cd

@Composable
fun TweaksSubScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    toastState: KomiToastState,
    content: LazyListScope.() -> Unit,
) {
    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = title,
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_cd),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
            )
        },
        toastState = toastState,
    ) { innerPadding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
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
