package zed.rainxch.githubstore.app.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.model.WhatsNewEntry
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

    init {
        viewModelScope.launch {
            try {
                evaluate()
            } catch (t: Throwable) {
                logger.e(t) { "Failed to evaluate what's-new state" }
            }
        }
        viewModelScope.launch {
            try {
                val entries = whatsNewLoader.loadAll()
                _historyEntries.value = entries
                _hasHistory.value = entries.size > 1
            } catch (t: Throwable) {
                logger.e(t) { "Failed to load what's-new history" }
            }
        }
    }

    private suspend fun evaluate() {
        val current = appVersionInfo.versionCode
        val lastSeen = tweaksRepository.getLastSeenWhatsNewVersionCode().first() ?: Int.MIN_VALUE

        if (lastSeen >= current) return

        val entry = whatsNewLoader.forVersionCode(current)
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
            } catch (t: Throwable) {
                logger.e(t) { "Failed to persist lastSeenWhatsNewVersionCode=${entry.versionCode}" }
            }
        }
    }

    fun forceShowLatest() {
        viewModelScope.launch {
            try {
                val current = appVersionInfo.versionCode
                val entry =
                    whatsNewLoader.forVersionCode(current)
                        ?: whatsNewLoader.loadAll().firstOrNull()
                        ?: return@launch
                _pendingEntry.value = entry
            } catch (t: Throwable) {
                logger.e(t) { "Failed to force-show latest what's-new entry" }
            }
        }
    }
}
