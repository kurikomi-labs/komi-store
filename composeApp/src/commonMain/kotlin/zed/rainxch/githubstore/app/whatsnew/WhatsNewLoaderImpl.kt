package zed.rainxch.githubstore.app.whatsnew

import kotlinx.serialization.json.Json
import zed.rainxch.core.data.dto.WhatsNewEntryDto
import zed.rainxch.core.data.mappers.toDomain
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.WhatsNewEntry
import zed.rainxch.core.domain.repository.WhatsNewLoader
import zed.rainxch.githubstore.core.presentation.res.Res

class WhatsNewLoaderImpl(
    private val knownVersionCodes: List<Int>,
    private val localizationManager: LocalizationManager,
    logger: GitHubStoreLogger,
) : WhatsNewLoader {
    private val tagged = logger.withTag("WhatsNewLoader")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadAll(): List<WhatsNewEntry> =
        knownVersionCodes
            .mapNotNull { vc -> loadOrNull(vc) }
            .sortedByDescending { it.versionCode }

    override suspend fun forVersionCode(versionCode: Int): WhatsNewEntry? = loadOrNull(versionCode)

    private suspend fun loadOrNull(versionCode: Int): WhatsNewEntry? {
        val candidates = candidatePaths(versionCode)
        for (path in candidates) {
            val parsed = readEntry(path)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun candidatePaths(versionCode: Int): List<String> {
        val full = localizationManager.getCurrentLanguageCode()
        val primary = localizationManager.getPrimaryLanguageCode()
        val paths = LinkedHashSet<String>()
        if (full.isNotBlank()) paths += "files/whatsnew/$full/$versionCode.json"
        if (primary.isNotBlank() && primary != full) paths += "files/whatsnew/$primary/$versionCode.json"
        paths += "files/whatsnew/$versionCode.json"
        return paths.toList()
    }

    private suspend fun readEntry(path: String): WhatsNewEntry? =
        try {
            val bytes = Res.readBytes(path)
            val text = bytes.decodeToString()
            json.decodeFromString(WhatsNewEntryDto.serializer(), text).toDomain()
        } catch (t: Throwable) {
            tagged.warn("Failed to load what's-new entry at $path: ${t.message}")
            null
        }
}

object KnownWhatsNewVersionCodes {
    val ALL: List<Int> = listOf(16, 15)
}
