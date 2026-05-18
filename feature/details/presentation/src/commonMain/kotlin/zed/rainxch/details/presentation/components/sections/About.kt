package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.util.applyThemeAwareImages
import zed.rainxch.details.presentation.components.TranslationControls
import zed.rainxch.details.presentation.markdown.githubStoreMarkdownComponents
import zed.rainxch.details.presentation.model.TranslationState
import zed.rainxch.details.presentation.utils.MarkdownImageTransformer
import zed.rainxch.details.presentation.utils.rememberMarkdownColors
import zed.rainxch.details.presentation.utils.rememberMarkdownTypography
import zed.rainxch.githubstore.core.presentation.res.*

fun LazyListScope.about(
    readmeMarkdown: String,
    readmeLanguage: String?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    collapsedHeight: Dp,
    measuredHeightPx: Float?,
    onMeasured: (Float) -> Unit,
    translationState: TranslationState,
    onTranslateClick: () -> Unit,
    onLanguagePickerClick: () -> Unit,
    onToggleTranslation: () -> Unit,
) {
    item {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.about_this_app),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )

                readmeLanguage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            TranslationControls(
                translationState = translationState,
                onTranslateClick = onTranslateClick,
                onLanguagePickerClick = onLanguagePickerClick,
                onToggleTranslation = onToggleTranslation,
            )
        }
    }

    item(key = "about_markdown") {
        val raw =
            if (translationState.isShowingTranslation && translationState.translatedText != null) {
                translationState.translatedText
            } else {
                readmeMarkdown
            }
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()

        ExpandableMarkdownContent(
            rawMarkdown = raw,
            isDark = isDark,
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded,
            imageTransformer = MarkdownImageTransformer,
            collapsedHeight = collapsedHeight,
            measuredHeightPx = measuredHeightPx,
            onMeasured = onMeasured,
            fadeColor = MaterialTheme.colorScheme.background,
            modifier =
                Modifier
                    .fillMaxWidth(),
        )
    }
}

@Composable
fun ExpandableMarkdownContent(
    rawMarkdown: String,
    isDark: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    imageTransformer: ImageTransformer,
    collapsedHeight: Dp,
    measuredHeightPx: Float?,
    onMeasured: (Float) -> Unit,
    fadeColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()

    // Pre-process markdown off the main thread. The theme-aware image rewrite
    // is regex-heavy; running it inside `remember { ... }` happened on the
    // composition thread (typically Main) and contributed to the visible
    // freeze on first render and theme toggle. We launch it on Default and
    // gate rendering on completion via `displayContent` being non-null.
    var displayContent by remember(rawMarkdown, isDark) { mutableStateOf<String?>(null) }
    LaunchedEffect(rawMarkdown, isDark) {
        val processed = withContext(Dispatchers.Default) {
            applyThemeAwareImages(rawMarkdown, isDark)
        }
        displayContent = processed
    }

    // Parser + flavour are heavy to construct and identical across recompositions;
    // hoist them once so they survive content / theme / scroll churn. The Markdown
    // lib parses lazily on Dispatchers.Default once handed a fresh MarkdownState.
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    val components = remember(isDark, imageTransformer) {
        githubStoreMarkdownComponents(imageTransformer, isDark)
    }

    val collapsedHeightPx = with(density) { collapsedHeight.toPx() }
    val effectiveHeight = measuredHeightPx ?: 0f
    val needsExpansion = effectiveHeight > collapsedHeightPx && collapsedHeightPx > 0f
    val measuredDp =
        measuredHeightPx?.let { with(density) { it.toDp() } }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester),
    ) {
        Box {
            Surface(
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier =
                    when {
                        !isExpanded && needsExpansion ->
                            Modifier
                                .height(collapsedHeight)
                                .clipToBounds()
                        isExpanded && measuredDp != null ->
                            Modifier.heightIn(min = measuredDp)
                        else -> Modifier
                    },
            ) {
                val content = displayContent
                if (content == null) {
                    // Pre-processing in flight. Show a small spinner clamped
                    // to the collapsed-section height so the scroll position
                    // doesn't jump once content lands.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(collapsedHeight.takeIf { it > 0.dp } ?: 120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else {
                    // `retainState = true` keeps the previous parsed AST on
                    // screen while the next parse runs in the background —
                    // no flash to a blank Loading state on theme toggle or
                    // minor edits.
                    val markdownState = rememberMarkdownState(
                        content = content,
                        flavour = flavour,
                        parser = parser,
                        retainState = true,
                    )
                    // Local-only measure latch — write the size up to the
                    // hoisted VM state once it has stabilised (within 1px of
                    // the previous local value), instead of forwarding every
                    // onSizeChanged tick. Each forwarded write recopies the
                    // full DetailsState; this damps that churn.
                    var lastReportedPx by remember(rawMarkdown) { mutableStateOf(0f) }
                    Markdown(
                        markdownState = markdownState,
                        colors = colors,
                        typography = typography,
                        imageTransformer = imageTransformer,
                        components = components,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    val measured = size.height.toFloat()
                                    val decisive = effectiveHeight > collapsedHeightPx
                                    if (decisive) return@onSizeChanged
                                    if (abs(measured - lastReportedPx) < 1f) return@onSizeChanged
                                    lastReportedPx = measured
                                    if (measured > effectiveHeight) {
                                        onMeasured(measured)
                                    }
                                },
                    )
                }
            }

            if (!isExpanded && needsExpansion) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to fadeColor.copy(alpha = 0f),
                                    1f to fadeColor,
                                ),
                            ),
                )
            }
        }

        if (needsExpansion) {
            TextButton(
                onClick = onToggleExpanded,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text =
                        if (isExpanded) {
                            stringResource(Res.string.show_less)
                        } else {
                            stringResource(Res.string.read_more)
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
