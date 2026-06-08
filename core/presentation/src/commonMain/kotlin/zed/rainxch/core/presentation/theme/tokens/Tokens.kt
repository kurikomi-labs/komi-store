package zed.rainxch.core.presentation.theme.tokens

import androidx.compose.ui.graphics.Color

object Tokens {
    enum class Palette { DYNAMIC, NORD, CREAM, FOREST, PLUM }

    enum class Mode { LIGHT, DARK, AMOLED }

    data class PaletteColors(
        val bg: Color,
        val surface: Color,
        val surface2: Color,
        val ink: Color,
        val ink2: Color,
        val outline: Color,
        val primary: Color,
        val tintP: Color,
        val success: Color,
        val successT: Color,
        val danger: Color,
        val dangerT: Color,
    )

    object Nord {
        val light = PaletteColors(
            bg = Color(0xFFECEFF4),
            surface = Color(0xFFF8FAFC),
            surface2 = Color(0xFFE5E9F0),
            ink = Color(0xFF2E3440),
            ink2 = Color(0xFF4C566A),
            outline = Color(0xFFD8DEE9),
            primary = Color(0xFF5E81AC),
            tintP = Color(0xFFD8E1EC),
            success = Color(0xFFA3BE8C),
            successT = Color(0xFFE1ECCF),
            danger = Color(0xFFBF616A),
            dangerT = Color(0xFFF2D6D7),
        )
        val dark = PaletteColors(
            bg = Color(0xFF242933),
            surface = Color(0xFF2E3440),
            surface2 = Color(0xFF3B4252),
            ink = Color(0xFFECEFF4),
            ink2 = Color(0xFFB8C0CC),
            outline = Color(0xFF3B4252),
            primary = Color(0xFF88C0D0),
            tintP = Color(0xFF3B4252),
            success = Color(0xFFA3BE8C),
            successT = Color(0xFF3F4D3A),
            danger = Color(0xFFBF616A),
            dangerT = Color(0xFF4D2F32),
        )
        val amoled = dark.copy(
            bg = Color(0xFF000000),
            surface = Color(0xFF0B0F14),
            surface2 = Color(0xFF161B22),
        )
    }

    object Cream {
        val light = PaletteColors(
            bg = Color(0xFFFBEFD8),
            surface = Color(0xFFFFF9EC),
            surface2 = Color(0xFFF3E4C6),
            ink = Color(0xFF2B1F14),
            ink2 = Color(0xFF7A6549),
            outline = Color(0xFFE6D5B8),
            primary = Color(0xFFB8542C),
            tintP = Color(0xFFFFE7CB),
            success = Color(0xFF7B8E4A),
            successT = Color(0xFFE0E5CB),
            danger = Color(0xFFB83A2C),
            dangerT = Color(0xFFF3D7CF),
        )
        val dark = PaletteColors(
            bg = Color(0xFF241910),
            surface = Color(0xFF332419),
            surface2 = Color(0xFF473324),
            ink = Color(0xFFFBEFD8),
            ink2 = Color(0xFFC7B196),
            outline = Color(0xFF473324),
            primary = Color(0xFFE89968),
            tintP = Color(0xFF473324),
            success = Color(0xFFA3BE8C),
            successT = Color(0xFF3F4D3A),
            danger = Color(0xFFD26B5A),
            dangerT = Color(0xFF4D2922),
        )
        val amoled = dark.copy(
            bg = Color(0xFF000000),
            surface = Color(0xFF120A05),
            surface2 = Color(0xFF1F140C),
        )
    }

