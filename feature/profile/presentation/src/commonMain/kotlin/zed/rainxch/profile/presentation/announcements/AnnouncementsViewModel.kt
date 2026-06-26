package zed.rainxch.profile.presentation.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.helpers.BrowserHelper
import zed.rainxch.core.domain.logging.KomiStoreLogger
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.core.domain.model.announcement.AnnouncementsFeedSnapshot
import zed.rainxch.core.domain.repository.AnnouncementsRepository

class AnnouncementsViewModel(
    private val repository: AnnouncementsRepository,
    private val browserHelper: BrowserHelper,
    private val logger: KomiStoreLogger,
) : ViewModel() {
    private val feed: StateFlow<AnnouncementsFeedSnapshot?> =
        repository
            .observeFeed()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )

    private val _isMuteSheetVisible = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<AnnouncementsState> =
        combine(
            feed.filterNotNull(),
            _isMuteSheetVisible,
            _isRefreshing,
            _expandedIds,
        ) { snapshot, isMuteSheetVisible, isRefreshing, expandedIds ->
            AnnouncementsState(
                items = snapshot.visibleItems.toImmutableList(),
                acknowledgedIds = snapshot.acknowledgedIds.toImmutableSet(),
                mutedCategories = snapshot.mutedCategories.toImmutableSet(),
                expandedIds = expandedIds.toImmutableSet(),
                refreshFailed = snapshot.lastRefreshFailed,
                isRefreshing = isRefreshing,
                isMuteSheetVisible = isMuteSheetVisible,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnnouncementsState(),
        )

    val unreadCount: StateFlow<Int> =
        feed
            .map { it?.unreadCount ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = 0,
            )

    init {
        viewModelScope.launch {
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Initial announcements refresh failed")
            }
        }
    }

    fun onAction(action: AnnouncementsAction) {
        when (action) {
            AnnouncementsAction.OnRefresh -> refresh()
            AnnouncementsAction.OnOpenMuteSheet -> _isMuteSheetVisible.value = true
            AnnouncementsAction.OnDismissMuteSheet -> _isMuteSheetVisible.value = false
            AnnouncementsAction.OnEnterScreen -> markRoutineItemsSeen()
            is AnnouncementsAction.OnCtaClick -> openCta(action.announcement)
            is AnnouncementsAction.OnDismissClick -> dismiss(action.announcement)
            is AnnouncementsAction.OnAcknowledgeClick -> acknowledge(action.announcement)
            is AnnouncementsAction.OnToggleExpand -> toggleExpand(action.announcementId)
            is AnnouncementsAction.OnToggleMute -> setMuted(action.category, action.muted)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Manual announcements refresh failed")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun toggleExpand(id: String) {
        _expandedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    private fun markRoutineItemsSeen() {
        val snapshot = feed.value ?: return
        viewModelScope.launch {
            try {
                snapshot.visibleItems.forEach { item ->
                    if (!item.requiresAcknowledgment && item.id !in snapshot.acknowledgedIds) {
                        repository.acknowledge(item.id)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Failed to mark routine announcements seen")
            }
        }
    }

    private fun dismiss(announcement: Announcement) {
        viewModelScope.launch {
            try {
                repository.dismiss(announcement.id)
                if (!announcement.requiresAcknowledgment) {
                    repository.acknowledge(announcement.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Failed to dismiss ${announcement.id}")
            }
        }
    }

    private fun acknowledge(announcement: Announcement) {
        viewModelScope.launch {
            try {
                repository.acknowledge(announcement.id)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Failed to acknowledge ${announcement.id}")
            }
        }
    }

    private fun openCta(announcement: Announcement) {
        val url = announcement.ctaUrl ?: return
        viewModelScope.launch {
            try {
                repository.acknowledge(announcement.id)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Failed to acknowledge before opening CTA ${announcement.id}")
            }
        }
        browserHelper.openUrl(url) { error ->
            logger.error("Failed to open CTA url for ${announcement.id}: $error")
        }
    }

    private fun setMuted(
        category: AnnouncementCategory,
        muted: Boolean,
    ) {
        viewModelScope.launch {
            try {
                repository.setMuted(category, muted)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("${t.message} - Failed to toggle mute for $category")
            }
        }
    }
}
