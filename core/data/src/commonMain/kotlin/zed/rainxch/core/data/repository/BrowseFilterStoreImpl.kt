package zed.rainxch.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.repository.FeedCategory
import zed.rainxch.core.domain.model.system.Platform
import zed.rainxch.core.domain.repository.BrowseFilterStore

class BrowseFilterStoreImpl : BrowseFilterStore {
    private val _platform = MutableStateFlow(detectedPlatform())
    override val platform: StateFlow<DiscoveryPlatform> = _platform.asStateFlow()

    private val _category = MutableStateFlow(FeedCategory.All)
    override val category: StateFlow<FeedCategory> = _category.asStateFlow()

    override fun setPlatform(platform: DiscoveryPlatform) {
        _platform.value = platform
    }

    override fun setCategory(category: FeedCategory) {
        _category.value = category
    }
}

private fun detectedPlatform(): DiscoveryPlatform =
    when (getPlatform()) {
        Platform.ANDROID -> DiscoveryPlatform.Android
        Platform.WINDOWS -> DiscoveryPlatform.Windows
        Platform.MACOS -> DiscoveryPlatform.Macos
        Platform.LINUX -> DiscoveryPlatform.Linux
    }
