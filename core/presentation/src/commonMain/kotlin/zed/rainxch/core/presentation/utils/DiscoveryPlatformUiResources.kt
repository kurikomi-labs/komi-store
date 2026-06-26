package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.githubstore.core.presentation.res.*

@Composable
fun DiscoveryPlatform.toIcon(): ImageVector? =
    when (this) {
        DiscoveryPlatform.All -> {
            null
        }

        DiscoveryPlatform.Android -> {
            vectorResource(Res.drawable.ic_platform_android)
        }

        DiscoveryPlatform.Macos -> {
            vectorResource(Res.drawable.ic_platform_macos)
        }

        DiscoveryPlatform.Windows -> {
            vectorResource(Res.drawable.ic_platform_windows)
        }

        DiscoveryPlatform.Linux -> {
            vectorResource(Res.drawable.ic_platform_linux)
        }

        DiscoveryPlatform.Ios -> {
            vectorResource(Res.drawable.ic_platform_ios)
        }
    }

@Composable
fun DiscoveryPlatform.toLabel(): String =
    when (this) {
        DiscoveryPlatform.All -> {
            stringResource(Res.string.category_all)
        }

        DiscoveryPlatform.Android -> {
            "Android"
        }

        DiscoveryPlatform.Macos -> {
            "macOS"
        }

        DiscoveryPlatform.Windows -> {
            "Windows"
        }

        DiscoveryPlatform.Linux -> {
            "Linux"
        }

        DiscoveryPlatform.Ios -> {
            "iOS"
        }
    }
