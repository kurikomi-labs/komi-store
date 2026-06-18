package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.isDesktop
import zed.rainxch.core.domain.model.appearance.FontTheme
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.ToggleSettingCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun appearanceSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KomiText(role = KomiTextRole.Title, text = stringResource(Res.string.appearance_section_mode))
        Spacer(Modifier.height(8.dp))

        ModeTiles(
            current = isDarkToChoice(state.isDarkTheme),
            paletteForPreview = state.selectedThemeColor,
            onSelected = { onAction(TweaksAction.OnDarkThemeChange(choiceToIsDark(it))) },
        )
        Spacer(Modifier.height(16.dp))

        KomiText(role = KomiTextRole.Title, text = stringResource(Res.string.theme_color))
        Spacer(Modifier.height(8.dp))

        PaletteGrid(
            isDarkTheme = state.isDarkTheme,
            amoledEnabled = state.isAmoledThemeEnabled,
            selected = state.selectedThemeColor,
            onSelected = { onAction(TweaksAction.OnThemeColorSelected(it)) },
        )

        run {
            val systemDark = isSystemInDarkTheme()
            val resolvedDark = state.isDarkTheme ?: systemDark
            AnimatedVisibility(
                visible = resolvedDark,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    ToggleSettingCard(
                        title = stringResource(Res.string.amoled_black_theme),
                        description = stringResource(Res.string.amoled_black_description),
                        checked = state.isAmoledThemeEnabled,
                        onCheckedChange = { onAction(TweaksAction.OnAmoledThemeToggled(it)) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        KomiText(role = KomiTextRole.Title, text = stringResource(Res.string.appearance_section_display))
        Spacer(Modifier.height(8.dp))

        ToggleSettingCard(
            title = stringResource(Res.string.system_font),
            description = stringResource(Res.string.system_font_description),
            checked = state.selectedFontTheme == FontTheme.SYSTEM,
            onCheckedChange = { enabled ->
                onAction(
                    TweaksAction.OnFontThemeSelected(
                        if (enabled) FontTheme.SYSTEM else FontTheme.CUSTOM,
                    ),
                )
            },
        )

        if (isDesktop()) {
            Spacer(Modifier.height(8.dp))
            ToggleSettingCard(
                title = stringResource(Res.string.scrollbar_option_title),
                description = stringResource(Res.string.scrollbar_option_description),
                checked = state.isScrollbarEnabled,
                onCheckedChange = { onAction(TweaksAction.OnScrollbarToggled(it)) },
            )

            Spacer(Modifier.height(8.dp))
            ContentWidthCard(
                selected = state.contentWidth,
                onSelected = { onAction(TweaksAction.OnContentWidthSelected(it)) },
            )
        }
    }
}
