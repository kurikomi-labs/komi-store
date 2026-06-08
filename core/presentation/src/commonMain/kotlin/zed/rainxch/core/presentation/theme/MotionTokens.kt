package zed.rainxch.core.presentation.theme

data class MotionTokens(
    val tapHighlightMs: Int,
    val paletteCrossfadeMs: Int,
    val sheetSlideMs: Int,
    val scrimFadeMs: Int,
    val toastSlideMs: Int,
    val toastFadeMs: Int,
    val heartbeatScaleFrom: Float,
    val heartbeatScaleTo: Float,
    val heartbeatHaloFromScale: Float,
    val heartbeatHaloToScale: Float,
    val heartbeatHaloFromAlpha: Float,
    val heartbeatHaloToAlpha: Float,
)
