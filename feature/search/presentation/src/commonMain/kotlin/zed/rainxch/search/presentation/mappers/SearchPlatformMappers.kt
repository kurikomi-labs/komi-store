package zed.rainxch.search.presentation.mappers

import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform.*
import zed.rainxch.search.presentation.model.SearchPlatformUi

fun SearchPlatformUi.toDomain(): DiscoveryPlatform =
    when (this) {
        SearchPlatformUi.All -> All
        SearchPlatformUi.Android -> Android
        SearchPlatformUi.Windows -> Windows
        SearchPlatformUi.Macos -> Macos
        SearchPlatformUi.Linux -> Linux
    }

fun DiscoveryPlatform.toSearchPlatformUi(): SearchPlatformUi =
    when (this) {
        All -> SearchPlatformUi.All
        Android -> SearchPlatformUi.Android
        Windows -> SearchPlatformUi.Windows
        Macos -> SearchPlatformUi.Macos
        Linux -> SearchPlatformUi.Linux
    }
