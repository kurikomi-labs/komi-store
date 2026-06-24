package zed.rainxch.tweaks.presentation.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.surfaces.KomiSurface
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.navigate_back
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_intro_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_load_failed
import zed.rainxch.githubstore.core.presentation.res.tweaks_licenses_title
import zed.rainxch.tweaks.presentation.TweaksViewModel

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
    val toastState = rememberKomiToastState()
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

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.tweaks_licenses_title),
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateBack,
                    )
                },
            )
        },
        toastState = toastState,
        grid = true,
        screentone = true
    ) {
        val colors = LocalPersonality.current.colors
        KomiSurface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                KomiText(
                    text = stringResource(Res.string.tweaks_licenses_intro_title),
                    role = KomiTextRole.Body,
                    color = colors.onSurface,
                    uppercase = false,
                )
                Spacer(Modifier.height(4.dp))
                KomiText(
                    text = stringResource(Res.string.tweaks_licenses_intro_body),
                    role = KomiTextRole.Body,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    uppercase = false,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            libraries == null && !loadError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KomiCircularProgress()
                }
            }

            loadError -> {
                KomiText(
                    text = stringResource(Res.string.tweaks_licenses_load_failed),
                    role = KomiTextRole.Body,
                    color = LocalPersonality.current.colors.error,
                    uppercase = false,
                    modifier = Modifier.padding(16.dp),
                )
            }

            libraries != null -> {
                libraries?.let { libraryEntries ->
                    libraryEntries.forEach { library ->
                        LibraryRow(
                            library = library,
                            onClick = {
                                runCatching { uriHandler.openUri(library.url) }
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(library: LibraryEntry, onClick: () -> Unit) {
    val colors = LocalPersonality.current.colors
    KomiSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            KomiText(
                text = library.name,
                role = KomiTextRole.Title,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                uppercase = false,
            )
            KomiText(
                text = library.license,
                role = KomiTextRole.Mono,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                uppercase = false,
            )
        }
    }
}
