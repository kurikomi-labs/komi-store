package zed.rainxch.details.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import zed.rainxch.details.presentation.model.SupportedLanguages

@Composable
fun ProvideLanguageLinkInterceptor(
    onTranslate: (String) -> Unit,
    onClearTranslation: (() -> Unit)? = null,
    onOpenInternalMarkdown: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current
    val customUriHandler = remember(defaultUriHandler, onTranslate, onClearTranslation, onOpenInternalMarkdown) {
        object : UriHandler {
            override fun openUri(uri: String) {
                if (Regex("""(?i)README\.md(?:[#?].*)?$""").find(uri) != null) {
                    if (onClearTranslation != null) {
                        onClearTranslation.invoke()
                        return
                    }
                }

                val match = Regex("""(?i)README[-._]([a-z]{2}(?:-[a-z]{2})?)\.md(?:[#?].*)?$""").find(uri)
                if (match != null) {
                    val code = match.groupValues[1]
                    val normalizedCode = when (code.lowercase()) {
                        "cn" -> "zh-CN"
                        "tw" -> "zh-TW"
                        "jp" -> "ja"
                        "kr" -> "ko"
                        "br" -> "pt-BR"
                        else -> code
                    }
                    val matchedLanguage = SupportedLanguages.all.find { 
                        it.code.equals(normalizedCode, ignoreCase = true) 
                    } ?: SupportedLanguages.all.find { 
                        it.code.startsWith(normalizedCode, ignoreCase = true) || normalizedCode.startsWith(it.code, ignoreCase = true)
                    }
                    if (matchedLanguage != null) {
                        onTranslate(matchedLanguage.code)
                        return
                    }
                }
                
                if (onOpenInternalMarkdown != null) {
                    val isInternalMarkdown = Regex("""(?i)^https://(?:raw\.githubusercontent\.com|github\.com)/[^/]+/[^/]+/(?:blob/)?[^/]+/.+\.md(?:[#?].*)?$""").find(uri) != null
                    if (isInternalMarkdown) {
                        onOpenInternalMarkdown.invoke(uri)
                        return
                    }
                }

                defaultUriHandler.openUri(uri)
            }
        }
    }
    CompositionLocalProvider(
        LocalUriHandler provides customUriHandler,
        content = content,
    )
}
