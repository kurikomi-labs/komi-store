package zed.rainxch.apps.presentation.import.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPackageVisibilityRequester(): PackageVisibilityRequester

interface PackageVisibilityRequester {
    suspend fun isGranted(): Boolean

    suspend fun requestOrOpenSettings(): Boolean
}
