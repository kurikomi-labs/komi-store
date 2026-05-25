package zed.rainxch.core.presentation.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual fun isDynamicColorAvailable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun dynamicColorScheme(isDark: Boolean): ColorScheme? {
    if (!isDynamicColorAvailable()) return null
    val context = LocalContext.current
    return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
