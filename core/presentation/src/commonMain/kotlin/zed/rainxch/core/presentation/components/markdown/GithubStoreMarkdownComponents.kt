package zed.rainxch.core.presentation.components.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

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

    val blockModifier = Modifier.fillMaxWidth()

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

    var depth = 0
    while (cursor != null && depth < 4) {
        if (cursor.type == MarkdownElementTypes.INLINE_LINK) {

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
