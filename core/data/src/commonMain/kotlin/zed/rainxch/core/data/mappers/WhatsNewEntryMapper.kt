package zed.rainxch.core.data.mappers

import zed.rainxch.core.data.dto.WhatsNewEntryDto
import zed.rainxch.core.data.dto.WhatsNewSectionDto
import zed.rainxch.core.domain.model.WhatsNewEntry
import zed.rainxch.core.domain.model.WhatsNewSection
import zed.rainxch.core.domain.model.WhatsNewSectionType

fun WhatsNewEntryDto.toDomain(): WhatsNewEntry =
    WhatsNewEntry(
        versionCode = versionCode,
        versionName = versionName,
        releaseDate = releaseDate,
        sections = sections.map { it.toDomain() },
        showAsSheet = showAsSheet,
    )

fun WhatsNewSectionDto.toDomain(): WhatsNewSection =
    WhatsNewSection(
        type = WhatsNewSectionType.valueOf(type.trim().uppercase()),
        bullets = bullets,
    )
