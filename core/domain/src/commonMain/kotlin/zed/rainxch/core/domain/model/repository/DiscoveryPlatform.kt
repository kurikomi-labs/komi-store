package zed.rainxch.core.domain.model

enum class DiscoveryPlatform {
    All,
    Android,
    Macos,
    Windows,
    Linux,
    ;

    companion object {
        fun fromName(name: String?): DiscoveryPlatform = DiscoveryPlatform.entries.find { it.name == name } ?: All

        val selectablePlatforms: List<DiscoveryPlatform> =
            listOf(Android, Macos, Windows, Linux)
    }
}
