@file:OptIn(ExperimentalTime::class)

package zed.rainxch.githubstore.app.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.helpers.BrowserHelper
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.core.domain.model.announcement.AnnouncementIconHint
import zed.rainxch.core.domain.model.announcement.AnnouncementSeverity
import zed.rainxch.core.domain.model.announcement.AnnouncementsFeedSnapshot
import zed.rainxch.core.domain.repository.AnnouncementsRepository
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AnnouncementsViewModel(
    private val repository: AnnouncementsRepository,
    private val browserHelper: BrowserHelper,
) : ViewModel() {
    private val logger = Logger.withTag("AnnouncementsViewModel")

    val feed: StateFlow<AnnouncementsFeedSnapshot> =
        repository
            .observeFeed()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    AnnouncementsFeedSnapshot(
                        items = emptyList(),
                        dismissedIds = emptySet(),
                        acknowledgedIds = emptySet(),
                        mutedCategories = emptySet(),
                        lastFetchedAtMillis = 0L,
                        lastRefreshFailed = false,
                    ),
            )

    private val _previewItems = MutableStateFlow<List<Announcement>>(emptyList())
    val previewItems: StateFlow<List<Announcement>> = _previewItems.asStateFlow()

    val displayedItems: StateFlow<List<Announcement>> =
        combine(feed, previewItems) { snapshot, preview ->
            (preview + snapshot.visibleItems).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> =
        feed
            .map { it.unreadCount }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pendingCriticalAcknowledgment: StateFlow<Announcement?> =
        combine(feed, previewItems) { snapshot, preview ->
            preview.firstOrNull {
                it.severity == AnnouncementSeverity.CRITICAL && it.requiresAcknowledgment
            } ?: snapshot.pendingCriticalAcknowledgment
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            try {
                repository.refresh()
            } catch (t: Throwable) {
                logger.e(t) { "Initial announcements refresh failed" }
            }
        }
    }

    suspend fun refresh() {
        try {
            repository.refresh()
        } catch (t: Throwable) {
            logger.e(t) { "Manual announcements refresh failed" }
        }
    }

    fun clearPreview() {
        if (_previewItems.value.isNotEmpty()) {
            _previewItems.value = emptyList()
        }
    }

    fun markRoutineItemsSeen() {
        viewModelScope.launch {
            try {
                val snapshot = feed.value
                snapshot.visibleItems.forEach { item ->
                    if (!item.requiresAcknowledgment && item.id !in snapshot.acknowledgedIds) {
                        repository.acknowledge(item.id)
                    }
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                logger.e(t) { "Failed to mark routine announcements as seen" }
            }
        }
    }

    fun dismiss(announcement: Announcement) {
        if (removeFromPreview(announcement.id)) return
        viewModelScope.launch {
            try {
                repository.dismiss(announcement.id)
                if (!announcement.requiresAcknowledgment) {
                    repository.acknowledge(announcement.id)
                }
            } catch (t: Throwable) {
                logger.e(t) { "Failed to dismiss ${announcement.id}" }
            }
        }
    }

    fun acknowledge(announcement: Announcement) {
        if (removeFromPreview(announcement.id)) return
        viewModelScope.launch {
            try {
                repository.acknowledge(announcement.id)
            } catch (t: Throwable) {
                logger.e(t) { "Failed to acknowledge ${announcement.id}" }
            }
        }
    }

    private fun removeFromPreview(id: String): Boolean {
        val current = _previewItems.value
        if (current.none { it.id == id }) return false
        _previewItems.value = current.filterNot { it.id == id }
        return true
    }

    fun previewSampleAnnouncements() {
        _previewItems.value = SamplePreviewItems
    }

    fun openCta(announcement: Announcement) {
        val url = announcement.ctaUrl ?: return
        viewModelScope.launch {
            try {
                repository.acknowledge(announcement.id)
            } catch (t: Throwable) {
                logger.e(t) { "Failed to acknowledge before opening CTA ${announcement.id}" }
            }
        }
        browserHelper.openUrl(url) { error ->
            logger.w("Failed to open CTA url for ${announcement.id}: $error")
        }
    }

    fun setMuted(
        category: AnnouncementCategory,
        muted: Boolean,
    ) {
        viewModelScope.launch {
            try {
                repository.setMuted(category, muted)
            } catch (t: Throwable) {
                logger.e(t) { "Failed to toggle mute for $category" }
            }
        }
    }

    private companion object {
        val SamplePreviewItems =
            listOf(
                Announcement(
                    id = "preview-info-news",
                    publishedAt = Instant.parse("2026-05-03T00:00:00Z"),
                    expiresAt = null,
                    severity = AnnouncementSeverity.INFO,
                    category = AnnouncementCategory.NEWS,
                    title = "Preview: backing Keep Android Open",
                    body =
                        "GitHub Store supports the Keep Android Open initiative. " +
                            "Google's proposed sideloading restrictions would make this app — " +
                            "and Obtainium, F-Droid, and others — much harder to use.",
                    ctaUrl = "https://github-store.org",
                    ctaLabel = "Read more",
                    dismissible = true,
                    requiresAcknowledgment = false,
                    minVersionCode = null,
                    maxVersionCode = null,
                    platforms = null,
                    installerTypes = null,
                    iconHint = AnnouncementIconHint.CHANGE,
                ),
                Announcement(
                    id = "preview-important-survey",
                    publishedAt = Instant.parse("2026-05-02T00:00:00Z"),
                    expiresAt = null,
                    severity = AnnouncementSeverity.IMPORTANT,
                    category = AnnouncementCategory.SURVEY,
                    title = "Preview: five-minute survey shaping 1.9",
                    body = "Mostly multiple choice. Your answers shape what ships next quarter.",
                    ctaUrl = "https://github-store.org",
                    ctaLabel = "Open survey",
                    dismissible = true,
                    requiresAcknowledgment = false,
                    minVersionCode = null,
                    maxVersionCode = null,
                    platforms = null,
                    installerTypes = null,
                    iconHint = AnnouncementIconHint.INFO,
                ),
                Announcement(
                    id = "preview-critical-security",
                    publishedAt = Instant.parse("2026-05-01T00:00:00Z"),
                    expiresAt = null,
                    severity = AnnouncementSeverity.CRITICAL,
                    category = AnnouncementCategory.SECURITY,
                    title = "Preview: critical security advisory",
                    body =
                        "This is a sample critical advisory for testing the modal flow. " +
                            "In the real channel, only download-integrity, killswitch, or " +
                            "credential-exposure events would carry this severity.",
                    ctaUrl = "https://github-store.org",
                    ctaLabel = "View advisory",
                    dismissible = false,
                    requiresAcknowledgment = true,
                    minVersionCode = null,
                    maxVersionCode = null,
                    platforms = null,
                    installerTypes = null,
                    iconHint = AnnouncementIconHint.SECURITY,
                ),
            )
    }
}
