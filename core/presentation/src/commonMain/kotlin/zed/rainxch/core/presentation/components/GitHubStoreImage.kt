package zed.rainxch.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.coil3.CoilImageState
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.crossfade.CrossfadePlugin
import kotlinx.coroutines.launch
import zed.rainxch.core.presentation.color.AvatarColorStore

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GitHubStoreImage(
    imageModel: () -> Any?,
    modifier: Modifier = Modifier,
    extractDominantFor: String? = null,
) {
    val scope = rememberCoroutineScope()
    CoilImage(
        imageModel = imageModel,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator()
            }
        },
        failure = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize(.5f),
                )
            }
        },
        onImageStateChanged = { state ->
            if (extractDominantFor != null && state is CoilImageState.Success) {
                val image = state.image
                if (image != null) {
                    scope.launch { AvatarColorStore.computeIfAbsent(extractDominantFor, image) }
                }
            }
        },
        component =
            rememberImageComponent {
                CrossfadePlugin()
            },
    )
}
