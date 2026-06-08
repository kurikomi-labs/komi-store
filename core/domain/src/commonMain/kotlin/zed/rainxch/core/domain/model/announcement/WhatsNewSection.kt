package zed.rainxch.core.domain.model.announcement

data class WhatsNewSection(
    val type: WhatsNewSectionType,
    val bullets: List<String>,
)
