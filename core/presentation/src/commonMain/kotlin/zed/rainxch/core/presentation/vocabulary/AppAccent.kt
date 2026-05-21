package zed.rainxch.core.presentation.vocabulary

import androidx.compose.ui.graphics.Color

/**
 * Per-app accent triple (DESIGN.md §2.4). The accent travels with the repo across
 * surfaces (lead card bg tint, freshness ring outer, install panel bloom).
 *
 *   - `c`   — saturated accent (the brand color)
 *   - `lt`  — light tint (used in all light palettes as a 18–22% bg)
 *   - `dtAlpha` — alpha applied to `c` over the dark surface (22% per themes.md)
 */
data class AppAccent(val c: Color, val lt: Color, val dtAlpha: Float = 0.22f) {
    fun tintFor(isDark: Boolean): Color = if (isDark) c.copy(alpha = dtAlpha) else lt
}

/**
 * Resolves a per-app accent from (in order):
 *   1. Backend-supplied hex (when [backendHex] is non-null and parseable)
 *   2. Topic-derived (first match in [TOPIC_ACCENTS])
 *   3. Language-derived ([LANGUAGE_ACCENTS])
 *   4. Blue fallback (Nord primary)
 *
 * Source-of-truth tables in UI-SPEC.md §6.1 / §6.2. Deterministic — same repo
 * resolves to the same accent across launches and devices.
 */
object AppAccentResolver {
    private val FALLBACK = AppAccent(c = Color(0xFF5E81AC), lt = Color(0xFFD8E1EC))

    private val TOPIC_ACCENTS: Map<String, AppAccent> = buildMap {
        // Each pair is (canonical topic name → accent). Aliases listed inline.
        val photo = AppAccent(Color(0xFF5E81AC), Color(0xFFD8E1EC))
        listOf("photo", "photos", "gallery").forEach { put(it, photo) }

        val manga = AppAccent(Color(0xFF7E6BA8), Color(0xFFDCD7E7))
        listOf("manga", "comic", "reader").forEach { put(it, manga) }

        val security = AppAccent(Color(0xFF4C6E96), Color(0xFFCFDAE7))
        listOf("password-manager", "security", "vault").forEach { put(it, security) }

        val audio = AppAccent(Color(0xFF6B8E5A), Color(0xFFDCE7CE))
        listOf("podcast", "audio", "music").forEach { put(it, audio) }

        val book = AppAccent(Color(0xFF9B6B3C), Color(0xFFEDD9BB))
        listOf("book", "ebook", "koreader").forEach { put(it, book) }

        val messaging = AppAccent(Color(0xFFA35365), Color(0xFFEFCDD3))
        listOf("messaging", "chat", "signal").forEach { put(it, messaging) }

        val vpn = AppAccent(Color(0xFF5C7A8E), Color(0xFFD5DEE5))
        listOf("vpn", "network", "proxy").forEach { put(it, vpn) }

        val notes = AppAccent(Color(0xFF7A6549), Color(0xFFE5D9C6))
        listOf("note", "notes", "markdown").forEach { put(it, notes) }

        val backup = AppAccent(Color(0xFF5A6A57), Color(0xFFCBD3C9))
        listOf("backup", "sync").forEach { put(it, backup) }

        val selfHosted = AppAccent(Color(0xFF356859), Color(0xFFB8E0D2))
        listOf("self-hosted", "home-server").forEach { put(it, selfHosted) }

        val video = AppAccent(Color(0xFFB8542C), Color(0xFFFFE7CB))
        listOf("video", "media").forEach { put(it, video) }
    }

    private val LANGUAGE_ACCENTS: Map<String, AppAccent> = mapOf(
        "Kotlin" to AppAccent(Color(0xFF7E6BA8), Color(0xFFDCD7E7)),
        "Java" to AppAccent(Color(0xFFB8542C), Color(0xFFFFE7CB)),
        "TypeScript" to AppAccent(Color(0xFF5E81AC), Color(0xFFD8E1EC)),
        "JavaScript" to AppAccent(Color(0xFF5E81AC), Color(0xFFD8E1EC)),
        "Python" to AppAccent(Color(0xFF356859), Color(0xFFB8E0D2)),
        "Rust" to AppAccent(Color(0xFFA35346), Color(0xFFEDCFC9)),
        "Go" to AppAccent(Color(0xFF5C7A8E), Color(0xFFD5DEE5)),
        "C" to AppAccent(Color(0xFF7A6549), Color(0xFFE5D9C6)),
        "C++" to AppAccent(Color(0xFF7A6549), Color(0xFFE5D9C6)),
        "Swift" to AppAccent(Color(0xFFB8542C), Color(0xFFFFE7CB)),
        "Dart" to AppAccent(Color(0xFF5E81AC), Color(0xFFD8E1EC)),
        "Ruby" to AppAccent(Color(0xFFB83A2C), Color(0xFFF3D7CF)),
        "Shell" to AppAccent(Color(0xFF6B8E5A), Color(0xFFDCE7CE)),
        "Bash" to AppAccent(Color(0xFF6B8E5A), Color(0xFFDCE7CE)),
    )

    fun resolve(
        backendHex: String? = null,
        topics: List<String> = emptyList(),
        primaryLanguage: String? = null,
    ): AppAccent {
        // 1. Backend
        backendHex?.let { parseHexAccent(it) }?.let { return it }
        // 2. Topic
        topics.forEach { t ->
            TOPIC_ACCENTS[t.lowercase()]?.let { return it }
        }
        // 3. Language
        primaryLanguage?.let { LANGUAGE_ACCENTS[it] }?.let { return it }
        // 4. Fallback
        return FALLBACK
    }

    private fun parseHexAccent(hex: String): AppAccent? {
        val cleaned = hex.removePrefix("#").trim()
        if (cleaned.length != 6 && cleaned.length != 8) return null
        val rgb = runCatching {
            val intVal = cleaned.toLong(16)
            if (cleaned.length == 6) (0xFF000000 or intVal).toULong() else intVal.toULong()
        }.getOrNull() ?: return null
        val c = Color(rgb.toLong())
        // Synthetic light tint = c blended toward white at 78%
        val lt = blendTowardWhite(c, 0.78f)
        return AppAccent(c, lt)
    }

    private fun blendTowardWhite(c: Color, factor: Float): Color = Color(
        red = c.red + (1f - c.red) * factor,
        green = c.green + (1f - c.green) * factor,
        blue = c.blue + (1f - c.blue) * factor,
        alpha = c.alpha,
    )
}
