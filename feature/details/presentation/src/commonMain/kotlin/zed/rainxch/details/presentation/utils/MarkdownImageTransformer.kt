package zed.rainxch.details.presentation.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Skips rendering of images whose advertised `Content-Length` exceeds
 * [MAX_IMAGE_BYTES]. A HEAD probe runs once per URL on `Dispatchers.IO`
 * via the injected [probeClient] (re-used: the same client used for
 * proxy testing in `core.data.di.SharedModule` under the `test`
 * qualifier — short request timeouts, no extra interceptors). Results
 * are cached process-wide so a README that references the same big
 * image twice still probes once.
 *
 * Decoded-bitmap dimension is also capped at [MAX_BITMAP_DIMENSION_PX]
 * so a 4K source image (Content-Length under the byte threshold) is
 * still rescaled before reaching the GPU.
 *
 * Servers that omit `Content-Length` (chunked transfer, mis-configured
 * CDNs) are treated as "size unknown" and rendered normally — the byte
 * cap is best-effort, not a guarantee.
 */
class MarkdownImageTransformer(
    private val probeClient: HttpClient,
) : ImageTransformer {

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

        val isDataUri = normalizedLink.startsWith("data:")

        var probeResult by remember(normalizedLink) {
            mutableStateOf<ProbeResult>(probeCache[normalizedLink] ?: ProbeResult.Pending)
        }
        LaunchedEffect(normalizedLink) {
            if (isDataUri) {
                probeResult = ProbeResult.Allowed
                return@LaunchedEffect
            }
            val cached = probeCache[normalizedLink]
            if (cached != null) {
                probeResult = cached
                return@LaunchedEffect
            }
            val result = withContext(Dispatchers.IO) {
                probeOnce(normalizedLink)
            }
            probeCache[normalizedLink] = result
            probeResult = result
        }

        // Skip rendering entirely when the advertised payload would have
        // burned bandwidth + decode time. Returning `null` makes the
        // markdown renderer omit the image element; alt text (if any)
        // remains visible in the surrounding paragraph.
        if (probeResult is ProbeResult.Skipped) return null

        val context = LocalPlatformContext.current
        val request =
            ImageRequest.Builder(context)
                .data(normalizedLink)
                .httpHeaders(networkHeaders)
                .size(MAX_BITMAP_DIMENSION_PX)
                .memoryCacheKey(normalizedLink)
                .diskCacheKey(normalizedLink)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(150)
                .build()

        val painter = rememberAsyncImagePainter(model = request)

        return ImageData(
            painter = painter,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            contentDescription = "Image",
            contentScale = ContentScale.Fit,
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size = painter.intrinsicSize

    /**
     * Override the lib's per-paragraph placeholder size so badge rows
     * (the typical `[![…](svg)](url) [![…](svg)](url)` stack in a
     * README header) don't all inherit the FIRST image's placeholder
     * dimensions — which is what produces the observed overlap when the
     * first image is an SVG with no intrinsic size (`Size(0, 180)`
     * defaults from the lib) and every following badge takes a 180sp
     * tall slot in the same line.
     *
     * Strategy: when the painter has not yet reported an intrinsic
     * size, allocate a badge-sized slot (32sp tall, 120sp wide) instead
     * of the lib's 180×180 default. That tiles cleanly horizontally on
     * a single line. Once Coil decodes and the painter reports a real
     * `intrinsicImageSize`, we scale into the container width with the
     * height capped at [MAX_INLINE_HEIGHT_PX] (240 px) so a stray hero
     * image doesn't dominate. Container width of `0f` (first
     * composition before the layout pass) is treated the same as
     * unspecified — otherwise the placeholder collapses to zero size,
     * the image is invisible on first frame, and a second
     * recomposition is forced once layout reports a real width.
     */
    override fun placeholderConfig(
        density: androidx.compose.ui.unit.Density,
        containerSize: Size,
        intrinsicImageSize: Size,
    ): com.mikepenz.markdown.model.PlaceholderConfig {
        val (widthSp, heightSp) = when {
            intrinsicImageSize.isUnspecified ||
                intrinsicImageSize.width <= 0f ||
                intrinsicImageSize.height <= 0f ->
                BADGE_DEFAULT_WIDTH_SP to BADGE_DEFAULT_HEIGHT_SP
            else -> with(density) {
                // Treat a zero container width the same as unspecified.
                // Compose can hand us `containerSize.width == 0f` on the
                // first composition before the surrounding paragraph
                // has been measured; without this fallback the
                // placeholder collapses to zero size and the image
                // disappears for one frame.
                val containerWidthPx = when {
                    containerSize.isUnspecified -> intrinsicImageSize.width
                    containerSize.width <= 0f -> intrinsicImageSize.width
                    else -> containerSize.width
                }
                val targetWidth = minOf(intrinsicImageSize.width, containerWidthPx)
                val ratio = intrinsicImageSize.height / intrinsicImageSize.width.coerceAtLeast(1f)
                val targetHeight = (targetWidth * ratio).coerceAtMost(
                    MAX_INLINE_HEIGHT_PX,
                )
                targetWidth.toSp().value to targetHeight.toSp().value
            }
        }
        return com.mikepenz.markdown.model.PlaceholderConfig(
            size = Size(widthSp, heightSp),
            verticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center,
        )
    }

    private suspend fun probeOnce(url: String): ProbeResult {
        probeMutex.withLock {
            probeCache[url]?.let { return it }
            val result = runCatching {
                val response: HttpResponse = probeClient.head(url)
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                when {
                    contentLength == null -> ProbeResult.Allowed
                    contentLength > MAX_IMAGE_BYTES -> ProbeResult.Skipped(contentLength)
                    else -> ProbeResult.Allowed
                }
            }.getOrElse {
                // Probe failed (timeout, CORS, network off). Allow render;
                // Coil itself will surface the error if the real request
                // fails. We don't penalise unreachable servers here.
                ProbeResult.Allowed
            }
            probeCache[url] = result
            return result
        }
    }

    sealed interface ProbeResult {
        data object Pending : ProbeResult
        data object Allowed : ProbeResult
        data class Skipped(val contentLength: Long) : ProbeResult
    }

    companion object {
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Mobile Safari/537.36 GitHubStore/1.8"

        // Hard cap on the decoded bitmap dimension (see class kdoc).
        const val MAX_BITMAP_DIMENSION_PX = 2048

        // Hard cap on the advertised payload size. Above this we skip the
        // image entirely. 5 MB covers ~98% of repo READMEs in the wild;
        // anything above is almost always a generated mega-screenshot or
        // an uncompressed source export that would be unreadable inline
        // anyway.
        const val MAX_IMAGE_BYTES = 5L * 1024 * 1024

        // Default inline slot for badges (most common SVG inline case).
        // 32sp tall fits a Shields.io / GitHub Actions badge with room
        // to breathe; 120sp wide is the typical "Get it on …" width.
        private const val BADGE_DEFAULT_WIDTH_SP = 120f
        private const val BADGE_DEFAULT_HEIGHT_SP = 32f
        // Cap inline image height when intrinsic size IS known so a
        // stray hero image inside a paragraph can't blow up the line.
        private const val MAX_INLINE_HEIGHT_PX = 240f

        private val networkHeaders =
            NetworkHeaders.Builder()
                .add("User-Agent", BROWSER_UA)
                .add(
                    "Accept",
                    "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
                )
                .build()

        // Process-wide probe cache. README rerenders, theme toggles, and
        // recompositions all share the same map so the HEAD round-trip
        // only happens once per URL per app session.
        private val probeCache = mutableMapOf<String, ProbeResult>()
        private val probeMutex = Mutex()
    }
}
