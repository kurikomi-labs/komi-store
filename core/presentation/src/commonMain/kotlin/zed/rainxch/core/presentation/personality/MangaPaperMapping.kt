package zed.rainxch.core.presentation.personality

import zed.rainxch.core.domain.model.appearance.MangaPaperId
import zed.rainxch.core.presentation.personality.manga.MangaPaper

fun MangaPaperId.toMangaPaper(): MangaPaper =
    when (this) {
        MangaPaperId.DAY -> MangaPaper.DAY
        MangaPaperId.NIGHT -> MangaPaper.NIGHT
        MangaPaperId.NORD -> MangaPaper.NORD
    }

fun MangaPaper.toMangaPaperId(): MangaPaperId =
    when (this) {
        MangaPaper.DAY -> MangaPaperId.DAY
        MangaPaper.NIGHT -> MangaPaperId.NIGHT
        MangaPaper.NORD -> MangaPaperId.NORD
    }
