package zed.rainxch.core.presentation.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale
import coil3.BitmapImage
import coil3.Image

private const val SampleSize = 48

actual fun computeDominantFromImage(image: Image): Color? {
    val src = (image as? BitmapImage)?.bitmap ?: return null
    val software = if (src.config == Bitmap.Config.HARDWARE) {
        src.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    } else if (src.config != Bitmap.Config.ARGB_8888) {
        src.copy(Bitmap.Config.ARGB_8888, false) ?: src
    } else {
        src
    }
    val sampled: Bitmap = if (software.width > SampleSize || software.height > SampleSize) {
        software.scale(SampleSize, SampleSize, filter = true)
    } else {
        software
    }
    val w = sampled.width
    val h = sampled.height
    val pixels = IntArray(w * h)
    val pxResult = runCatching {
        sampled.getPixels(pixels, 0, w, 0, 0, w, h)
    }
    if (pxResult.isFailure) return null
    return dominantFromArgb(pixels)
}
