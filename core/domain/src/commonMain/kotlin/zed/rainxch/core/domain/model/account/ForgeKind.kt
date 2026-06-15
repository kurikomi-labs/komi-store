package zed.rainxch.core.domain.model.account

enum class ForgeKind(
    val tokenHost: String,
    val displayName: String,
    val tokenCreationUrl: String,
    val tokenTermNoun: String,
) {
    GITHUB(
        tokenHost = "github.com",
        displayName = "GitHub",
        tokenCreationUrl =
            "https://github.com/settings/tokens/new" +
                "?scopes=repo,read:user&description=GitHub%20Store",
        tokenTermNoun = "Personal access token",
    ),
    CODEBERG(
        tokenHost = "codeberg.org",
        displayName = "Codeberg",
        tokenCreationUrl = "https://codeberg.org/user/settings/applications",
        tokenTermNoun = "Access token",
    ),
    ;

    companion object {
        fun fromHost(host: String): ForgeKind? = entries.firstOrNull { it.tokenHost == host }
    }
}
