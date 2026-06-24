package zed.rainxch.details.presentation.markdownviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.compose.Markdown
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.components.markdown.MarkdownImageTransformer
import zed.rainxch.core.presentation.components.markdown.githubStoreMarkdownComponents
import zed.rainxch.core.presentation.components.markdown.rememberMarkdownColors
import zed.rainxch.core.presentation.components.markdown.rememberMarkdownTypography
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.details.presentation.components.LanguagePicker
import zed.rainxch.details.presentation.components.TranslationCard
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cd_back
import zed.rainxch.githubstore.core.presentation.res.retry

@Composable
fun MarkdownViewerRoot(
    onNavigateBack: () -> Unit,
    onNavigateToMarkdownViewer: (String) -> Unit,
    viewModel: MarkdownViewerViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MarkdownViewerScreen(
        state = state,
        onBack = onNavigateBack,
        onRetry = viewModel::retry,
        onTranslate = viewModel::translate,
        onToggleTranslation = viewModel::toggleTranslation,
        onPickLanguage = viewModel::showLanguagePicker,
        onDismissLanguagePicker = viewModel::dismissLanguagePicker,
        onClearTranslation = viewModel::clearTranslation,
        onNavigateToMarkdownViewer = onNavigateToMarkdownViewer,
    )
}

@Composable
private fun MarkdownViewerScreen(
    state: MarkdownViewerState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTranslate: (String) -> Unit,
    onToggleTranslation: () -> Unit,
    onPickLanguage: () -> Unit,
    onDismissLanguagePicker: () -> Unit,
    onClearTranslation: () -> Unit,
    onNavigateToMarkdownViewer: (String) -> Unit,
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
        state.markdown
    }

    var isReadyToRender by remember { mutableStateOf(false) }
    LaunchedEffect(displayedMarkdown) {
        if (displayedMarkdown.isNotEmpty()) {
            delay(350) // Let the slide-in animation finish smoothly
            isReadyToRender = true
        } else {
            isReadyToRender = false
        }
    }

    val markdownChunks = remember(displayedMarkdown) {
        if (displayedMarkdown.isEmpty()) return@remember emptyList<String>()
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(displayedMarkdown)
        parsedTree.children.map { node ->
            displayedMarkdown.substring(node.startOffset, node.endOffset)
        }.filter { it.isNotBlank() }
    }

    val filename = remember(state.url) {
        state.url.substringAfterLast("/", "Document").substringBefore("?")
    }

    KomiScaffold (
        topBar = {
            KomiTopBar(
                leading = {
                    KomiIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.cd_back),
                    )
                },
                title = filename
            )
        }
    ) {

    }

    val personalityColors = LocalPersonality.current.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(personalityColors.background)
            .systemBarsPadding(),
    ) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { KomiCircularProgress() }

            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KomiText(
                        text = state.errorMessage,
                        role = KomiTextRole.Body,
                        color = personalityColors.error,
                    )
                    Spacer(Modifier.size(8.dp))
                    KomiText(
                        text = stringResource(Res.string.retry),
                        role = KomiTextRole.Label,
                        color = personalityColors.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(personalityColors.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { onRetry() },
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "translation_card") {
                    TranslationCard(
                        state = state.translation,
                        deviceLanguageCode = state.deviceLanguageCode,
                        onPickLanguage = onPickLanguage,
                        onTranslate = onTranslate,
                        onToggle = onToggleTranslation,
                        onCancel = onClearTranslation,
                    )
                }
                if (isReadyToRender) {
                    items(
                        items = markdownChunks,
                    ) { chunk ->
                        zed.rainxch.details.presentation.utils.ProvideLanguageLinkInterceptor(
                            onTranslate = onTranslate,
                            onClearTranslation = onClearTranslation,
                            onOpenInternalMarkdown = onNavigateToMarkdownViewer,
                        ) {
                            Markdown(
                                content = chunk,
                                colors = colors,
                                typography = typography,
                                imageTransformer = imageTransformer,
                                components = components,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            )
                        }
                    }
                } else {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            KomiCircularProgress(modifier = Modifier.size(32.dp))
                        }
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