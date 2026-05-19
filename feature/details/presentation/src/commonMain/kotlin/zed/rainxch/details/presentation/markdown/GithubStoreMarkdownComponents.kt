package zed.rainxch.details.presentation.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

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
    image = { model -> LinkAwareMarkdownImage(model.content, model.node, imageTransformer) },
)

/**
 * Drop-in replacement for the lib's default `MarkdownImage` that also
 * supports the `[![alt](image-url)](href)` pattern — i.e. the image is a
 * clickable hyperlink. README badges (Maven Central status, build status,
 * sponsor buttons, "Get it on Play Store" / "Get it on F-Droid" tiles)
 * all use that pattern.
 *
 * The lib's stock renderer pulls the image's own `LINK_DESTINATION` (the
 * src) but ignores the outer INLINE_LINK that wraps it. We walk up via
 * `ASTNode.parent`; if an ancestor is an INLINE_LINK, we grab its
 * `LINK_DESTINATION` and apply `Modifier.clickable { openUri }` around
 * the rendered image.
 */
@Composable
private fun LinkAwareMarkdownImage(
    content: String,
    node: ASTNode,
    imageTransformer: ImageTransformer,
) {
    val imageSrc = findChildRecursive(node, MarkdownElementTypes.LINK_DESTINATION)
        ?.getUnescapedTextInNode(content)
        ?: return
    val outerHref = findEnclosingLinkDestination(node, content)
    val imageData = imageTransformer.transform(imageSrc) ?: return

    // Block-level images: GitHub-style sizing. Cap width to the
    // content column, let height flow naturally from the intrinsic
    // aspect ratio. No height cap — a tall screenshot renders at its
    // full proportional height and the user scrolls past it, matching
    // what github.com / `.markdown-body img { max-width: 100% }` does.
    // Inline rendering still uses the lib's `Placeholder` slot via
    // `MarkdownImageTransformer`.
    val blockModifier = androidx.compose.ui.Modifier.fillMaxWidth()

    if (outerHref != null) {
        val uriHandler = LocalUriHandler.current
        Image(
            painter = imageData.painter,
            contentDescription = imageData.contentDescription,
            modifier = blockModifier.clickable {
                runCatching { uriHandler.openUri(outerHref) }
            },
            alignment = imageData.alignment,
            contentScale = imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter,
        )
    } else {
        Image(
            painter = imageData.painter,
            contentDescription = imageData.contentDescription,
            modifier = blockModifier,
            alignment = imageData.alignment,
            contentScale = imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter,
        )
    }
}

private fun findChildRecursive(node: ASTNode, type: IElementType): ASTNode? {
    for (child in node.children) {
        if (child.type == type) return child
        val nested = findChildRecursive(child, type)
        if (nested != null) return nested
    }
    return null
}

private fun findEnclosingLinkDestination(imageNode: ASTNode, content: String): String? {
    var cursor: ASTNode? = imageNode.parent
    // Walk at most a few levels — INLINE_LINK is usually one or two
    // parents up. Bail before drifting into the surrounding paragraph.
    var depth = 0
    while (cursor != null && depth < 4) {
        if (cursor.type == MarkdownElementTypes.INLINE_LINK) {
            // The INLINE_LINK's direct LINK_DESTINATION child is the href.
            // Use the non-recursive child lookup so we don't accidentally
            // pick up the image's own (inner) LINK_DESTINATION.
            val destNode = cursor.children.firstOrNull {
                it.type == MarkdownElementTypes.LINK_DESTINATION
            } ?: return null
            return destNode.getUnescapedTextInNode(content)
        }
        cursor = cursor.parent
        depth++
    }
    return null
}
