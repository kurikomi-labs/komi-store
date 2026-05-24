package zed.rainxch.tweaks.presentation.licenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_title
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

private data class Library(
    val name: String,
    val license: String,
    val url: String,
)

private val LIBRARIES: List<Library> = listOf(
    Library("Kotlin", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    Library("Compose Multiplatform", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"),
    Library("Jetpack Compose", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    Library("Ktor", "Apache-2.0", "https://github.com/ktorio/ktor"),
    Library("Room", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    Library("Koin", "Apache-2.0", "https://github.com/InsertKoinIO/koin"),
    Library("kotlinx.serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    Library("kotlinx.coroutines", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    Library("kotlinx.datetime", "Apache-2.0", "https://github.com/Kotlin/kotlinx-datetime"),
    Library("DataStore", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/datastore"),
    Library("Landscapist", "Apache-2.0", "https://github.com/skydoves/landscapist"),
    Library("Kermit", "Apache-2.0", "https://github.com/touchlab/Kermit"),
    Library("MOKO Permissions", "Apache-2.0", "https://github.com/icerockdev/moko-permissions"),
    Library("Navigation Compose", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    Library("multiplatform-markdown-renderer", "Apache-2.0", "https://github.com/mikepenz/multiplatform-markdown-renderer"),
    Library("Shizuku", "Apache-2.0", "https://github.com/RikkaApps/Shizuku"),
    Library("WorkManager", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/work"),
    Library("KSafe", "Apache-2.0", "https://github.com/Anifantakis/KSafe"),
    Library("Geist (Vercel)", "OFL-1.1", "https://github.com/vercel/geist-font"),
    Library("Material Icons Extended", "Apache-2.0", "https://fonts.google.com/icons"),
)

@Composable
fun LicensesRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

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

        LIBRARIES.forEach { library ->
            item(key = "lib_${library.name}") {
                LibraryRow(library = library, onClick = {
                    runCatching { uriHandler.openUri(library.url) }
                })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LibraryRow(library: Library, onClick: () -> Unit) {
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
