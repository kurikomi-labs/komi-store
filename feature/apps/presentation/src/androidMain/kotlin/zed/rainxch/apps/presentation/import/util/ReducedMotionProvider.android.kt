package zed.rainxch.apps.presentation.import.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    // Reading once at composition is fine — ANIMATOR_DURATION_SCALE is a
    // global system setting that almost never flips while the wizard is
    // on screen. If it does, the next composition (rotation, navigation)
    // will pick up the new value.
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
