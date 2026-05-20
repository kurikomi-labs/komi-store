package zed.rainxch.details.data.translation

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import zed.rainxch.details.domain.model.TranslationResult

internal class LibreTranslator(
    private val httpClient: () -> HttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val apiKey: String?,
) : Translator {
    override val maxChunkSize: Int = 2_000

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String,
    ): TranslationResult {
        if (baseUrl.isBlank()) {
            throw TranslationProviderNotConfiguredException(
                "LibreTranslate instance URL not configured",
            )
        }

        val endpoint = baseUrl.trimEnd('/') + "/translate"
        val source = mapLanguageCode(sourceLanguage)
        val target = mapLanguageCode(targetLanguage)

        val response = httpClient().submitForm(
            url = endpoint,
            formParameters = Parameters.build {
                append("q", text)
                append("source", source)
                append("target", target)
                append("format", "text")
                if (!apiKey.isNullOrBlank()) append("api_key", apiKey)
            },
        )

        val body = response.bodyAsText()
        val root = json.parseToJsonElement(body).jsonObject

        root["error"]?.jsonPrimitive?.content?.let { msg ->
            throw RuntimeException("LibreTranslate error: $msg")
        }

        val translation = root["translatedText"]?.jsonPrimitive?.content.orEmpty()
        val detected = root["detectedLanguage"]
            ?.jsonObject
            ?.get("language")
            ?.jsonPrimitive
            ?.content
            ?.takeIf { it.isNotBlank() }

        return TranslationResult(
            translatedText = translation,
            detectedSourceLanguage = detected,
        )
    }

    private fun mapLanguageCode(code: String): String =
        when (code.lowercase()) {
            "", "auto" -> "auto"
            "zh-cn", "zh-hans" -> "zh"
            "zh-tw", "zh-hant" -> "zt"
            else -> code.lowercase().substringBefore('-')
        }
}
