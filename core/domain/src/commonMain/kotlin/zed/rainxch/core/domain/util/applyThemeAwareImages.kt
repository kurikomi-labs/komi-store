package zed.rainxch.core.domain.util

fun applyThemeAwareImages(
    content: String,
    isDark: Boolean,
): String {
    if (content.isEmpty()) return content
    var processed = content

    // Strip markdown-image entries whose URL has `#gh-dark-mode-only`
    // or `#gh-light-mode-only` and we're on the OTHER theme. GitHub's
    // own renderer hides these on the mismatched theme; emulating that
    // keeps READMEs from showing both light + dark variants stacked.
    processed = processed.replace(
        Regex("""!\[([^\]]*)\]\(([^)]*?)#gh-(dark|light)-mode-only([^)]*)\)"""),
    ) { match ->
        val mode = match.groupValues[3]
        if ((isDark && mode == "light") || (!isDark && mode == "dark")) {
            "" // drop entirely; alt text would just be noise here
        } else {
            // Strip the fragment so the URL is clean for Coil's cache key.
            val alt = match.groupValues[1]
            val urlBase = match.groupValues[2]
            val trailing = match.groupValues[4]
            "![$alt]($urlBase$trailing)"
        }
    }

    // Same for raw HTML <img> tags.
    processed = processed.replace(
        Regex(
            """<img(\s[^>]*?)src\s*=\s*(["'])([^"']*?)#gh-(dark|light)-mode-only([^"']*?)\2([^>]*?)>""",
            RegexOption.IGNORE_CASE,
        ),
    ) { match ->
        val mode = match.groupValues[4]
        if ((isDark && mode == "light") || (!isDark && mode == "dark")) {
            ""
        } else {
            val pre = match.groupValues[1]
            val quote = match.groupValues[2]
            val urlBase = match.groupValues[3]
            val trailing = match.groupValues[5]
            val post = match.groupValues[6]
            "<img${pre}src=$quote$urlBase$trailing$quote$post>"
        }
    }

    return processed
}
