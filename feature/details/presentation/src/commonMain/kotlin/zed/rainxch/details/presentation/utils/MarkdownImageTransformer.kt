package zed.rainxch.details.presentation.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer

object MarkdownImageTransformer : ImageTransformer {
    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Mobile Safari/537.36 GitHubStore/1.8"

    private val networkHeaders =
        NetworkHeaders.Builder()
            .add("User-Agent", BROWSER_UA)
            .add(
                "Accept",
                "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
            )
            .build()

    @Composable
    override fun transform(link: String): ImageData? {
        if (link.isBlank()) return null

        val normalizedLink =
            if (link.contains("github.com") && link.contains("/blob/")) {
                link
                    .replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            } else {
                link
            }

        if (!normalizedLink.startsWith("http://") &&
            !normalizedLink.startsWith("https://") &&
            !normalizedLink.startsWith("data:")
        ) {
            return null
        }

        val context = LocalPlatformContext.current
        val request =
            ImageRequest.Builder(context)
                .data(normalizedLink)
                .httpHeaders(networkHeaders)
                .build()

        val painter = rememberAsyncImagePainter(model = request)

        return ImageData(
            painter = painter,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Image",
            contentScale = ContentScale.Fit,
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size = painter.intrinsicSize
}
