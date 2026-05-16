package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.details.presentation.components.TranslationControls
import zed.rainxch.details.presentation.model.TranslationState
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
            Text(
                text = stringResource(Res.string.whats_new),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            TranslationControls(
                translationState = translationState,
                onTranslateClick = onTranslateClick,
                onLanguagePickerClick = onLanguagePickerClick,
                onToggleTranslation = onToggleTranslation,
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        release.tagName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        release.publishedAt.take(10),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    item(key = "whats_new_markdown") {
        Spacer(Modifier.height(12.dp))

        ExpandableMarkdownContent(
            translationState = translationState,
            release = release,
            collapsedHeight = collapsedHeight,
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded,
            measuredHeightPx = measuredHeightPx,
            onMeasured = onMeasured,
        )
    }
}

@Composable
private fun ExpandableMarkdownContent(
    translationState: TranslationState,
    release: GithubRelease,
    collapsedHeight: Dp,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    measuredHeightPx: Float?,
    onMeasured: (Float) -> Unit,
) {
    val raw =
        if (translationState.isShowingTranslation && translationState.translatedText != null) {
            translationState.translatedText
        } else {
            release.description ?: stringResource(Res.string.no_release_notes)
        }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val displayContent =
        remember(raw, isDark) {
            zed.rainxch.core.domain.util.applyThemeAwareImages(raw, isDark)
        }

    val density = LocalDensity.current
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val flavour = remember { GFMFlavourDescriptor() }
    val cardColor = MaterialTheme.colorScheme.surfaceContainerLow

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
                Markdown(
                    content = displayContent,
                    colors = colors,
                    typography = typography,
                    flavour = flavour,
                    imageTransformer = MarkdownImageTransformer,
                    components = zed.rainxch.details.presentation.markdown
                        .githubStoreMarkdownComponents(MarkdownImageTransformer, isDark),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                val measured = size.height.toFloat()
                                val decisive = effectiveHeight > collapsedHeightPx
                                if (!decisive && measured > effectiveHeight) {
                                    onMeasured(measured)
                                }
                            },
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
                                    0f to cardColor.copy(alpha = 0f),
                                    1f to cardColor,
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
