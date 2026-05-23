package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.vocabulary.Squiggle
import kotlinx.coroutines.withContext
import org.intellij.markdown.parser.MarkdownParser
import com.mikepenz.markdown.model.rememberMarkdownState
import zed.rainxch.core.domain.util.applyThemeAwareImages
import zed.rainxch.details.presentation.markdown.githubStoreMarkdownComponents
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.details.presentation.utils.MarkdownImageTransformer
import zed.rainxch.details.presentation.utils.rememberMarkdownColors
import zed.rainxch.details.presentation.utils.rememberMarkdownTypography
import zed.rainxch.githubstore.core.presentation.res.*

fun LazyListScope.whatsNew(
    release: GithubRelease,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    collapsedHeight: Dp,
    measuredHeightPx: Float?,
    onMeasured: (Float) -> Unit,
    onReadMore: (() -> Unit)? = null,
) {
    item {
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(Res.string.whats_new),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Squiggle()
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = Radii.row,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = release.tagName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = release.publishedAt.take(10),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }

    item(key = "whats_new_markdown") {
        Spacer(Modifier.height(12.dp))

        ExpandableMarkdownContent(
            release = release,
            collapsedHeight = collapsedHeight,
            isExpanded = isExpanded,
            onToggleExpanded = onReadMore ?: onToggleExpanded,
            measuredHeightPx = measuredHeightPx,
            onMeasured = onMeasured,
        )
    }
}

@Composable
private fun ExpandableMarkdownContent(
    release: GithubRelease,
    collapsedHeight: Dp,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    measuredHeightPx: Float?,
    onMeasured: (Float) -> Unit,
) {
    val raw = release.description ?: stringResource(Res.string.no_release_notes)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    var fullChunks by remember(raw, isDark) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(raw, isDark) {
        val processed = withContext(Dispatchers.Default) {
            val themed = applyThemeAwareImages(raw, isDark)
            zed.rainxch.core.domain.util.separateAdjacentImageLinks(themed)
        }
        val chunks = withContext(Dispatchers.Default) {
            zed.rainxch.details.presentation.utils
                .splitMarkdownIntoChunks(processed, targetChunkChars = 4000)
        }
        fullChunks = chunks
    }

    val density = LocalDensity.current
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    val probeClient = org.koin.compose.koinInject<io.ktor.client.HttpClient>(
        qualifier = org.koin.core.qualifier.named("test"),
    )
    val imageTransformer = remember(probeClient) {
        MarkdownImageTransformer(probeClient)
    }
    val components = remember(isDark, imageTransformer) {
        githubStoreMarkdownComponents(imageTransformer, isDark)
    }
    val cardColor = MaterialTheme.colorScheme.background

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

    Column(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
        Box {
            Box(
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
                    rawKey = raw,
                )
            }

            if (!isExpanded && needsExpansion) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to cardColor.copy(alpha = 0f),
                                    0.35f to cardColor.copy(alpha = 0.10f),
                                    0.6f to cardColor.copy(alpha = 0.35f),
                                    0.8f to cardColor.copy(alpha = 0.7f),
                                    1f to cardColor,
                                ),
                            ),
                        ),
                )
            }
        }

        if (needsExpansion) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface)
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isExpanded) {
                        stringResource(Res.string.show_less)
                    } else {
                        stringResource(Res.string.read_more)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.surface,
                )
                if (!isExpanded) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
