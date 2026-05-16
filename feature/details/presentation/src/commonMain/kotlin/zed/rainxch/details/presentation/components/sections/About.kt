package zed.rainxch.details.presentation.components.sections

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.ImageTransformer
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.components.TranslationControls
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

    item {
        val displayContent =
            if (translationState.isShowingTranslation && translationState.translatedText != null) {
                translationState.translatedText
            } else {
                readmeMarkdown
            }

        AnimatedContent(
            targetState = displayContent,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "about_content",
        ) { content ->
            ExpandableMarkdownContent(
                content = content,
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded,
                imageTransformer = MarkdownImageTransformer,
                collapsedHeight = collapsedHeight,
                fadeColor = MaterialTheme.colorScheme.background,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
            )
        }
    }
}

@Composable
fun ExpandableMarkdownContent(
    content: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    imageTransformer: ImageTransformer,
    collapsedHeight: Dp,
    fadeColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val flavour = remember { GFMFlavourDescriptor() }

    val collapsedHeightPx = with(density) { collapsedHeight.toPx() }
    var contentHeightPx by remember(content, collapsedHeightPx) { mutableStateOf(0f) }
    val needsExpansion = contentHeightPx > collapsedHeightPx && collapsedHeightPx > 0f

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        Box {
            Surface(
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier =
                    if (!isExpanded && needsExpansion) {
                        Modifier.heightIn(max = collapsedHeight).clipToBounds()
                    } else {
                        Modifier
                    },
            ) {
                Markdown(
                    content = content,
                    colors = colors,
                    typography = typography,
                    flavour = flavour,
                    imageTransformer = imageTransformer,
                    components = zed.rainxch.details.presentation.markdown
                        .githubStoreMarkdownComponents(imageTransformer),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                val measured = coordinates.size.height.toFloat()
                                if (measured > contentHeightPx) {
                                    contentHeightPx = measured
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
