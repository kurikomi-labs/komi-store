package zed.rainxch.tweaks.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.tweaks.presentation.model.ProxyType

data class PastedProxy(
    val type: ProxyType,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
)

fun parseProxyUrl(raw: String): PastedProxy? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val schemeIdx = trimmed.indexOf("://")
    if (schemeIdx <= 0) return null
    val scheme = trimmed.substring(0, schemeIdx).lowercase()
    val type = when (scheme) {
        "http", "https" -> ProxyType.HTTP
        "socks", "socks4", "socks5", "socks5h" -> ProxyType.SOCKS
        else -> return null
    }
    val afterScheme = trimmed.substring(schemeIdx + 3)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    if (afterScheme.isEmpty()) return null

    val atIdx = afterScheme.lastIndexOf('@')
    val credPart = if (atIdx >= 0) afterScheme.substring(0, atIdx) else null
    val hostPart = if (atIdx >= 0) afterScheme.substring(atIdx + 1) else afterScheme

    val (username, password) = if (credPart != null) {
        val colonIdx = credPart.indexOf(':')
        if (colonIdx >= 0) {
            credPart.substring(0, colonIdx) to credPart.substring(colonIdx + 1)
        } else {
            credPart to ""
        }
    } else {
        null to null
    }

    val colonIdx = hostPart.lastIndexOf(':')
    if (colonIdx <= 0) return null
    val host = hostPart.substring(0, colonIdx)
    val port = hostPart.substring(colonIdx + 1).toIntOrNull() ?: return null
    if (host.isBlank() || port !in 1..65535) return null

    return PastedProxy(
        type = type,
        host = host,
        port = port,
        username = username?.takeIf { it.isNotBlank() },
        password = password?.takeIf { it.isNotBlank() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteProxyUrlSheet(
    onDismiss: () -> Unit,
    onParsed: (PastedProxy) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Paste proxy URL",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Paste a full proxy URL and we'll fill the form for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GhsTextField(
                value = input,
                onValueChange = {
                    input = it
                    error = null
                },
                label = "scheme://user:pass@host:port",
                isError = error != null,
                supportingText = error,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            GhsButton(
                onClick = {
                    val parsed = parseProxyUrl(input)
                    if (parsed == null) {
                        error = "Couldn't read that URL."
                    } else {
                        onParsed(parsed)
                    }
                },
                label = "Use this URL",
                variant = GhsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
