package zed.rainxch.core.presentation.color

import androidx.compose.ui.graphics.Color

internal fun dominantFromArgb(pixels: IntArray): Color? {
    val strict = averageColor(pixels, lumMin = 24, lumMax = 232, chromaMin = 20)
    if (strict != null) return strict
    val relaxed = averageColor(pixels, lumMin = 10, lumMax = 245, chromaMin = 8)
    if (relaxed != null) return relaxed
    val anyColor = averageColor(pixels, lumMin = 4, lumMax = 251, chromaMin = 0)
    return anyColor
}

private fun averageColor(
    pixels: IntArray,
    lumMin: Int,
    lumMax: Int,
    chromaMin: Int,
): Color? {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L
    for (px in pixels) {
        val alpha = (px ushr 24) and 0xFF
        if (alpha < 128) continue
        val r = (px ushr 16) and 0xFF
        val g = (px ushr 8) and 0xFF
        val b = px and 0xFF
        val luminance = (r + g + b) / 3
        if (luminance < lumMin || luminance > lumMax) continue
        if (chromaMin > 0) {
            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            if (maxC - minC < chromaMin) continue
        }
        rSum += r
        gSum += g
        bSum += b
        count += 1
    }
    if (count < 6L) return null
    return Color(
        red = (rSum / count).toInt(),
        green = (gSum / count).toInt(),
        blue = (bSum / count).toInt(),
        alpha = 255,
    )
}
