package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.ContentWidth.COMPACT
import zed.rainxch.core.domain.model.ContentWidth.EXTRA_WIDE
import zed.rainxch.core.domain.model.ContentWidth.WIDE
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.content_width_compact
import zed.rainxch.githubstore.core.presentation.res.content_width_extra_wide
import zed.rainxch.githubstore.core.presentation.res.content_width_wide

val ContentWidth.displayName: String
    @Composable
    get() =
        stringResource(
            when (this) {
                COMPACT -> Res.string.content_width_compact
                WIDE -> Res.string.content_width_wide
                EXTRA_WIDE -> Res.string.content_width_extra_wide
            },
        )
