package zed.rainxch.details.data.utils

fun preprocessMarkdown(
    markdown: String,
    baseUrl: String,
): String {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    var processed = markdown

    fun normalizeGitHubUrl(url: String): String =
        if (url.contains("github.com") && url.contains("/blob/")) {
            url
                .replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
        } else {
            url
        }

    fun isSvgUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".svg") ||
            lower.contains(".svg?") ||
            lower.contains(".svg#") ||
            lower.contains("/svg-badge") ||
            lower.contains("badge.svg")
    }

    fun isBadgeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("img.shields.io") ||
            lower.contains("shields.io/badge") ||
            lower.contains("badge.fury.io") ||
            lower.contains("badgen.net") ||
            lower.contains("repology.org/badge") ||
            lower.contains("hosted.weblate.org/widget") ||
            lower.contains("codecov.io") ||
            lower.contains("coveralls.io") ||
            lower.contains("travis-ci.") ||
            lower.contains("circleci.com") ||
            lower.contains("github.com/workflows") ||
            (lower.contains("/badge") && isSvgUrl(lower))
    }

    // SVGs are no longer skipped wholesale — Coil's SVG decoder now
    // handles them (registered in the App composable). Only known badge
    // / shields services stay skipped because they're noise even when
    // rendered correctly (status badges = clutter on small screens).
    fun shouldSkipImage(url: String): Boolean = isBadgeUrl(url)

    fun resolveUrl(path: String): String {
        val trimmed = path.trim()
        val isAbsolute =
            trimmed.startsWith("http://") ||
                trimmed.startsWith("https://") ||
                trimmed.startsWith("data:")
        return if (isAbsolute) {
            normalizeGitHubUrl(trimmed)
        } else {
            when {
                trimmed.startsWith("./") -> {
                    "$normalizedBaseUrl${trimmed.removePrefix("./")}"
                }

                trimmed.startsWith("/") -> {
                    "$normalizedBaseUrl${trimmed.removePrefix("/")}"
                }

                trimmed.startsWith("../") -> {
                    var base = normalizedBaseUrl.trimEnd('/')
                    var rel = trimmed
                    while (rel.startsWith("../")) {
                        base = base.substringBeforeLast('/', base)
                        rel = rel.removePrefix("../")
                    }
                    "$base/$rel"
                }

                else -> {
                    "$normalizedBaseUrl$trimmed"
                }
            }
        }
    }

    // ========================================================================
    // Phase 0: Handle reference-style markdown definitions and usages
    // ========================================================================
    // Reference definitions: [ref-name]: https://example.com/image.svg
    // Reference usages: ![alt][ref-name] or [![img-ref]][link-ref]

    // 0a. Parse all reference definitions
    val refDefinitionRegex =
        Regex(
            """^\[([^\]]+)\]:\s*(\S+).*$""",
            RegexOption.MULTILINE,
        )
    val referenceMap = mutableMapOf<String, String>()
    for (match in refDefinitionRegex.findAll(processed)) {
        val refName = match.groupValues[1].lowercase()
        val url = match.groupValues[2]
        referenceMap[refName] = url
    }

    // 0b. Identify which references point to SVGs/badges
    val skipRefNames =
        referenceMap
            .filter { (_, url) ->
                shouldSkipImage(resolveUrl(url))
            }.keys

    // 0c. Remove reference-style image usages that point to SVGs: ![alt][svg-ref]
    if (skipRefNames.isNotEmpty()) {
        processed =
            processed.replace(
                Regex("""!\[([^\]]*)\]\[([^\]]+)\]"""),
            ) { match ->
                val alt = match.groupValues[1]
                val refName = match.groupValues[2].lowercase()
                if (refName in skipRefNames) {
                    if (alt.isNotEmpty()) "**$alt**" else ""
                } else {
                    match.value
                }
            }
    }

    // 0d. Resolve remaining reference-style images to inline format: ![alt][ref] → ![alt](url)
    processed =
        processed.replace(
            Regex("""!\[([^\]]*)\]\[([^\]]+)\]"""),
        ) { match ->
            val alt = match.groupValues[1]
            val refName = match.groupValues[2].lowercase()
            val url = referenceMap[refName]
            if (url != null) {
                val resolved = resolveUrl(url)
                "![$alt]($resolved)"
            } else {
                match.value
            }
        }

    // 0e. Handle nested badge-as-link patterns: [![badge-ref]][link-ref]
    // After 0c strips the inner image, this can leave [**text**][link-ref] or [][link-ref]
    processed =
        processed.replace(
            Regex("""\[(\*\*[^*]*\*\*)\]\[([^\]]+)\]"""),
        ) { match ->
            val boldText = match.groupValues[1]
            val refName = match.groupValues[2].lowercase()
            val url = referenceMap[refName]
            if (url != null) {
                "[$boldText](${resolveUrl(url)})"
            } else {
                boldText
            }
        }
    // Clean empty bracket patterns left from stripped badge images: [][ref]
    processed =
        processed.replace(
            Regex("""\[\s*\]\[([^\]]+)\]"""),
            "",
        )

    // 0f. Handle reference-style links: [text][ref] → [text](url)
    processed =
        processed.replace(
            Regex("""\[([^\]]+)\]\[([^\]]+)\]"""),
        ) { match ->
            val text = match.groupValues[1]
            val refName = match.groupValues[2].lowercase()
            val url = referenceMap[refName]
            // Don't convert if text looks like it was already an image (starts with !)
            if (url != null && !text.startsWith("!")) {
                "[$text](${resolveUrl(url)})"
            } else {
                match.value
            }
        }

    // 0g. Remove all reference definitions that were resolved
    processed =
        processed.replace(
            Regex("""^\[([^\]]+)\]:\s*\S+.*$""", RegexOption.MULTILINE),
        ) { match ->
            val refName = match.groupValues[1].lowercase()
            if (refName in referenceMap) "" else match.value
        }

    // ========================================================================
    // Phase 1: HTML → Markdown conversions
    // ========================================================================

    // 1. Unwrap <picture> elements → keep only the <img> fallback
    processed =
        processed.replace(
            Regex(
                """<picture[^>]*>.*?(<img\s[^>]*?>).*?</picture>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            match.groupValues[1]
        }
    // Also strip orphaned <source> tags (outside <picture>)
    processed =
        processed.replace(
            Regex("""<source\s[^>]*?/?>""", RegexOption.IGNORE_CASE),
            "",
        )

    // 2. Unwrap <a> tags that wrap <img> tags — keep the <img> for step 3
    processed =
        processed.replace(
            Regex(
                """<a\s[^>]*?>\s*(<img\s[^>]*?>)\s*</a>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            match.groupValues[1]
        }

    // 3. Convert <img> tags → markdown images (handles multiline img tags)
    processed =
        processed.replace(
            Regex(
                """<img\s+([^>]*?)\s*/?>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { imgMatch ->
            val imgTag = imgMatch.groupValues[1]

            val srcMatch = Regex("""src\s*=\s*(["'])([^"']+)\1""").find(imgTag)
            val src = srcMatch?.groupValues?.get(2) ?: ""

            val altMatch = Regex("""alt\s*=\s*(["'])([^"']*)\1""").find(imgTag)
            val alt = altMatch?.groupValues?.get(2) ?: ""

            if (src.isNotEmpty()) {
                val normalizedSrc = resolveUrl(src)

                if (shouldSkipImage(normalizedSrc)) {
                    if (alt.isNotEmpty()) "**$alt**" else ""
                } else {
                    "![$alt]($normalizedSrc)"
                }
            } else {
                ""
            }
        }

    // 4. Normalize markdown image URLs (resolve relative, normalize GitHub blob)
    processed =
        processed.replace(
            Regex("""!\[([^\]]*)\]\(([^)]+)\)"""),
        ) { match ->
            val alt = match.groupValues[1]
            val originalPath = match.groupValues[2].trim()
            val finalUrl = resolveUrl(originalPath)

            if (shouldSkipImage(finalUrl)) {
                if (alt.isNotEmpty()) "**$alt**" else ""
            } else {
                "![$alt]($finalUrl)"
            }
        }

    // 5. Handle <video> tags → markdown link or remove
    processed =
        processed.replace(
            Regex(
                """<video[^>]*?\ssrc=(["'])([^"']+)\1[^>]*>.*?</video>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val src = match.groupValues[2]
            "[Video](${resolveUrl(src)})"
        }
    // Video with <source> inside
    processed =
        processed.replace(
            Regex(
                """<video[^>]*>.*?<source\s[^>]*?\ssrc=(["'])([^"']+)\1[^>]*?>.*?</video>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val src = match.groupValues[2]
            "[Video](${resolveUrl(src)})"
        }

    // 6. Convert HTML headings <h1>–<h6> → markdown headings
    for (level in 1..6) {
        val hashes = "#".repeat(level)
        processed =
            processed.replace(
                Regex(
                    """<h$level[^>]*>(.*?)</h$level>""",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                ),
            ) { match ->
                val content = match.groupValues[1].trim()
                "\n$hashes $content\n"
            }
    }

    // 7. Convert <br> and <hr> tags
    processed =
        processed.replace(
            Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE),
            "\n",
        )
    processed =
        processed.replace(
            Regex("""<hr\s*/?>""", RegexOption.IGNORE_CASE),
            "\n---\n",
        )

    // 8. Convert inline formatting tags
    // <b> / <strong> → **text**
    processed =
        processed.replace(
            Regex(
                """<(b|strong)>(.*?)</\1>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            "**${match.groupValues[2]}**"
        }
    // <i> / <em> → *text*
    processed =
        processed.replace(
            Regex(
                """<(i|em)>(.*?)</\1>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            "*${match.groupValues[2]}*"
        }
    // <pre><code class="language-XYZ"> → ``` fence with language hint.
    // Must run BEFORE the single-line <code> rule below — that one would
    // otherwise grab the inner <code> and lose the language attribute.
    processed =
        processed.replace(
            Regex(
                """<pre[^>]*>\s*<code(?:\s+[^>]*?class\s*=\s*["'][^"']*?language-(\w+)[^"']*?["'])?[^>]*>(.*?)</code>\s*</pre>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val lang = match.groupValues[1]
            val code = match.groupValues[2]
            "\n```$lang\n$code\n```\n"
        }
    // <code> → `text` (single-line only, not <pre><code>)
    processed =
        processed.replace(
            Regex(
                """<code>([^<]*?)</code>""",
                RegexOption.IGNORE_CASE,
            ),
        ) { match ->
            "`${match.groupValues[1]}`"
        }
    // <blockquote>X</blockquote> → markdown `> ` lines.
    processed =
        processed.replace(
            Regex(
                """<blockquote[^>]*>(.*?)</blockquote>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val body = match.groupValues[1].trim()
            body.lineSequence().joinToString("\n") { "> $it" }
        }
    // <s> / <del> / <strike> → ~~text~~
    processed =
        processed.replace(
            Regex(
                """<(s|del|strike)>(.*?)</\1>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            "~~${match.groupValues[2]}~~"
        }

    // 9. Convert <a href="url">text</a> → [text](url) (non-image links)
    processed =
        processed.replace(
            Regex(
                """<a\s+[^>]*?href\s*=\s*(["'])([^"']+)\1[^>]*>(.*?)</a>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val url = match.groupValues[2]
            val text = match.groupValues[3].trim()
            val resolvedUrl = resolveUrl(url)
            if (text.isEmpty()) {
                "[$resolvedUrl]($resolvedUrl)"
            } else {
                "[$text]($resolvedUrl)"
            }
        }

    // 10. <kbd> → `text`
    processed =
        processed.replace(
            Regex(
                """<kbd>(.*?)</kbd>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            "`${match.groupValues[1]}`"
        }

    // 11. Strip remaining wrapper tags (keep content)
    // <div> tags
    processed =
        processed.replace(
            Regex("""<div[^>]*?>\s*""", RegexOption.IGNORE_CASE),
            "\n\n",
        )
    processed =
        processed.replace(
            Regex("""</div>\s*""", RegexOption.IGNORE_CASE),
            "\n\n",
        )
    // <p> / </p>
    processed =
        processed.replace(
            Regex("""<p[^>]*?>""", RegexOption.IGNORE_CASE),
            "\n",
        )
    processed =
        processed.replace(
            Regex("""</p>""", RegexOption.IGNORE_CASE),
            "\n",
        )
    // <details>…<summary>X</summary>BODY</details> → fenced code block
    // with `ghs-details` info string. Summary text is %-encoded after a
    // `|` so it survives markdown whitespace + special chars. Custom
    // codeFence slot recognises the lang prefix and renders an
    // ExpandableDetails card.
    //
    // Gap between `<details>` and `<summary>` is permissive (`.*?`) —
    // earlier passes have already stripped `<div>` / `<p>` into newlines
    // but may have left trailing whitespace or stray text in between.
    // Padding with blank lines around the fence is required so the
    // intellij-markdown parser recognises it as a real fenced block
    // rather than inline text.
    processed =
        processed.replace(
            Regex(
                """<details\b[^>]*?>.*?<summary[^>]*?>(.*?)</summary>(.*?)</details>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            val summary = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()
            // Inline details (entire match on one line) usually sits
            // inside a GFM table cell. A multi-line fenced block there
            // would terminate the table mid-row. Fall back to a flat
            // `**summary**: body` rendering that stays on one line.
            val isInline = !match.value.contains('\n')
            if (isInline) {
                when {
                    summary.isEmpty() && body.isEmpty() -> ""
                    body.isEmpty() -> "**$summary**"
                    summary.isEmpty() -> body
                    else -> "**$summary**: $body"
                }
            } else {
                val encodedSummary = encodeDetailsSummary(summary)
                "\n\n```ghs-details|$encodedSummary\n$body\n```\n\n"
            }
        }
    // Handle bare/stripped <details> without inner <summary> — keep
    // contents as plain markdown.
    processed =
        processed.replace(
            Regex("""</?details[^>]*?>""", RegexOption.IGNORE_CASE),
            "\n",
        )
    processed =
        processed.replace(
            Regex(
                """<summary[^>]*?>(.*?)</summary>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
        ) { match ->
            "**${match.groupValues[1].trim()}**\n"
        }
    // <sup>X</sup> / <sub>X</sub> → Unicode superscript/subscript chars
    // where mappable (digits + operators + a few letters). Falls back
    // to the literal char when no Unicode codepoint exists — markdown
    // lib doesn't expose inline BaselineShift, so this is the best we
    // can do without a custom text span.
    processed =
        processed.replace(
            Regex("""<sup[^>]*>(.*?)</sup>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        ) { match ->
            match.groupValues[1].map { SUPERSCRIPTS[it] ?: it }.joinToString("")
        }
    processed =
        processed.replace(
            Regex("""<sub[^>]*>(.*?)</sub>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        ) { match ->
            match.groupValues[1].map { SUBSCRIPTS[it] ?: it }.joinToString("")
        }
    // <span> — strip tags, keep content
    processed =
        processed.replace(
            Regex("""</?span[^>]*?>""", RegexOption.IGNORE_CASE),
            "",
        )
    // Strip other common straggler HTML tags
    processed =
        processed.replace(
            Regex(
                """</?(?:center|font|u|section|article|header|footer|nav|main|aside|figure|figcaption)[^>]*?>""",
                RegexOption.IGNORE_CASE,
            ),
            "\n",
        )

    // 12. Decode HTML entities. Named entity table covers the long tail
    //     of READMEs (©, ™, ‘curly quotes’, em/en-dash, ellipsis,
    //     French/Spanish quotation marks, currency symbols).
    HTML_ENTITIES.forEach { (entity, char) -> processed = processed.replace(entity, char) }
    // Numeric HTML entities (decimal): &#NNN; → char.
    processed =
        processed.replace(Regex("""&#(\d+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 32..0x10FFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }
    // Numeric HTML entities (hex): &#xHHHH; → char.
    processed =
        processed.replace(Regex("""&#x([0-9A-Fa-f]+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 32..0x10FFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

    // 13. Clean up empty <p> tags and excess newlines
    processed =
        processed.replace(
            Regex("""<p[^>]*?>\s*</p>""", RegexOption.IGNORE_CASE),
            "",
        )
    processed =
        processed.replace(
            Regex("""\n{3,}"""),
            "\n\n",
        )

    // 14. Clean up orphaned markdown link fragments
    processed =
        processed.replace(
            Regex("""^\]\([^)]+\)""", RegexOption.MULTILINE),
            "",
        )

    // 15. Replace GitHub emoji shortcodes (:rocket: → 🚀). Skips fenced
    //     code blocks; inline `:foo:` patterns inside `` `code` `` are
    //     rare enough not to warrant deeper tokenisation.
    processed = zed.rainxch.core.domain.util.EmojiShortcodes.render(processed)

    // 16. Join consecutive image-only lines into a single paragraph so
    //     badge galleries (Play Store + GitHub buttons etc.) render
    //     in a row instead of stacking one per line.
    processed = joinAdjacentImageLines(processed)

    return processed.trim()
}

private fun encodeDetailsSummary(text: String): String {
    // URL-encode special chars to keep summary on one line inside the
    // fence info string. Decoder mirror lives in the codeFence slot.
    val safe = StringBuilder()
    text.forEach { c ->
        when (c) {
            '\n', '\r', '\t', '`', ' ', '|' -> {
                safe.append("%").append(c.code.toString(16).padStart(2, '0').uppercase())
            }

            else -> safe.append(c)
        }
    }
    return safe.toString()
}

private fun joinAdjacentImageLines(content: String): String {
    val imageOnlyLine =
        Regex("""^\s*(?:!\[[^\]]*]\([^)]+\)\s*){1,}\s*$""")
    val lines = content.split('\n')
    val out = StringBuilder()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (imageOnlyLine.matches(line)) {
            // Greedily collect adjacent image-only lines.
            val group = StringBuilder(line.trim())
            var j = i + 1
            while (j < lines.size && imageOnlyLine.matches(lines[j])) {
                group.append(' ').append(lines[j].trim())
                j++
            }
            out.append(group)
            if (j < lines.size) out.append('\n')
            i = j
        } else {
            out.append(line)
            if (i + 1 < lines.size) out.append('\n')
            i++
        }
    }
    return out.toString()
}

// ============================================================================
// Lookup tables for HTML entity decoding + sub/sup Unicode mapping.
// ============================================================================

private val HTML_ENTITIES: Map<String, String> = mapOf(
    // Core 5 (must come first; processor relies on them being decoded
    // before any subsequent regex that operates on raw `<`/`>`).
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&apos;" to "'",
    "&#39;" to "'",
    // Whitespace
    "&nbsp;" to " ",
    "&ensp;" to " ",
    "&emsp;" to " ",
    "&thinsp;" to " ",
    // Punctuation / typography
    "&hellip;" to "…",
    "&mdash;" to "—",
    "&ndash;" to "–",
    "&laquo;" to "«",
    "&raquo;" to "»",
    "&ldquo;" to "“",
    "&rdquo;" to "”",
    "&lsquo;" to "‘",
    "&rsquo;" to "’",
    "&sbquo;" to "‚",
    "&bdquo;" to "„",
    "&bull;" to "•",
    "&middot;" to "·",
    "&sect;" to "§",
    "&para;" to "¶",
    // Math / arrows
    "&times;" to "×",
    "&divide;" to "÷",
    "&plusmn;" to "±",
    "&deg;" to "°",
    "&micro;" to "µ",
    "&fnof;" to "ƒ",
    "&infin;" to "∞",
    "&asymp;" to "≈",
    "&ne;" to "≠",
    "&le;" to "≤",
    "&ge;" to "≥",
    "&larr;" to "←",
    "&rarr;" to "→",
    "&uarr;" to "↑",
    "&darr;" to "↓",
    "&harr;" to "↔",
    "&lArr;" to "⇐",
    "&rArr;" to "⇒",
    // Legal / brand
    "&copy;" to "©",
    "&reg;" to "®",
    "&trade;" to "™",
    // Currency
    "&euro;" to "€",
    "&pound;" to "£",
    "&yen;" to "¥",
    "&cent;" to "¢",
)

private val SUPERSCRIPTS: Map<Char, Char> = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
    'n' to 'ⁿ', 'i' to 'ⁱ',
)

private val SUBSCRIPTS: Map<Char, Char> = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
    'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ',
    'h' to 'ₕ', 'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ',
    'n' to 'ₙ', 'p' to 'ₚ', 's' to 'ₛ', 't' to 'ₜ',
)
