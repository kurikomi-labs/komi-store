package zed.rainxch.apps.presentation.import.util

import androidx.compose.runtime.Composable

// Desktop has no equivalent system "remove animations" toggle that the
// JVM Compose target can read portably. The wizard isn't shown on
// Desktop today (E2 territory), so this is a safe default.
@Composable
actual fun rememberSystemReducedMotion(): Boolean = false
