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
    require(maxChars >= 0) { "maxChars must be >= 0, got $maxChars" }
    if (maxChars == 0) return ""
    if (content.length <= maxChars) return content

    // First pass: prefer a paragraph-break boundary in the last 25% of the
    // window — the natural end-of-section cut.
    val searchFrom = (maxChars * 0.75).toInt().coerceAtLeast(0)
    val window = content.substring(0, maxChars)
    val paragraphBreak = window.lastIndexOf("\n\n", maxChars).takeIf { it >= searchFrom }
    val candidate = when {
        paragraphBreak != null && paragraphBreak > 0 -> window.substring(0, paragraphBreak)
        else -> {
            val newline = window.lastIndexOf('\n', maxChars).takeIf { it >= searchFrom }
            if (newline != null && newline > 0) window.substring(0, newline) else window
        }
    }

    // Code-fence safety: if the candidate has an odd number of fence
    // openers (` ``` ` or ` ~~~ ` at line start), the cut is inside a
    // code block. Walking the renderer over that would emit raw
    // backticks in the preview. Truncate further back to the line
    // BEFORE the unclosed fence opener.
    val safe = closeOpenFence(candidate)
    return if (safe.length == content.length) content else safe.trimEnd() + "\n"
}

private fun closeOpenFence(candidate: String): String {
    var opens = 0
    var lastFenceLineStart = -1
    var lineStart = 0
    for (i in candidate.indices) {
        if (i == lineStart) {
            val rest = candidate.substring(i)
            val starts3 = rest.startsWith("```") || rest.startsWith("~~~")
            if (starts3) {
                opens++
                lastFenceLineStart = lineStart
            }
        }
        if (candidate[i] == '\n') lineStart = i + 1
    }
    if (opens % 2 == 0) return candidate
    // Odd → unclosed fence. Cut before the last opener.
    return if (lastFenceLineStart > 0) candidate.substring(0, lastFenceLineStart).trimEnd()
    else candidate
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
    require(targetChunkChars > 0) { "targetChunkChars must be > 0, got $targetChunkChars" }
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
