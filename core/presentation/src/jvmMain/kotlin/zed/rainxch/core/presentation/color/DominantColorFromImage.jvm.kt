package zed.rainxch.core.presentation.color

import androidx.compose.ui.graphics.Color
import coil3.Image
import coil3.toBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

private const val SampleSize = 48

actual fun computeDominantFromImage(image: Image): Color? {
    val w = SampleSize
    val h = SampleSize
    val bitmap = runCatching { image.toBitmap(w, h) }.getOrNull() ?: return null
    val info = ImageInfo(
        width = w,
        height = h,
        colorType = ColorType.BGRA_8888,
        alphaType = ColorAlphaType.UNPREMUL,
    )
    val bytes = runCatching {
        bitmap.readPixels(dstInfo = info, dstRowBytes = w * 4, srcX = 0, srcY = 0)
    }.getOrNull() ?: return null
    val pixels = IntArray(w * h)
    var i = 0
    var p = 0
    while (i < pixels.size && p + 3 < bytes.size) {
        val b = bytes[p].toInt() and 0xFF
        val g = bytes[p + 1].toInt() and 0xFF
        val r = bytes[p + 2].toInt() and 0xFF
        val a = bytes[p + 3].toInt() and 0xFF
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        i += 1
        p += 4
    }
    return dominantFromArgb(pixels)
}
