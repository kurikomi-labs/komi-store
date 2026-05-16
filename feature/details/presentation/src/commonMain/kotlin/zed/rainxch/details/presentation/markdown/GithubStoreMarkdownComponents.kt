package zed.rainxch.details.presentation.markdown

import androidx.compose.runtime.Composable
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.ImageTransformer

@Composable
fun githubStoreMarkdownComponents(
    imageTransformer: ImageTransformer,
): MarkdownComponents = markdownComponents(
    blockQuote = { model ->
        AlertBlockQuote(
            model = model,
            imageTransformer = imageTransformer,
            fallback = { defaultBlockQuoteFallback(model) },
        )
    },
)
