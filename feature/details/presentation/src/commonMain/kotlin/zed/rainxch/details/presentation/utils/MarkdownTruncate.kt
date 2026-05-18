package zed.rainxch.details.presentation.utils

/**
 * Returns a markdown substring suitable for rendering as a collapsed
 * "preview" without the cost of composing the full tree. Truncates at a
 * sensible boundary (double newline, then single newline, then `maxChars`)
 * so headings and code fences aren't sliced mid-block.
 *
 * Always callable from any thread — no Compose APIs touched.
 */
fun truncateMarkdownPreview(content: String, maxChars: Int): String {
    if (content.length <= maxChars) return content
    val window = content.substring(0, maxChars)

    // Prefer cutting at paragraph break (blank line) inside the last 25%
    // of the window so the preview ends on a natural section boundary.
    val searchFrom = (maxChars * 0.75).toInt().coerceAtLeast(0)
    val paragraphBreak = window.lastIndexOf("\n\n", maxChars).takeIf { it >= searchFrom }
    if (paragraphBreak != null && paragraphBreak > 0) {
        return window.substring(0, paragraphBreak).trimEnd() + "\n"
    }
    val newline = window.lastIndexOf('\n', maxChars).takeIf { it >= searchFrom }
    if (newline != null && newline > 0) {
        return window.substring(0, newline).trimEnd() + "\n"
    }
    return window.trimEnd() + "…"
}

/**
 * Splits a markdown document into roughly equal chunks at top-level block
 * boundaries (double newlines), each ≤ `targetChunkChars`. Each chunk is a
 * self-contained snippet the markdown parser can handle in isolation; we
 * render them as separate `Markdown(...)` composables so the expensive
 * AST-to-Compose pass happens once per chunk rather than once for the
 * entire document.
 *
 * Code fences are kept whole (we never slice between ```...```), even if
 * that makes one chunk larger than `targetChunkChars`. Without that
 * guard the parser would emit half-open fence nodes and the renderer
 * would print raw backticks.
 *
 * Always callable from any thread — no Compose APIs touched.
 */
fun splitMarkdownIntoChunks(content: String, targetChunkChars: Int): List<String> {
    if (content.length <= targetChunkChars) return listOf(content)
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    var inFence = false
    val lines = content.split('\n')
    fun flush() {
        if (current.isNotEmpty()) {
            chunks += current.toString()
            current.clear()
        }
    }
    for (line in lines) {
        val trimmed = line.trimStart()
        // Toggle fence flag on lines starting with ``` (markdown fence
        // opener / closer). `~~~` is the alternate syntax used by some
        // forges; same toggle rules apply.
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            inFence = !inFence
        }
        if (current.isNotEmpty()) current.append('\n')
        current.append(line)
        if (!inFence &&
            current.length >= targetChunkChars &&
            line.isBlank()
        ) {
            flush()
        }
    }
    flush()
    return chunks
}
