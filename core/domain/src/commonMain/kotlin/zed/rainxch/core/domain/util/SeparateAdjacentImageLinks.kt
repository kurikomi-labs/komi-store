package zed.rainxch.core.domain.util

/**
 * Splits adjacent badge / image-link lines into separate paragraphs so
 * each renders as a block-level element instead of inline content in a
 * single paragraph.
 *
 * Why this matters: markdown like
 *
 * ```
 * [![Apache-2.0](badge1.svg)](url1)
 * [![Downloads](badge2.svg)](url2)
 * [![Stars](badge3.svg)](url3)
 * ```
 *
 * is one paragraph with soft line breaks. The renderer (mikepenz lib)
 * uses ONE shared `Placeholder` for all inline images in a paragraph
 * — when image height > the placeholder slot, adjacent images paint
 * into each other's vertical space and visually overlap. Splitting
 * the lines with blank rows turns each link into its own paragraph,
 * which the lib renders via the block-level image component
 * (`LinkAwareMarkdownImage` in our case) where size is controlled
 * cleanly.
 *
 * Strategy: walk line by line. If a line contains ONLY a single
 * markdown link / image-link / HTML anchor-wrapping-image (i.e. it
 * is "structurally a badge row"), and the PREVIOUS non-blank line
 * also contained ONLY a link / image, ensure there's a blank row
 * between them. No-op for everything else.
 */
fun separateAdjacentImageLinks(content: String): String {
    if (content.isEmpty()) return content
    val lines = content.split('\n')
    val out = StringBuilder(content.length + 64)
    var lastWasLinkLine = false
    var lastEmittedWasBlank = true
    for (line in lines) {
        val trimmed = line.trim()
        val isLinkLine = trimmed.isNotEmpty() && isPureImageLinkLine(trimmed)
        if (isLinkLine && lastWasLinkLine && !lastEmittedWasBlank) {
            out.append('\n')
            lastEmittedWasBlank = true
        }
        if (out.isNotEmpty()) out.append('\n')
        out.append(line)
        lastEmittedWasBlank = trimmed.isEmpty()
        if (trimmed.isNotEmpty()) {
            lastWasLinkLine = isLinkLine
        }
    }
    return out.toString()
}

/**
 * `true` when [line] (already trimmed) is composed purely of image-link
 * structures with optional whitespace between — i.e. nothing else but
 * markdown link / image-link / HTML `<a>...<img/>...</a>` constructs.
 *
 * Heuristic-quality only. Aim is to recognise common badge rows in the
 * wild without misclassifying paragraphs that happen to start with an
 * image link inline next to prose.
 */
private fun isPureImageLinkLine(line: String): Boolean {
    // Cheap rejection: must contain `](` (markdown link) or `<a` (HTML).
    if ("](" !in line && !line.contains("<a", ignoreCase = true)) return false
    // Strip out all candidate constructs in order; if what's left is
    // blank, the line was purely image-link content.
    var stripped = line
    // `[![...](src)](href)` and `[![](src)](href)` variants
    stripped = stripped.replace(
        Regex("""\[!\[[^\]]*]\([^)]+\)]\([^)]+\)"""),
        "",
    )
    // `![alt](src)` standalone image
    stripped = stripped.replace(
        Regex("""!\[[^\]]*]\([^)]+\)"""),
        "",
    )
    // `[text](href)` plain link — keep only if its text is empty or
    // contains an image we already stripped (otherwise it's prose).
    stripped = stripped.replace(
        Regex("""\[\s*]\([^)]+\)"""),
        "",
    )
    // HTML `<a href="…"> ... </a>` blocks that contain an `<img …>`
    stripped = stripped.replace(
        Regex(
            """<a\b[^>]*>\s*(?:<img\b[^>]*/?>|\s)*</a>""",
            RegexOption.IGNORE_CASE,
        ),
        "",
    )
    // Standalone `<img …/>`
    stripped = stripped.replace(
        Regex("""<img\b[^>]*/?>""", RegexOption.IGNORE_CASE),
        "",
    )
    return stripped.isBlank()
}
