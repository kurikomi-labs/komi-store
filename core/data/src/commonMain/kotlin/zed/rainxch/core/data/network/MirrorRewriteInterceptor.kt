package zed.rainxch.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey

val NO_MIRROR_REWRITE: AttributeKey<Boolean> = AttributeKey("NoMirrorRewrite")

fun HttpClient.installMirrorRewrite() {
    plugin(HttpSend).intercept { request ->
        if (!request.attributes.contains(NO_MIRROR_REWRITE)) {
            val original = request.url.buildString()
            if (MirrorRewriter.shouldRewrite(original)) {
                val active = ProxyManager.currentMirror()
                val kind = MirrorRewriter.classify(original)
                if (active != null && kind != null && kind in active.trafficKinds) {
                    val rewritten =
                        MirrorRewriter
                            .applyTemplate(active.template, original)
                            ?.let { runCatching { Url(it) }.getOrNull() }
                    if (rewritten != null) {
                        val originalHost = request.url.host
                        request.url.takeFrom(rewritten)
                        if (rewritten.host != originalHost) {
                            request.headers.remove(HttpHeaders.Authorization)
                        }
                    }
                }
            }
        }
        execute(request)
    }
}
