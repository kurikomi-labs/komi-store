package zed.rainxch.tweaks.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.RestartReason
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.back_cd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweaksSubScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    snackbarState: SnackbarHostState,
    restartReasons: Set<RestartReason>,
    onRestartNow: () -> Unit,
    onRestartLater: () -> Unit,
    showRestartBanner: Boolean,
    content: LazyListScope.() -> Unit,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_cd),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarState,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomNavHeight + 32.dp),
            ) {
            if (showRestartBanner && restartReasons.isNotEmpty()) {
                item(key = "restart_banner") {
                    RestartBanner(
                        reasons = restartReasons,
                        onRestartNow = onRestartNow,
                        onLater = onRestartLater,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
                content()
            }
        }
    }
}
