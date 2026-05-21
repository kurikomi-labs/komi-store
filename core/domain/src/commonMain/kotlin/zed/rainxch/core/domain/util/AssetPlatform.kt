package zed.rainxch.core.domain.util

import zed.rainxch.core.domain.model.DiscoveryPlatform

fun assetPlatformOf(assetName: String): DiscoveryPlatform? {
    val lower = assetName.lowercase()
    return when {
        lower.endsWith(".apk") -> DiscoveryPlatform.Android
        lower.endsWith(".exe") || lower.endsWith(".msi") -> DiscoveryPlatform.Windows
        lower.endsWith(".dmg") || lower.endsWith(".pkg") -> DiscoveryPlatform.Macos
        lower.endsWith(".deb") ||
            lower.endsWith(".rpm") ||
            lower.endsWith(".appimage") ||
            lower.endsWith(".pkg.tar.zst") ||
            lower.endsWith(".snap") ||
            lower.endsWith(".flatpakref") -> DiscoveryPlatform.Linux
        else -> null
    }
}
