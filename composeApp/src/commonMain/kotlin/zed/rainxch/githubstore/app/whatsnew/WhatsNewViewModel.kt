package zed.rainxch.githubstore.app.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.model.announcement.WhatsNewEntry
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.repository.WhatsNewLoader
import zed.rainxch.core.domain.system.AppVersionInfo

class WhatsNewViewModel(
    private val tweaksRepository: TweaksRepository,
    private val appVersionInfo: AppVersionInfo,
    private val whatsNewLoader: WhatsNewLoader,
) : ViewModel() {
    private val logger = Logger.withTag("WhatsNewViewModel")

    private val _pendingEntry = MutableStateFlow<WhatsNewEntry?>(null)
    val pendingEntry: StateFlow<WhatsNewEntry?> = _pendingEntry.asStateFlow()

    private val _historyEntries = MutableStateFlow<List<WhatsNewEntry>>(emptyList())
    val historyEntries: StateFlow<List<WhatsNewEntry>> = _historyEntries.asStateFlow()

    private val _hasHistory = MutableStateFlow(false)
    val hasHistory: StateFlow<Boolean> = _hasHistory.asStateFlow()

    @Volatile
    private var lastLanguageTag: String? = null

    init {

        viewModelScope.launch {
            try {
                tweaksRepository
                    .getAppLanguage()
                    .distinctUntilChanged()
                    .collect { tag ->
                        lastLanguageTag = tag
                        reloadHistory(tag)
                        reloadPending(tag)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.e(t) { "Failed to observe app-language for what's-new reloads" }
            }
        }
    }

    private suspend fun reloadHistory(languageTag: String?) {
        try {
            val entries = whatsNewLoader.loadAll(languageTag)
            _historyEntries.value = entries
            _hasHistory.value = entries.size > 1
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            logger.e(t) { "Failed to load what's-new history" }
        }
    }

    private suspend fun reloadPending(languageTag: String?) {
        try {
            evaluate(languageTag)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            logger.e(t) { "Failed to evaluate what's-new state" }
        }
    }

    private suspend fun evaluate(languageTag: String? = lastLanguageTag) {
        val current = appVersionInfo.versionCode
        val lastSeen = tweaksRepository.getLastSeenWhatsNewVersionCode().first() ?: Int.MIN_VALUE

        if (lastSeen >= current) return

        val entry = whatsNewLoader.forVersionCode(current, languageTag)
        if (entry == null || !entry.showAsSheet) {
            tweaksRepository.setLastSeenWhatsNewVersionCode(current)
            return
        }

        _pendingEntry.value = entry
    }

    fun markSeen() {
        val entry = _pendingEntry.value ?: return
        _pendingEntry.value = null
        viewModelScope.launch {
            try {
                tweaksRepository.setLastSeenWhatsNewVersionCode(entry.versionCode)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.e(t) { "Failed to persist lastSeenWhatsNewVersionCode=${entry.versionCode}" }
            }
        }
    }

    fun forceShowLatest() {
        viewModelScope.launch {
            try {
                val tag = lastLanguageTag
                val current = appVersionInfo.versionCode
                val entry =
                    whatsNewLoader.forVersionCode(current, tag)
                        ?: whatsNewLoader.loadAll(tag).firstOrNull()
                        ?: return@launch
                _pendingEntry.value = entry
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.e(t) { "Failed to force-show latest what's-new entry" }
            }
        }
    }
}
