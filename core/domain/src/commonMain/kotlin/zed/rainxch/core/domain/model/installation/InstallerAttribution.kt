package zed.rainxch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface InstallerAttribution {
    @Serializable
    data object SystemDefault : InstallerAttribution

    @Serializable
    data class Preset(val key: PresetKey) : InstallerAttribution

    @Serializable
    data class Custom(val packageName: String) : InstallerAttribution

    fun resolvePackageName(): String? = when (this) {
        SystemDefault -> null
        is Preset -> key.packageName
        is Custom -> packageName.trim().takeIf { it.isNotBlank() }
    }
}

@Serializable
enum class PresetKey(val packageName: String) {
    PLAY_STORE("com.android.vending"),
    FDROID("org.fdroid.fdroid"),
    OBTAINIUM("dev.imranr.obtainium.app"),
    ;

    companion object {
        fun fromName(name: String?): PresetKey? = entries.find { it.name == name }
    }
}

object InstallerAttributionDefaults {
    val packageNamePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$")

    fun isValidPackageName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return packageNamePattern.matches(trimmed)
    }
}
