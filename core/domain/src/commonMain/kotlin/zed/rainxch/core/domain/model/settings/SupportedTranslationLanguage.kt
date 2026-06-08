package zed.rainxch.core.domain.model

data class SupportedTranslationLanguage(
    val code: String,
    val displayName: String,
)

object SupportedTranslationLanguages {
    val all: List<SupportedTranslationLanguage> =
        listOf(
            SupportedTranslationLanguage("ar", "Arabic"),
            SupportedTranslationLanguage("bn", "Bengali"),
            SupportedTranslationLanguage("zh-CN", "Chinese (Simplified)"),
            SupportedTranslationLanguage("zh-TW", "Chinese (Traditional)"),
            SupportedTranslationLanguage("cs", "Czech"),
            SupportedTranslationLanguage("da", "Danish"),
            SupportedTranslationLanguage("nl", "Dutch"),
            SupportedTranslationLanguage("en", "English"),
            SupportedTranslationLanguage("fi", "Finnish"),
            SupportedTranslationLanguage("fr", "French"),
            SupportedTranslationLanguage("de", "German"),
            SupportedTranslationLanguage("el", "Greek"),
            SupportedTranslationLanguage("he", "Hebrew"),
            SupportedTranslationLanguage("hi", "Hindi"),
            SupportedTranslationLanguage("hu", "Hungarian"),
            SupportedTranslationLanguage("id", "Indonesian"),
            SupportedTranslationLanguage("it", "Italian"),
            SupportedTranslationLanguage("ja", "Japanese"),
            SupportedTranslationLanguage("ko", "Korean"),
            SupportedTranslationLanguage("ms", "Malay"),
            SupportedTranslationLanguage("no", "Norwegian"),
            SupportedTranslationLanguage("pl", "Polish"),
            SupportedTranslationLanguage("pt", "Portuguese"),
            SupportedTranslationLanguage("pt-BR", "Portuguese (Brazil)"),
            SupportedTranslationLanguage("ro", "Romanian"),
            SupportedTranslationLanguage("ru", "Russian"),
            SupportedTranslationLanguage("es", "Spanish"),
            SupportedTranslationLanguage("sv", "Swedish"),
            SupportedTranslationLanguage("th", "Thai"),
            SupportedTranslationLanguage("tr", "Turkish"),
            SupportedTranslationLanguage("uk", "Ukrainian"),
            SupportedTranslationLanguage("uz", "Uzbek"),
            SupportedTranslationLanguage("vi", "Vietnamese"),
        )

    fun findByCode(code: String?): SupportedTranslationLanguage? =
        code?.let { c -> all.firstOrNull { it.code.equals(c, ignoreCase = true) } }
}
