package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.runtime.Composable
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.model.PersonalityDecor

enum class TweaksDecorSlot {
    Settings,
    LookAndFeel,
    Connectivity,
    InstallsUpdates,
    PrivacyData,
    App,
    Personality,
    Accent,
    Mirror,
    MirrorOfficial,
    MirrorCommunity,
    Hidden,
    Skipped,
    Tokens,
}

@Composable
fun tweaksKicker(slot: TweaksDecorSlot): String? =
    when (LocalPersonality.current.decor) {
        PersonalityDecor.None -> null
        PersonalityDecor.MangaKicker -> mangaKicker(slot)
    }

@Composable
fun personalityUsesDecor(): Boolean = LocalPersonality.current.decor != PersonalityDecor.None

private fun mangaKicker(slot: TweaksDecorSlot): String =
    when (slot) {
        TweaksDecorSlot.Settings -> "設定 · SETTINGS"
        TweaksDecorSlot.LookAndFeel -> "外観"
        TweaksDecorSlot.Connectivity -> "接続"
        TweaksDecorSlot.InstallsUpdates -> "導入"
        TweaksDecorSlot.PrivacyData -> "個人情報"
        TweaksDecorSlot.App -> "アプリ"
        TweaksDecorSlot.Personality -> "個性"
        TweaksDecorSlot.Accent -> "色"
        TweaksDecorSlot.Mirror -> "ミラー"
        TweaksDecorSlot.MirrorOfficial -> "公式"
        TweaksDecorSlot.MirrorCommunity -> "有志"
        TweaksDecorSlot.Hidden -> "非表示"
        TweaksDecorSlot.Skipped -> "スキップ"
        TweaksDecorSlot.Tokens -> "トークン"
    }
