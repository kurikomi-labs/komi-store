package zed.rainxch.tweaks.presentation.model

import zed.rainxch.core.domain.model.settings.ProxyConfig

enum class ProxyType {
    NONE,
    SYSTEM,
    HTTP,
    SOCKS,
    ;

    companion object {
        fun fromConfig(config: ProxyConfig): ProxyType =
            when (config) {
                is ProxyConfig.None -> NONE
                is ProxyConfig.System -> SYSTEM
                is ProxyConfig.Http -> HTTP
                is ProxyConfig.Socks -> SOCKS
            }
    }
}