    object Forest {
        val light = PaletteColors(
            bg = Color(0xFFEDF1E8),
            surface = Color(0xFFF7FAF3),
            surface2 = Color(0xFFDDE5D2),
            ink = Color(0xFF2D3A2C),
            ink2 = Color(0xFF5A6A57),
            outline = Color(0xFFCFD9C2),
            primary = Color(0xFF6B8E5A),
            tintP = Color(0xFFDCE7CE),
            success = Color(0xFF6B8E5A),
            successT = Color(0xFFDCE7CE),
            danger = Color(0xFFB83A2C),
            dangerT = Color(0xFFF3D6D2),
        )
        val dark = PaletteColors(
            bg = Color(0xFF1D241D),
            surface = Color(0xFF272F26),
            surface2 = Color(0xFF363F35),
            ink = Color(0xFFEDF1E8),
            ink2 = Color(0xFFB5C2AE),
            outline = Color(0xFF363F35),
            primary = Color(0xFFA3BE8C),
            tintP = Color(0xFF363F35),
            success = Color(0xFFA3BE8C),
            successT = Color(0xFF3D4D3A),
            danger = Color(0xFFD26B5A),
            dangerT = Color(0xFF4D2922),
        )
        val amoled = dark.copy(
            bg = Color(0xFF000000),
            surface = Color(0xFF0A0F0A),
            surface2 = Color(0xFF131A12),
        )
    }

    object Plum {
        val light = PaletteColors(
            bg = Color(0xFFEDE8EF),
            surface = Color(0xFFF7F2F9),
            surface2 = Color(0xFFDCD5E0),
            ink = Color(0xFF322A36),
            ink2 = Color(0xFF5C546A),
            outline = Color(0xFFCFC8D6),
            primary = Color(0xFF7E6BA8),
            tintP = Color(0xFFDCD7E7),
            success = Color(0xFF7B8E4A),
            successT = Color(0xFFDBE5C7),
            danger = Color(0xFFB83A2C),
            dangerT = Color(0xFFF3D6D2),
        )
        val dark = PaletteColors(
            bg = Color(0xFF1D1A22),
            surface = Color(0xFF26222D),
            surface2 = Color(0xFF34303D),
            ink = Color(0xFFEDE8EF),
            ink2 = Color(0xFFB5ACBC),
            outline = Color(0xFF34303D),
            primary = Color(0xFFB39CD4),
            tintP = Color(0xFF34303D),
            success = Color(0xFFA3BE8C),
            successT = Color(0xFF3F4D3A),
            danger = Color(0xFFD26B5A),
            dangerT = Color(0xFF4D2922),
        )
        val amoled = dark.copy(
            bg = Color(0xFF000000),
            surface = Color(0xFF0C0A10),
            surface2 = Color(0xFF161320),
        )
    }

    fun palette(p: Palette, m: Mode): PaletteColors = when (p) {
        Palette.DYNAMIC -> when (m) {
            Mode.LIGHT -> Nord.light
            Mode.DARK -> Nord.dark
            Mode.AMOLED -> Nord.amoled
        }
        Palette.NORD -> when (m) {
            Mode.LIGHT -> Nord.light
            Mode.DARK -> Nord.dark
            Mode.AMOLED -> Nord.amoled
        }
        Palette.CREAM -> when (m) {
            Mode.LIGHT -> Cream.light
            Mode.DARK -> Cream.dark
            Mode.AMOLED -> Cream.amoled
        }
        Palette.FOREST -> when (m) {
            Mode.LIGHT -> Forest.light
            Mode.DARK -> Forest.dark
            Mode.AMOLED -> Forest.amoled
        }
        Palette.PLUM -> when (m) {
            Mode.LIGHT -> Plum.light
            Mode.DARK -> Plum.dark
            Mode.AMOLED -> Plum.amoled
        }
    }

