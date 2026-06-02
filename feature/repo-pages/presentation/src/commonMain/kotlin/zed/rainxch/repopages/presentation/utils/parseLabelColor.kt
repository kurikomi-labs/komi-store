package zed.rainxch.repopages.presentation.utils

import androidx.compose.ui.graphics.Color

internal fun parseLabelColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6) Color(("FF$clean").toLong(16)) else Color(0xFF888888)
    } catch (_: Exception) {
        Color(0xFF888888)
    }
}