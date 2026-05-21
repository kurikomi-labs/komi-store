package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.MirrorConfig
import zed.rainxch.core.domain.model.MirrorPreference

interface MirrorRepository {

    fun observeCatalog(): Flow<List<MirrorConfig>>

    suspend fun refreshCatalog(): Result<Unit>

    fun observePreference(): Flow<MirrorPreference>

    suspend fun setPreference(pref: MirrorPreference)

    fun observeRemovedNotices(): Flow<MirrorRemoved>

    suspend fun snoozeAutoSuggest(forMs: Long)

    suspend fun dismissAutoSuggestPermanently()
}

data class MirrorRemoved(
    val displayName: String,
)
