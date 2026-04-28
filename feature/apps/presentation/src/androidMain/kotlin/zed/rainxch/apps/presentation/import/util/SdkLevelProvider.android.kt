package zed.rainxch.apps.presentation.import.util

import android.os.Build
import androidx.compose.runtime.Composable

@Composable
actual fun rememberSdkInt(): Int? = Build.VERSION.SDK_INT
