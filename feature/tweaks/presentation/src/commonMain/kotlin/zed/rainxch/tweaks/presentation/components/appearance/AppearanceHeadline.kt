package zed.rainxch.tweaks.presentation.components.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.domain.model.appearance.AppPersonality
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.MangaAccent
import zed.rainxch.tweaks.presentation.components.shell.SettingsGroup

private val AccentOrder =
    listOf(
        MangaAccent.CRIMSON,
        MangaAccent.COBALT,
        MangaAccent.SUN,
        MangaAccent.FROST,
        MangaAccent.MONO,
    )

@Composable
fun AppearanceHeadline(
    personality: AppPersonality,
    accent: MangaAccent,
    onPersonalitySelected: (AppPersonality) -> Unit,
    onAccentSelected: (MangaAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 16.dp, bottom = 17.dp)) {
            HeadlineLabel(latin = "Personality", jp = "個性")
            Row(
                modifier = Modifier.padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PersonalityTile(
                    kind = AppPersonality.MANGA,
                    selected = personality == AppPersonality.MANGA,
                    onClick = { onPersonalitySelected(AppPersonality.MANGA) },
                    modifier = Modifier.weight(1f),
                )
                PersonalityTile(
                    kind = AppPersonality.CLASSIC,
                    selected = personality == AppPersonality.CLASSIC,
                    onClick = { onPersonalitySelected(AppPersonality.CLASSIC) },
                    modifier = Modifier.weight(1f),
                )
            }

            HeadlineLabel(latin = "Accent", jp = "色", modifier = Modifier.padding(top = 18.dp))
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccentOrder.forEach { entry ->
                    AccentSwatch(
                        accent = entry,
                        selected = accent == entry,
                        onClick = { onAccentSelected(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadlineLabel(
    latin: String,
    jp: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPersonality.current.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KomiText(
            text = latin,
            role = KomiTextRole.Stamp,
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
        KomiText(
            text = jp,
            role = KomiTextRole.Label,
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            uppercase = false,
        )
    }
}
