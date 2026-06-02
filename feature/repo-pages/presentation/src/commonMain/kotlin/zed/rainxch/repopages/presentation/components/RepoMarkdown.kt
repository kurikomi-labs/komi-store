package zed.rainxch.repopages.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.compose.Markdown
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import zed.rainxch.details.presentation.markdown.githubStoreMarkdownComponents
import zed.rainxch.details.presentation.utils.MarkdownImageTransformer
import zed.rainxch.details.presentation.utils.rememberMarkdownColors
import zed.rainxch.details.presentation.utils.rememberMarkdownTypography

@Composable
fun RepoMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val probeClient = koinInject<HttpClient>(qualifier = named("test"))
    val imageTransformer = remember(probeClient) { MarkdownImageTransformer(probeClient) }
    val colors = rememberMarkdownColors()
    val typography = rememberMarkdownTypography()
    val components = remember(isDark, imageTransformer) {
        githubStoreMarkdownComponents(imageTransformer, isDark)
    }
    Markdown(
        content = content,
        colors = colors,
        typography = typography,
        imageTransformer = imageTransformer,
        components = components,
        modifier = modifier,
    )
}