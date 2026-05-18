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
import zed.rainxch.details.presentation.utils.splitMarkdownIntoChunks
import zed.rainxch.details.presentation.utils.truncateMarkdownPreview
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
        val probeClient = org.koin.compose.koinInject<io.ktor.client.HttpClient>(
            qualifier = org.koin.core.qualifier.named("test"),
        )
        val imageTransformer = remember(probeClient) {
            MarkdownImageTransformer(probeClient)
        }

        ExpandableMarkdownContent(
            rawMarkdown = raw,
            isDark = isDark,
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded,
            imageTransformer = imageTransformer,
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
    // gate rendering on completion via `fullChunks` being non-null.
    //
    // We split the full body into ~4 000-char chunks (kept whole around
    // code fences). Chunk 0 doubles as the collapsed preview; subsequent
    // chunks stream in one frame at a time once the user taps Expand.
    // Crucially, chunk 0 stays mounted across collapse → expand, so the
    // transition is a height grow rather than a content swap — no flicker.
    var fullChunks by remember(rawMarkdown, isDark) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(rawMarkdown, isDark) {
        val processed = withContext(Dispatchers.Default) {
            applyThemeAwareImages(rawMarkdown, isDark)
        }
        val chunks = withContext(Dispatchers.Default) {
            splitMarkdownIntoChunks(processed, targetChunkChars = 4000)
        }
        fullChunks = chunks
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
                ProgressiveMarkdown(
                    isExpanded = isExpanded,
                    fullChunks = fullChunks,
                    collapsedHeight = collapsedHeight,
                    colors = colors,
                    typography = typography,
                    components = components,
                    flavour = flavour,
                    parser = parser,
                    imageTransformer = imageTransformer,
                    onMeasured = onMeasured,
                    effectiveHeight = effectiveHeight,
                    collapsedHeightPx = collapsedHeightPx,
                    rawKey = rawMarkdown,
                )
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

/**
 * Renders chunked markdown progressively. Chunk 0 is always mounted
 * (used as the collapsed-state preview, clipped by the parent's
 * `Modifier.height(collapsedHeight)` modifier). When `isExpanded` flips
 * true, subsequent chunks stream in one frame at a time without
 * unmounting / remounting chunk 0 — that stable identity is what kills
 * the previous expand-time flicker.
 *
 * Each chunk is its own `Markdown(...)` composable with its own
 * `MarkdownState` (parser runs on `Dispatchers.Default` per the
 * mikepenz lib). The "one per frame" cadence keeps composition cost
 * predictable instead of dropping the entire body's compose pass on
 * Main in a single 4-second hit (observed Davey on a kubernetes-sized
 * README before this change).
 */
@Composable
internal fun ProgressiveMarkdown(
    isExpanded: Boolean,
    fullChunks: List<String>?,
    collapsedHeight: Dp,
    colors: com.mikepenz.markdown.model.MarkdownColors,
    typography: com.mikepenz.markdown.model.MarkdownTypography,
    components: com.mikepenz.markdown.compose.components.MarkdownComponents,
    flavour: GFMFlavourDescriptor,
    parser: MarkdownParser,
    imageTransformer: ImageTransformer,
    onMeasured: (Float) -> Unit,
    effectiveHeight: Float,
    collapsedHeightPx: Float,
    rawKey: String,
) {
    val chunks = fullChunks
    if (chunks == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(collapsedHeight.takeIf { it > 0.dp } ?: 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        return
    }

    // Counter starts at 1 so chunk 0 (the natural preview) renders
    // immediately for the collapsed view. Once the user expands, the
    // remaining chunks stream in one frame at a time.
    var renderedCount by remember(rawKey) { mutableStateOf(1) }
    LaunchedEffect(rawKey, isExpanded, chunks.size) {
        if (!isExpanded) return@LaunchedEffect
        while (renderedCount < chunks.size) {
            // Yield to the frame so the previously-added chunk has a chance
            // to compose + layout + draw before the next chunk arrives.
            kotlinx.coroutines.yield()
            renderedCount++
        }
    }

    var lastReportedPx by remember(rawKey) { mutableStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                val measured = size.height.toFloat()
                val decisive = effectiveHeight > collapsedHeightPx
                if (decisive) return@onSizeChanged
                if (abs(measured - lastReportedPx) < 1f) return@onSizeChanged
                lastReportedPx = measured
                if (measured > effectiveHeight) onMeasured(measured)
            },
    ) {
        val visible = chunks.take(renderedCount.coerceAtMost(chunks.size))
        visible.forEachIndexed { index, chunk ->
            androidx.compose.runtime.key(rawKey, index) {
                val markdownState = rememberMarkdownState(
                    content = chunk,
                    flavour = flavour,
                    parser = parser,
                    retainState = true,
                )
                Markdown(
                    markdownState = markdownState,
                    colors = colors,
                    typography = typography,
                    imageTransformer = imageTransformer,
                    components = components,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (isExpanded && renderedCount < chunks.size) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }
    }
}
