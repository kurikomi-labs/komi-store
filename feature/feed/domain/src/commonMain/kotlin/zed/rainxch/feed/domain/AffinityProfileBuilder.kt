package zed.rainxch.feed.domain

import kotlinx.coroutines.flow.first
import zed.rainxch.core.domain.model.installation.isReallyInstalled
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.feed.domain.model.AffinityProfile

class AffinityProfileBuilder(
    private val starredRepository: StarredRepository,
    private val installedAppsRepository: InstalledAppsRepository,
) {
    suspend fun build(): AffinityProfile {
        val starred = starredRepository.getAllStarred().first()
        val installed = installedAppsRepository.getAllInstalledApps().first()
            .filter { it.isReallyInstalled() }

        val owners = HashMap<String, Double>()
        val languages = HashMap<String, Double>()

        starred.forEach { repo ->
            mergeMax(owners, repo.repoOwner, STARRED_WEIGHT)
            mergeMax(languages, repo.primaryLanguage, STARRED_WEIGHT)
        }
        installed.forEach { app ->
            mergeMax(owners, app.repoOwner, INSTALLED_WEIGHT)
            mergeMax(languages, app.primaryLanguage, INSTALLED_WEIGHT)
        }

        // topicWeights intentionally empty: starred/installed/seen Room tables store
        // primaryLanguage but no topics, so a topic affinity can't be built on-device
        // without a network round-trip. Deferred (GH#742 follow-up).
        return AffinityProfile(
            ownerWeights = owners,
            languageWeights = languages,
            topicWeights = emptyMap(),
        )
    }

    private fun mergeMax(target: MutableMap<String, Double>, key: String?, weight: Double) {
        val clean = key?.trim()?.takeIf { it.isNotEmpty() } ?: return
        target[clean] = maxOf(target[clean] ?: 0.0, weight)
    }

    companion object {
        private const val STARRED_WEIGHT = 1.0
        private const val INSTALLED_WEIGHT = 0.7
    }
}
