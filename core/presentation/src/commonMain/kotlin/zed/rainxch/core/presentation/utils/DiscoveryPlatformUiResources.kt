package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.githubstore.core.presentation.res.*

@Composable
fun DiscoveryPlatform.toIcons(): List<ImageVector> =
    when (this) {
        DiscoveryPlatform.All -> {
            listOf(
                vectorResource(Res.drawable.ic_platform_android),
                vectorResource(Res.drawable.ic_platform_linux),
                vectorResource(Res.drawable.ic_platform_macos),
                vectorResource(Res.drawable.ic_platform_windows),
            )
        }

        DiscoveryPlatform.Android -> {
            listOf(vectorResource(Res.drawable.ic_platform_android))
        }

        DiscoveryPlatform.Macos -> {
            listOf(vectorResource(Res.drawable.ic_platform_macos))
        }

        DiscoveryPlatform.Windows -> {
            listOf(vectorResource(Res.drawable.ic_platform_windows))
        }

        DiscoveryPlatform.Linux -> {
            listOf(vectorResource(Res.drawable.ic_platform_linux))
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
    }
