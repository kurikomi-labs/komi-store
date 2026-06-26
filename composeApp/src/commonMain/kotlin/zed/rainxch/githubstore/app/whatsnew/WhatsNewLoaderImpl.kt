package zed.rainxch.githubstore.app.whatsnew

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import zed.rainxch.core.data.dto.WhatsNewEntryDto
import zed.rainxch.core.data.mappers.toDomain
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.logging.KomiStoreLogger
import zed.rainxch.core.domain.model.announcement.WhatsNewEntry
import zed.rainxch.core.domain.repository.WhatsNewLoader
import zed.rainxch.githubstore.core.presentation.res.Res

class WhatsNewLoaderImpl(
    private val localizationManager: LocalizationManager,
    logger: KomiStoreLogger,
) : WhatsNewLoader {
    private val tagged = logger.withTag("WhatsNewLoader")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadAll(languageTag: String?): List<WhatsNewEntry> =
        KnownWhatsNewVersionCodes.ALL
            .mapNotNull { vc -> loadOrNull(vc, languageTag) }
            .sortedByDescending { it.versionCode }

    override suspend fun forVersionCode(
        versionCode: Int,
        languageTag: String?,
    ): WhatsNewEntry? = loadOrNull(versionCode, languageTag)

    private suspend fun loadOrNull(
        versionCode: Int,
        languageTag: String?,
    ): WhatsNewEntry? {
        val candidates = candidatePaths(versionCode, languageTag)
        candidates.forEachIndexed { index, path ->
            val isDefault = index == candidates.lastIndex
            val parsed = readEntry(path, warnOnMissing = isDefault)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun candidatePaths(
        versionCode: Int,
        languageTag: String?,
    ): List<String> {
        val (full, primary) =
            if (!languageTag.isNullOrBlank()) {
                languageTag to languageTag.substringBefore('-')
            } else {
                localizationManager.getCurrentLanguageCode() to
                    localizationManager.getPrimaryLanguageCode()
            }
        val paths = LinkedHashSet<String>()
        if (full.isNotBlank()) paths += "files/whatsnew/$full/$versionCode.json"
        if (primary.isNotBlank() && primary != full) paths += "files/whatsnew/$primary/$versionCode.json"
        paths += "files/whatsnew/$versionCode.json"
        return paths.toList()
    }

    private suspend fun readEntry(
        path: String,
        warnOnMissing: Boolean,
    ): WhatsNewEntry? =
        try {
            val bytes = Res.readBytes(path)
            val text = bytes.decodeToString()
            json.decodeFromString(WhatsNewEntryDto.serializer(), text).toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (isMissingResource(t)) {
                if (warnOnMissing) {
                    tagged.warn("Missing what's-new entry at $path: ${t.message}")
                }
            } else {
                tagged.warn("Failed to parse what's-new entry at $path: ${t.message}")
            }
            null
        }

    private fun isMissingResource(t: Throwable): Boolean =
        t::class.simpleName == "MissingResourceException" ||
            t.message?.contains("Missing resource", ignoreCase = true) == true
}
