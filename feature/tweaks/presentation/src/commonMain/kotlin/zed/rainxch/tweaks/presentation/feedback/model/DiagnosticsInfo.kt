package zed.rainxch.tweaks.presentation.feedback.model

data class DiagnosticsInfo(
    val appVersion: String,
    val platform: String,
    val osVersion: String,
    val locale: String,
    val installerType: String?,
    val githubUsername: String?,
    val themePalette: String,
    val themeMode: String,
)
