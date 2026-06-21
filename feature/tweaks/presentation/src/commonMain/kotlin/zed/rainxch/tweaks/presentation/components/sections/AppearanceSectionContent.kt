package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.isDesktop
import zed.rainxch.core.domain.model.appearance.ContentWidth
import zed.rainxch.core.presentation.components.inputs.KomiSwitch
import zed.rainxch.core.presentation.utils.displayName
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.shell.SettingsGroup
import zed.rainxch.tweaks.presentation.components.shell.SettingsRow
import zed.rainxch.tweaks.presentation.components.shell.SettingsSegment
import zed.rainxch.tweaks.presentation.components.shell.SettingsSegmented

private enum class ModeChoice { LIGHT, DARK, SYSTEM }

private fun isDarkToChoice(value: Boolean?): ModeChoice =
    when (value) {
        true -> ModeChoice.DARK
        false -> ModeChoice.LIGHT
        null -> ModeChoice.SYSTEM
    }

private fun choiceToIsDark(choice: ModeChoice): Boolean? =
    when (choice) {
        ModeChoice.DARK -> true
        ModeChoice.LIGHT -> false
        ModeChoice.SYSTEM -> null
    }

@Composable
fun appearanceSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val desktop = isDesktop()
    SettingsGroup(modifier = modifier) {
        SettingsRow(
            title = stringResource(Res.string.appearance_section_mode),
            trailing = {
                SettingsSegmented(
                    value = isDarkToChoice(state.isDarkTheme),
                    small = true,
                    onSelect = { onAction(TweaksAction.OnDarkThemeChange(choiceToIsDark(it))) },
                    options =
                        listOf(
                            SettingsSegment(ModeChoice.LIGHT, stringResource(Res.string.theme_light)),
                            SettingsSegment(ModeChoice.DARK, stringResource(Res.string.theme_dark)),
                            SettingsSegment(ModeChoice.SYSTEM, stringResource(Res.string.theme_system)),
                        ),
                )
            },
        )
        SettingsRow(
            title = stringResource(Res.string.amoled_black_theme),
            subtitle = stringResource(Res.string.amoled_black_description),
            last = !desktop,
            trailing = {
                KomiSwitch(
                    checked = state.isAmoledThemeEnabled,
                    onCheckedChange = { onAction(TweaksAction.OnAmoledThemeToggled(it)) },
                )
            },
        )
        if (desktop) {
            SettingsRow(
                title = stringResource(Res.string.scrollbar_option_title),
                subtitle = stringResource(Res.string.scrollbar_option_description),
                trailing = {
                    KomiSwitch(
                        checked = state.isScrollbarEnabled,
                        onCheckedChange = { onAction(TweaksAction.OnScrollbarToggled(it)) },
                    )
                },
            )
            SettingsRow(
                title = stringResource(Res.string.content_width_title),
                subtitle = stringResource(Res.string.content_width_description),
                last = true,
                trailing = {
                    SettingsSegmented(
                        value = state.contentWidth,
                        small = true,
                        onSelect = { onAction(TweaksAction.OnContentWidthSelected(it)) },
                        options = ContentWidth.entries.map { SettingsSegment(it, it.displayName) },
                    )
                },
            )
        }
    }
}
