package zed.rainxch.tweaks.presentation.hidden

data class HiddenRepoUi(
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
) {
    val fullName: String get() = "$repoOwner/$repoName"
}
