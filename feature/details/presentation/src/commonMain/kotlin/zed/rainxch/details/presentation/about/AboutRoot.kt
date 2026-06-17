package zed.rainxch.details.presentation.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import zed.rainxch.core.presentation.components.markdown.MarkdownImageTransformer
import zed.rainxch.core.presentation.components.markdown.githubStoreMarkdownComponents
import zed.rainxch.core.presentation.components.markdown.rememberMarkdownColors
import zed.rainxch.core.presentation.components.markdown.rememberMarkdownTypography
import zed.rainxch.core.presentation.vocabulary.Squiggle
import zed.rainxch.details.presentation.components.LanguagePicker
import zed.rainxch.details.presentation.components.TranslationCard
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cd_back
import zed.rainxch.githubstore.core.presentation.res.details_about_screen_title
import zed.rainxch.githubstore.core.presentation.res.retry

@Composable
fun AboutRoot(
    repositoryId: Long,
    owner: String,
    repo: String,
    sourceHost: String?,
    onNavigateBack: () -> Unit,
    viewModel: DetailsAboutViewModel = koinViewModel {
        parametersOf(repositoryId, owner, repo, sourceHost)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutScreen(
        state = state,
        onBack = onNavigateBack,
        onRetry = viewModel::retry,
        onTranslate = viewModel::translate,
        onToggleTranslation = viewModel::toggleTranslation,
        onPickLanguage = viewModel::showLanguagePicker,
        onDismissLanguagePicker = viewModel::dismissLanguagePicker,
        onClearTranslation = viewModel::clearTranslation,
    )
}

@Composable
private fun AboutScreen(
    state: DetailsAboutState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTranslate: (String) -> Unit,
    onToggleTranslation: () -> Unit,
    onPickLanguage: () -> Unit,
    onDismissLanguagePicker: () -> Unit,
    onClearTranslation: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val probeClient = koinInject<HttpClient>(qualifier = named("test"))
    val imageTransformer = remember(probeClient) { MarkdownImageTransformer(probeClient) }
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val components = remember(isDark, imageTransformer) {
        githubStoreMarkdownComponents(imageTransformer, isDark)
    }

    val displayedMarkdown = if (
        state.translation.isShowingTranslation && state.translation.translatedText != null
    ) {
        state.translation.translatedText
    } else {
        state.readmeMarkdown
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        AboutTopBar(title = state.repoName, onBack = onBack)
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(Res.string.retry),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(Res.string.details_about_screen_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 26.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Squiggle()
                        state.readmeLanguage?.let { lang ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item(key = "translation_card") {
                    Spacer(Modifier.height(4.dp))
                    TranslationCard(
                        state = state.translation,
                        deviceLanguageCode = state.deviceLanguageCode,
                        onPickLanguage = onPickLanguage,
                        onTranslate = onTranslate,
                        onToggle = onToggleTranslation,
                        onCancel = onClearTranslation,
                    )
                }
                item(key = "markdown") {
                    Spacer(Modifier.height(4.dp))
                    val defaultUriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val customUriHandler = remember(defaultUriHandler, onTranslate) {
                        object : androidx.compose.ui.platform.UriHandler {
                            override fun openUri(uri: String) {
                                val match = Regex("""(?i)README[-._]([a-z]{2}(?:-[a-z]{2})?)\.md$""").find(uri)
                                if (match != null) {
                                    val code = match.groupValues[1]
                                    val normalizedCode = when (code.lowercase()) {
                                        "cn" -> "zh-CN"
                                        "tw" -> "zh-TW"
                                        "jp" -> "ja"
                                        "kr" -> "ko"
                                        "br" -> "pt-BR"
                                        else -> code
                                    }
                                    val matchedLanguage = zed.rainxch.details.presentation.model.SupportedLanguages.all.find { 
                                        it.code.equals(normalizedCode, ignoreCase = true) 
                                    } ?: zed.rainxch.details.presentation.model.SupportedLanguages.all.find { 
                                        it.code.startsWith(normalizedCode, ignoreCase = true) || normalizedCode.startsWith(it.code, ignoreCase = true)
                                    }
                                    if (matchedLanguage != null) {
                                        onTranslate(matchedLanguage.code)
                                        return
                                    }
                                }
                                defaultUriHandler.openUri(uri)
                            }
                        }
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalUriHandler provides customUriHandler,
                    ) {
                        Markdown(
                            content = displayedMarkdown,
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

    LanguagePicker(
        isVisible = state.isLanguagePickerVisible,
        selectedLanguageCode = state.translation.targetLanguageCode ?: state.deviceLanguageCode,
        deviceLanguageCode = state.deviceLanguageCode,
        onLanguageSelected = { lang ->
            onDismissLanguagePicker()
            onTranslate(lang.code)
        },
        onDismiss = onDismissLanguagePicker,
    )
}

@Composable
private fun AboutTopBar(title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(
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
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
