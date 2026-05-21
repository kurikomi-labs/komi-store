package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.Announcement
import zed.rainxch.core.domain.model.AnnouncementCategory
import zed.rainxch.core.domain.model.AnnouncementSeverity
import zed.rainxch.core.domain.model.AnnouncementsFeedSnapshot

interface AnnouncementsRepository {
    fun observeFeed(): Flow<AnnouncementsFeedSnapshot>

    suspend fun refresh(): Result<Unit>

    suspend fun dismiss(id: String)

    suspend fun acknowledge(id: String)

    suspend fun setMuted(category: AnnouncementCategory, muted: Boolean)
}