package zed.rainxch.core.domain.model

/**
 * Backend used to translate README content.
 *
 * - [GOOGLE] hits Google's public `translate.googleapis.com/translate_a/single`
 *   endpoint. No credentials required and works everywhere Google does, but
 *   it's an undocumented endpoint that can change or rate-limit at any time,
 *   and it's unreachable from mainland China without a proxy.
 * - [YOUDAO] hits Youdao's official `openapi.youdao.com/api`. Requires the
 *   user to provide their own `appKey` / `appSecret` from Youdao's developer
 *   portal (there's no anonymous free tier). Directly accessible from
 *   mainland China, which is why it exists — see issue #429.
 */
enum class TranslationProvider {
    GOOGLE,
    YOUDAO,
    LIBRE_TRANSLATE,
    DEEPL,
    ;

    companion object {
        val Default: TranslationProvider = GOOGLE

        fun fromName(name: String?): TranslationProvider =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
