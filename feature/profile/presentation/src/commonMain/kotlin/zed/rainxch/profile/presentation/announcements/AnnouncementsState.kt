package zed.rainxch.profile.presentation.announcements

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory

data class AnnouncementsState(
    val items: ImmutableList<Announcement> = persistentListOf(),
    val acknowledgedIds: ImmutableSet<String> = persistentSetOf(),
    val mutedCategories: ImmutableSet<AnnouncementCategory> = persistentSetOf(),
    val expandedIds: ImmutableSet<String> = persistentSetOf(),
    val refreshFailed: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMuteSheetVisible: Boolean = false,
)
