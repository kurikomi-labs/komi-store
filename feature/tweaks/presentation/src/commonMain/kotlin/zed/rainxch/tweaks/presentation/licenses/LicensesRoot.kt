package zed.rainxch.tweaks.presentation.licenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_load_failed
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_title
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

@Serializable
private data class LibraryEntry(
    val name: String,
    val license: String,
    val url: String,
)

@Serializable
private data class LicensesPayload(
    val libraries: List<LibraryEntry> = emptyList(),
)

private val licensesJson = Json { ignoreUnknownKeys = true }

@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
fun LicensesRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    var libraries by remember { mutableStateOf<List<LibraryEntry>?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            val bytes = Res.readBytes("files/licenses.json")
            licensesJson.decodeFromString(
                LicensesPayload.serializer(),
                bytes.decodeToString(),
            ).libraries
        }.onSuccess { libraries = it }
            .onFailure { loadError = true }
    }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_licenses_title),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        item(key = "intro") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Radii.row,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.tweaks_licenses_intro_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.tweaks_licenses_intro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val list = libraries
        when {
            list == null && !loadError -> {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            loadError -> {
                item(key = "error") {
                    Text(
                        text = stringResource(Res.string.tweaks_licenses_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            list != null -> {
                list.forEach { library ->
                    item(key = "lib_${library.name}") {
                        LibraryRow(library = library, onClick = {
                            runCatching { uriHandler.openUri(library.url) }
                        })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(library: LibraryEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = library.license,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
