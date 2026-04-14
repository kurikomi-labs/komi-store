package zed.rainxch.core.domain.model

object AssetPreferenceMatcher {
    fun choosePreferredAsset(
        assets: List<GithubAsset>,
        preferredAssetNames: List<String?>,
    ): GithubAsset? {
        if (assets.isEmpty()) return null

        for (preferredName in preferredAssetNames) {
            val match = findBestMatchForPreferred(assets, preferredName)
            if (match != null) return match
        }

        return null
    }

    private fun findBestMatchForPreferred(
        assets: List<GithubAsset>,
        preferredName: String?,
    ): GithubAsset? {
        val normalizedPreferred = preferredName?.trim()?.lowercase().orEmpty()
        if (normalizedPreferred.isBlank()) return null

        assets.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }?.let { return it }

        val preferredFamily = familyKey(normalizedPreferred)
        val familyCandidates =
            assets.filter { asset ->
                familyKey(asset.name.lowercase()) == preferredFamily
            }
        if (familyCandidates.isEmpty()) return null

        val preferredExt = extension(normalizedPreferred)
        val sameExtCandidates =
            familyCandidates.filter { asset ->
                extension(asset.name.lowercase()) == preferredExt
            }
        val candidates = sameExtCandidates.ifEmpty { familyCandidates }

        return candidates.maxWithOrNull(
            compareBy<GithubAsset>(
                { similarityScore(normalizedPreferred, it.name.lowercase()) },
                { it.size },
            ),
        )
    }

    private fun extension(assetName: String): String = assetName.substringAfterLast('.', "")

    private fun familyKey(assetName: String): String {
        val normalized = assetName.substringBeforeLast('.', assetName).lowercase()
        val tokens =
            normalized
                .split(NON_ALNUM_REGEX)
                .filter { it.isNotBlank() }
                .filterNot { isLikelyVersionToken(it) }

        return if (tokens.isNotEmpty()) {
            tokens.joinToString("-")
        } else {
            normalized
        }
    }

    private fun similarityScore(
        preferredName: String,
        candidateName: String,
    ): Int {
        val preferredBase = preferredName.substringBeforeLast('.', preferredName)
        val candidateBase = candidateName.substringBeforeLast('.', candidateName)

        val sharedTokens =
            tokenize(preferredBase)
                .intersect(tokenize(candidateBase))
                .size
        val prefixLength = commonPrefixLength(preferredBase, candidateBase)

        return (sharedTokens * 100) + prefixLength
    }

    private fun tokenize(name: String): Set<String> =
        name
            .split(NON_ALNUM_REGEX)
            .filter { it.isNotBlank() }
            .toSet()

    private fun commonPrefixLength(
        first: String,
        second: String,
    ): Int {
        val limit = minOf(first.length, second.length)
        var idx = 0
        while (idx < limit && first[idx] == second[idx]) {
            idx++
        }
        return idx
    }

    private fun isLikelyVersionToken(token: String): Boolean = VERSION_TOKEN_REGEX.matches(token)

    private val NON_ALNUM_REGEX = Regex("[^a-z0-9]+")
    private val VERSION_TOKEN_REGEX = Regex("^v?\\d+(?:[._-]?\\d+)*[a-z]*$")
}
