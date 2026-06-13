package zed.rainxch.feed.domain.model

data class AffinityProfile(
    val ownerWeights: Map<String, Double>,
    val languageWeights: Map<String, Double>,
    val topicWeights: Map<String, Double>,
) {
    val isEmpty: Boolean
        get() = ownerWeights.isEmpty() && languageWeights.isEmpty() && topicWeights.isEmpty()

    companion object {
        val EMPTY = AffinityProfile(emptyMap(), emptyMap(), emptyMap())
    }
}
