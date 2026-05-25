package zed.rainxch.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

expect fun isDynamicColorAvailable(): Boolean

@Composable
expect fun dynamicColorScheme(isDark: Boolean): ColorScheme?
