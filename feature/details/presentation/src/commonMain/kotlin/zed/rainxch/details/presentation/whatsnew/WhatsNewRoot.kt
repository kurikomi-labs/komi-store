package zed.rainxch.details.presentation.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.compose.Markdown
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import zed.rainxch.core.presentation.components.buttons.IconButton
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.vocabulary.Squiggle
import zed.rainxch.details.presentation.markdown.githubStoreMarkdownComponents
import zed.rainxch.details.presentation.utils.MarkdownImageTransformer
import zed.rainxch.details.presentation.utils.rememberMarkdownColors
import zed.rainxch.details.presentation.utils.rememberMarkdownTypography
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cd_back
import zed.rainxch.githubstore.core.presentation.res.details_whats_new_screen_title
import zed.rainxch.githubstore.core.presentation.res.no_release_notes

@Composable
fun WhatsNewRoot(
    repositoryId: Long,
    owner: String,
    repo: String,
    sourceHost: String?,
    onNavigateBack: () -> Unit,
    viewModel: DetailsWhatsNewViewModel = koinViewModel {
        parametersOf(repositoryId, owner, repo, sourceHost)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WhatsNewScreen(
        state = state,
        onBack = onNavigateBack,
    )
}

@Composable
private fun WhatsNewScreen(
    state: DetailsWhatsNewState,
    onBack: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val probeClient = koinInject<HttpClient>(qualifier = named("test"))
    val imageTransformer = remember(probeClient) { MarkdownImageTransformer(probeClient) }
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val components = remember(isDark, imageTransformer) {
        githubStoreMarkdownComponents(imageTransformer, isDark)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = state.repoName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(Res.string.details_whats_new_screen_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 26.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Squiggle()
                    }
                }

                items(items = state.releases, key = { it.id }) { release ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radii.row)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = Radii.row,
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = release.publishedAt.take(10),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        val body = release.description?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.no_release_notes)
                        Markdown(
                            content = body,
                            colors = colors,
                            typography = typography,
                            imageTransformer = imageTransformer,
                            components = components,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
