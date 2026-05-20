package zed.rainxch.details.data.repository

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import zed.rainxch.core.data.network.TranslationClientProvider
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.details.data.translation.GoogleTranslator
import zed.rainxch.details.data.translation.Translator
import zed.rainxch.details.data.translation.DeeplTranslator
import zed.rainxch.details.data.translation.LibreTranslator
import zed.rainxch.details.data.translation.YoudaoTranslator
import zed.rainxch.details.domain.model.TranslationResult
import zed.rainxch.details.domain.repository.TranslationRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Orchestrates translation: picks the user-configured [Translator]
 * ([TranslationProvider]), drives chunking + caching in this layer
 * (so each concrete translator only has to round-trip a single
 * chunk), and stitches results back together.
 */
class TranslationRepositoryImpl(
    private val localizationManager: LocalizationManager,
    private val clientProvider: TranslationClientProvider,
    private val tweaksRepository: TweaksRepository,
) : TranslationRepository {
    private val httpClient: HttpClient get() = clientProvider.client

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    // Google's provider has no per-install config — share a single
    // instance for the lifetime of the repository.
    private val googleTranslator: GoogleTranslator =
        GoogleTranslator(httpClient = { httpClient }, json = json)

    private val cacheMutex = Mutex()
    private val cache = LinkedHashMap<CacheKey, CachedTranslation>(MAX_CACHE_SIZE, 0.75f, true)

    @OptIn(ExperimentalTime::class)
    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String,
    ): TranslationResult {
        val cacheKey = CacheKey(text, targetLanguage, sourceLanguage)

        cacheMutex.withLock {
            cache[cacheKey]?.let { cached ->
                if (!cached.isExpired()) return cached.result
                cache.remove(cacheKey)
            }
        }

        // Mask out machine-readable spans (code fences, HTML tags,
        // markdown URLs, bare URLs) so the translator doesn't mangle
        // them. See `protectFromTranslation` kdoc for the full list.
        val protection = protectFromTranslation(text)

        val translator = resolveTranslator()
        val chunks = chunkText(protection.maskedText, translator.maxChunkSize)
        val translatedParts = mutableListOf<Pair<String, String>>()
        var detectedLang: String? = null

        for ((chunkText, delimiter) in chunks) {
            val response = translator.translate(chunkText, targetLanguage, sourceLanguage)
            translatedParts.add(response.translatedText to delimiter)
            if (detectedLang == null) {
                detectedLang = response.detectedSourceLanguage
            }
        }

        val joined =
            translatedParts
                .dropLast(1)
                .joinToString("") { (text, delim) -> text + delim } +
                translatedParts.lastOrNull()?.first.orEmpty()

        val result =
            TranslationResult(
                translatedText = restoreProtectedSpans(joined, protection.spans),
                detectedSourceLanguage = detectedLang,
            )

        cacheMutex.withLock {
            if (cache.size >= MAX_CACHE_SIZE) {
                val firstKey = cache.keys.first()
                cache.remove(firstKey)
            }
            cache[cacheKey] = CachedTranslation(result)
        }
        return result
    }

    /**
     * Strips machine-readable spans (code fences, HTML tags, markdown
     * link/image URLs) that translators will otherwise mangle. Each span
     * is replaced with an opaque marker the translator passes through
     * verbatim, then restored after the joined translation comes back.
     *
     * Why bigger than just fenced code: real READMEs commonly stack
     * `[![alt](badge.svg)](https://store/...)` badge rows and raw
     * `<img>` tags. Translators tokenize on `(` and `<`, then insert
     * whitespace into URLs (observed on `https://appgallery.cloud.
     * huawei.com/...`) or translate alt text into the href slot. Both
     * corrupt the rendered markdown.
     */
    private fun protectFromTranslation(text: String): TranslationProtection {
        val spans = mutableListOf<String>()
        var masked = text

        // 1. Fenced code blocks first — these are the most disruptive
        //    spans and matching them before everything else means later
        //    passes won't accidentally re-mask their inner content.
        masked = Regex("```[\\s\\S]*?```", RegexOption.MULTILINE).replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }
        // 2. HTML tags including their bodies (`<a>...</a>`, `<img …/>`,
        //    `<picture>...</picture>`). Translators rewrite `href` and
        //    `src` attributes; preserve the whole element.
        masked = Regex("<[^/!][a-zA-Z0-9]*[^>]*?/>").replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }
        masked = Regex(
            "<(a|img|picture|source|video|audio|svg)\\b[^>]*>[\\s\\S]*?</\\1>",
            RegexOption.IGNORE_CASE,
        ).replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }
        masked = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }
        // 3. Markdown link / image URLs: the `](url)` tail. Keep the
        //    `[label]` part untouched so prose-style link text still
        //    translates (e.g. `[the docs]` → `[la documentación]`).
        masked = Regex("\\]\\(([^)]+)\\)").replace(masked) { match ->
            val url = match.groupValues[1]
            "](" + replaceWithMarker(spans, url) + ")"
        }
        // 4. Bare URLs in plain text. Without this, translators
        //    sometimes split long URLs across whitespace.
        masked = Regex("https?://[^\\s<>\")]+").replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }
        // 5. GFM alert callout markers: `[!NOTE]`, `[!TIP]`,
        //    `[!IMPORTANT]`, `[!WARNING]`, `[!CAUTION]`. The renderer
        //    pattern-matches these literally to pick the alert kind /
        //    icon / tint; translation rewrites `[!IMPORTANT]` into
        //    e.g. `[!Vajno]` (ru) and the renderer falls back to a
        //    plain blockquote, losing the formatting entirely.
        masked = Regex(
            "\\[!(?:NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]",
            RegexOption.IGNORE_CASE,
        ).replace(masked) { match ->
            replaceWithMarker(spans, match.value)
        }

        return TranslationProtection(masked, spans)
    }

    private fun replaceWithMarker(spans: MutableList<String>, value: String): String {
        val idx = spans.size
        spans += value
        // Unicode math brackets + ALL_CAPS underscore token — empirically
        // preserved verbatim by Google + Youdao translators across the 33
        // supported targets.
        return "⟦TR_${idx}_END⟧"
    }

    private fun restoreProtectedSpans(translated: String, spans: List<String>): String {
        if (spans.isEmpty()) return translated
        var result = translated
        spans.forEachIndexed { i, original ->
            // Tolerate translator inserting whitespace around the marker.
            // Falls back to leaving any unmatched marker in place rather
            // than corrupting prose.
            val pattern = Regex("⟦\\s*TR_\\s*${i}\\s*_END\\s*⟧")
            result = pattern.replaceFirst(result, Regex.escapeReplacement(original))
        }
        return result
    }

    private data class TranslationProtection(
        val maskedText: String,
        val spans: List<String>,
    )

    override fun getDeviceLanguageCode(): String = localizationManager.getPrimaryLanguageCode()

    /**
     * Resolves the currently-selected translator from preferences.
     * Called per request rather than held as a field so provider /
     * credential changes take effect on the next translation without
     * requiring the repository to be rebuilt.
     */
    private suspend fun resolveTranslator(): Translator {
        val provider = tweaksRepository.getTranslationProvider().first()
        return when (provider) {
            TranslationProvider.GOOGLE -> googleTranslator
            TranslationProvider.YOUDAO -> {
                val appKey = tweaksRepository.getYoudaoAppKey().first()
                val appSecret = tweaksRepository.getYoudaoAppSecret().first()
                YoudaoTranslator(
                    httpClient = { httpClient },
                    json = json,
                    appKey = appKey,
                    appSecret = appSecret,
                )
            }
            TranslationProvider.LIBRE_TRANSLATE -> {
                val baseUrl = tweaksRepository.getLibreTranslateBaseUrl().first()
                val apiKey = tweaksRepository.getLibreTranslateApiKey().first().takeIf { it.isNotBlank() }
                LibreTranslator(
                    httpClient = { httpClient },
                    json = json,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                )
            }
            TranslationProvider.DEEPL -> {
                val authKey = tweaksRepository.getDeeplAuthKey().first()
                DeeplTranslator(
                    httpClient = { httpClient },
                    json = json,
                    authKey = authKey,
                )
            }
        }
    }

    private fun chunkText(text: String, maxChunkSize: Int): List<Pair<String, String>> {
        val paragraphs = text.split("\n\n")
        val chunks = mutableListOf<Pair<String, String>>()
        val currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            if (paragraph.length > maxChunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(Pair(currentChunk.toString(), "\n\n"))
                    currentChunk.clear()
                }
                chunkLargeParagraph(paragraph, chunks, maxChunkSize)
            } else if (currentChunk.length + paragraph.length + 2 > maxChunkSize) {
                chunks.add(Pair(currentChunk.toString(), "\n\n"))
                currentChunk.clear()
                currentChunk.append(paragraph)
            } else {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(paragraph)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(Pair(currentChunk.toString(), "\n\n"))
        }

        return chunks
    }

    private fun chunkLargeParagraph(
        paragraph: String,
        chunks: MutableList<Pair<String, String>>,
        maxChunkSize: Int,
    ) {
        val lines = paragraph.split("\n")
        val currentChunk = StringBuilder()

        for (line in lines) {
            if (line.length > maxChunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(Pair(currentChunk.toString(), "\n"))
                    currentChunk.clear()
                }
                var start = 0
                while (start < line.length) {
                    val end = minOf(start + maxChunkSize, line.length)
                    chunks.add(Pair(line.substring(start, end), ""))
                    start = end
                }
            } else if (currentChunk.length + line.length + 1 > maxChunkSize) {
                chunks.add(Pair(currentChunk.toString(), "\n"))
                currentChunk.clear()
                currentChunk.append(line)
            } else {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                currentChunk.append(line)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(Pair(currentChunk.toString(), "\n"))
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 50
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }

    @OptIn(ExperimentalTime::class)
    private class CachedTranslation(
        val result: TranslationResult,
        private val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    ) {
        fun isExpired(): Boolean = Clock.System.now().toEpochMilliseconds() - timestamp > CACHE_TTL_MS
    }

    private data class CacheKey(
        val text: String,
        val targetLanguage: String,
        val sourceLanguage: String,
    )
}
