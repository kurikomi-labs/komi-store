package zed.rainxch.core.domain.model.installation

object InstallerAttributionDefaults {
    val packageNamePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$")

    fun isValidPackageName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return packageNamePattern.matches(trimmed)
    }
}
