package zed.rainxch.tweaks.presentation.components.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.MangaAccent
import zed.rainxch.core.presentation.personality.manga.decoration.gridPaper
import zed.rainxch.core.presentation.personality.manga.decoration.hardShadow
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.section_app_block
import zed.rainxch.githubstore.core.presentation.res.section_connectivity
import zed.rainxch.githubstore.core.presentation.res.section_look_and_feel
import zed.rainxch.githubstore.core.presentation.res.section_privacy_and_data
import zed.rainxch.githubstore.core.presentation.res.tweaks_title
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.core.domain.model.appearance.AppPersonality
import zed.rainxch.tweaks.presentation.components.sections.appSection
import zed.rainxch.tweaks.presentation.components.sections.connectivitySection
import zed.rainxch.tweaks.presentation.components.sections.lookAndFeelSection
import zed.rainxch.tweaks.presentation.components.sections.privacySection

private enum class DesktopSection(val jp: String) {
    LOOK("外観"),
    CONNECTIVITY("接続"),
    PRIVACY("個人情報"),
    APP("アプリ"),
}

@Composable
private fun DesktopSection.label(): String =
    when (this) {
        DesktopSection.LOOK -> stringResource(Res.string.section_look_and_feel)
        DesktopSection.CONNECTIVITY -> stringResource(Res.string.section_connectivity)
        DesktopSection.PRIVACY -> stringResource(Res.string.section_privacy_and_data)
        DesktopSection.APP -> stringResource(Res.string.section_app_block)
    }

@Composable
fun TweaksDesktopContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    personality: AppPersonality,
    accent: MangaAccent,
    onPersonalitySelected: (AppPersonality) -> Unit,
    onAccentSelected: (MangaAccent) -> Unit,
    currentLanguageLabel: String,
    onOpenLanguage: () -> Unit,
    onOpenFeedback: () -> Unit,
    onNavigateToMirrorPicker: () -> Unit,
    onNavigateToHiddenRepositories: () -> Unit,
    onNavigateToHostTokens: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableStateOf(DesktopSection.LOOK) }
    val colors = LocalPersonality.current.colors

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .width(230.dp)
                    .fillMaxHeight()
                    .background(colors.surface)
                    .drawBehind {
                        val stroke = 3.dp.toPx()
                        drawLine(
                            color = colors.outline,
                            start = Offset(size.width - stroke / 2f, 0f),
                            end = Offset(size.width - stroke / 2f, size.height),
                            strokeWidth = stroke,
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.tweaks_title),
                role = KomiTextRole.Display,
                color = colors.onSurface,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
            KomiText(
                text = "設定",
                role = KomiTextRole.Label,
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                uppercase = false,
                modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
            )
            DesktopSection.entries.forEach { entry ->
                NavRow(
                    label = entry.label(),
                    jp = entry.jp,
                    active = section == entry,
                    onClick = { section = entry },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .gridPaper(colors.onSurface, colors.gridOpacity)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (section) {
                    DesktopSection.LOOK ->
                        lookAndFeelSection(
                            state = state,
                            onAction = onAction,
                            personality = personality,
                            accent = accent,
                            onPersonalitySelected = onPersonalitySelected,
                            onAccentSelected = onAccentSelected,
                            currentLanguageLabel = currentLanguageLabel,
                            onOpenLanguage = onOpenLanguage,
                        )

                    DesktopSection.CONNECTIVITY ->
                        connectivitySection(
                            state = state,
                            onAction = onAction,
                            onNavigateToMirrorPicker = onNavigateToMirrorPicker,
                        )

                    DesktopSection.PRIVACY ->
                        privacySection(
                            state = state,
                            onAction = onAction,
                            onNavigateToHiddenRepositories = onNavigateToHiddenRepositories,
                            onNavigateToHostTokens = onNavigateToHostTokens,
                        )

                    DesktopSection.APP -> appSection(onOpenFeedback = onOpenFeedback)
                }
            }
        }
    }
}

@Composable
private fun NavRow(
    label: String,
    jp: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPersonality.current.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .then(
                    if (active) {
                        Modifier
                            .hardShadow(DpOffset(3.dp, 3.dp), colors.shadow)
                            .background(colors.primary)
                            .border(2.5.dp, colors.outline)
                    } else {
                        Modifier
                    },
                )
                .height(40.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KomiText(
            text = label,
            role = KomiTextRole.Stamp,
            color = if (active) colors.onPrimary else colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        KomiText(
            text = jp,
            role = KomiTextRole.Label,
            color = if (active) colors.onPrimary else colors.onSurfaceVariant,
            fontSize = 9.5.sp,
            uppercase = false,
        )
    }
}