    object Status {
        object Freshness {
            val hot = Color(0xFFE07856)
            val fresh = Color(0xFF6BA068)
            val warm = Color(0xFFC49652)
            val cool = Color(0xFF8E8E8E)
            val dormant = Color(0xFF9E9E9E)
        }
        object Wax {
            val intact = Color(0xFF8B4A2B)
            val cracked = Color(0xFFB83A2C)
            val open = Color(0xFF8E8E8E)
        }
        object Perm {
            val low = Color(0xFF6BA068)
            val moderate = Color(0xFFC49652)
            val high = Color(0xFFB83A2C)
        }
        object Trend {
            val rising = Color(0xFF6BA068)
            val flat = Color(0xFF8E8E8E)
            val falling = Color(0xFFB83A2C)
        }
        object Star {
            val activeLight = Color(0xFFC49652)
            val activeDark = Color(0xFFD6AB6A)
        }
        object IssueState {
            val openLight = Color(0xFF2DA44E)
            val openDark = Color(0xFF3FB950)
            val closedLight = Color(0xFF8957E5)
            val closedDark = Color(0xFFA371F7)
            val prClosedLight = Color(0xFFCF222E)
            val prClosedDark = Color(0xFFF85149)
        }
        object Severity {
            val criticalLight = Color(0xFFCF222E)
            val criticalDark = Color(0xFFF85149)
            val highLight = Color(0xFFBC4C00)
            val highDark = Color(0xFFDB6D28)
            val mediumLight = Color(0xFF9A6700)
            val mediumDark = Color(0xFFD29922)
            val lowLight = Color(0xFF1A7F37)
            val lowDark = Color(0xFF3FB950)
            val unknownLight = Color(0xFF6E7781)
            val unknownDark = Color(0xFF8B949E)
        }
        object Method {
            val readyLight = Color(0xFF4CAF50)
            val readyDark = Color(0xFF4CAF50)
            val warningLight = Color(0xFFFF9800)
            val warningDark = Color(0xFFFF9800)
            val errorLight = Color(0xFFFF5722)
            val errorDark = Color(0xFFFF5722)
        }
        object Protection {
            val signatureLight = Color(0xFFB87100)
            val signatureDark = Color(0xFFB87100)
            val privilegedLight = Color(0xFF8E4900)
            val privilegedDark = Color(0xFF8E4900)
        }
    }

    object Thresholds {
        data class FreshnessBucket(
            val maxDaysInclusive: Int?,
            val state: String,
            val ringFraction: Float,
            val color: Color,
        )
        val freshness = listOf(
            FreshnessBucket(3, "hot", 1.00f, Status.Freshness.hot),
            FreshnessBucket(30, "fresh", 0.78f, Status.Freshness.fresh),
            FreshnessBucket(90, "warm", 0.55f, Status.Freshness.warm),
            FreshnessBucket(365, "cool", 0.30f, Status.Freshness.cool),
            FreshnessBucket(null, "dormant", 0.12f, Status.Freshness.dormant),
        )

        data class MaintenanceBucket(
            val maxDaysInclusive: Int?,
            val state: String,
            val heartbeatPeriodMs: Int?,
            val color: Color,
        )
        val maintenance = listOf(
            MaintenanceBucket(1, "active", 1400, Status.Freshness.fresh),
            MaintenanceBucket(7, "recent", 2400, Status.Freshness.fresh),
            MaintenanceBucket(30, "quiet", 4200, Status.Freshness.warm),
            MaintenanceBucket(null, "dormant", null, Status.Freshness.dormant),
        )
    }

    object Motion {
        const val tapHighlightMs = 120
        const val paletteCrossfadeMs = 250
        const val sheetSlideMs = 240
        const val scrimFadeMs = 180
        const val toastSlideMs = 200
        const val toastFadeMs = 240
        const val heartbeatScaleFrom = 1.0f
        const val heartbeatScaleTo = 1.25f
        const val heartbeatHaloFromScale = 1.0f
        const val heartbeatHaloToScale = 2.4f
        const val heartbeatHaloFromAlpha = 0.45f
        const val heartbeatHaloToAlpha = 0.0f
    }

    object Spacing {
        val xs = 4
        val sm = 8
        val md = 12
        val lg = 16
        val xl = 24
        val xxl = 32
    }

    object Topics {
        val supported = setOf(
            "security", "privacy", "networking", "ai", "notes",
            "audio", "video", "photo", "reader",
            "messaging", "browser", "self-hosted", "backup",
            "social", "launcher",
        )
    }

    object Licenses {
        val copyleft = setOf(
            "AGPL-3.0", "GPL-3.0", "GPL-2.0", "LGPL-3.0", "LGPL-2.1", "MPL-2.0",
        )
        val permissive = setOf(
            "MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "Unlicense",
        )
    }
}
