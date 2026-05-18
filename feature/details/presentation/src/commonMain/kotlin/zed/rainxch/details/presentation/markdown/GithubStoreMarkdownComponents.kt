package zed.rainxch.details.presentation.markdown

import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.ImageTransformer
import org.intellij.markdown.MarkdownTokenTypes

// Plain (non-@Composable) factory so callers can wrap in `remember(isDark)`
// — the wrapped MarkdownComponents holds lambdas that get invoked inside the
// Markdown render scope, but constructing the wrapper has no composition cost.
fun githubStoreMarkdownComponents(
    imageTransformer: ImageTransformer,
    isDark: Boolean,
): MarkdownComponents = markdownComponents(
    blockQuote = { model ->
        AlertBlockQuote(
            model = model,
            imageTransformer = imageTransformer,
            fallback = { defaultBlockQuoteFallback(model) },
        )
    },
    codeFence = { model ->
        val lang = model.node.children
            .firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
            ?.let { model.content.substring(it.startOffset, it.endOffset).trim() }
            .orEmpty()
        val detailsSummary = infoStringForDetails(lang)
        if (detailsSummary != null) {
            ExpandableDetails(
                model = model,
                encodedSummary = detailsSummary,
                imageTransformer = imageTransformer,
            )
        } else {
            SyntaxHighlightedCode(model, isDark)
        }
    },
)
