package zed.rainxch.feed.domain

import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.feed.domain.model.AffinityProfile

object FeedAffinityScorer {

    const val MAX_BOOST = 0.4
    private const val W_OWNER = 0.6
    private const val W_LANG = 0.25
    private const val W_TOPIC = 0.15
    private const val SEEN_PENALTY = 0.25
    private const val OWNER_WINDOW = 8
    private const val INSTALLED_TOP_FLOOR = 3

    fun rank(
        items: List<GithubRepoSummary>,
        profile: AffinityProfile,
        seenIds: Set<Long>,
        installedRepoIds: Set<Long>,
    ): List<GithubRepoSummary> {
        if (items.size < 2) return items
        if (profile.isEmpty && seenIds.isEmpty() && installedRepoIds.isEmpty()) return items

        val reranked =
            items
                .mapIndexed { index, repo -> Scored(repo, finalRank(repo, index, profile, seenIds)) }
                .sortedBy { it.rank }
                .map { it.repo }

        return demoteInstalled(spaceOwners(reranked), installedRepoIds)
    }

    private fun finalRank(
        repo: GithubRepoSummary,
        index: Int,
        profile: AffinityProfile,
        seenIds: Set<Long>,
    ): Double {
        val ownerB = profile.ownerWeights[repo.owner.login] ?: 0.0
        val langB = repo.language?.let { profile.languageWeights[it] } ?: 0.0
        val topicB = repo.topics?.sumOf { profile.topicWeights[it] ?: 0.0 } ?: 0.0
        val positiveBoost = (W_OWNER * ownerB + W_LANG * langB + W_TOPIC * topicB).coerceIn(0.0, MAX_BOOST)
        val seenPenalty = if (repo.id in seenIds) SEEN_PENALTY else 0.0
        // 1-indexed position so a top item (index 0) can still be reordered.
        return (index + 1) * (1.0 - positiveBoost + seenPenalty)
    }

    private fun spaceOwners(list: List<GithubRepoSummary>): List<GithubRepoSummary> {
        val pending = ArrayDeque(list)
        val result = ArrayList<GithubRepoSummary>(list.size)
        while (pending.isNotEmpty()) {
            val pickIndex = pending.indexOfFirst { candidate ->
                val recent = result.takeLast(OWNER_WINDOW)
                recent.none { it.owner.login == candidate.owner.login }
            }
            if (pickIndex == -1) {
                result.add(pending.removeFirst())
            } else {
                result.add(pending.removeAt(pickIndex))
            }
        }
        return result
    }

    private fun demoteInstalled(
        list: List<GithubRepoSummary>,
        installedRepoIds: Set<Long>,
    ): List<GithubRepoSummary> {
        if (installedRepoIds.isEmpty() || list.size <= INSTALLED_TOP_FLOOR) return list
        val deferred = ArrayList<GithubRepoSummary>()
        val result = ArrayList<GithubRepoSummary>(list.size)
        val iterator = list.iterator()
        while (result.size < INSTALLED_TOP_FLOOR && iterator.hasNext()) {
            val item = iterator.next()
            if (item.id in installedRepoIds) deferred.add(item) else result.add(item)
        }
        result.addAll(deferred)
        while (iterator.hasNext()) result.add(iterator.next())
        return result
    }

    private data class Scored(val repo: GithubRepoSummary, val rank: Double)
}
