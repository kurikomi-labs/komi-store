package zed.rainxch.core.domain.system

data class OrchestratedDownload(
    val id: String,
    val packageName: String,
    val repoOwner: String,
    val repoName: String,
    val displayAppName: String,
    val assetName: String,
    val assetSize: Long,
    val downloadUrl: String,
    val releaseTag: String,

    val filePath: String?,

    val installPolicy: InstallPolicy,
    val stage: DownloadStage,

    val progressPercent: Int?,

    val bytesDownloaded: Long = 0L,

    val totalBytes: Long? = null,

    val errorMessage: String? = null,
    val installOutcome: InstallOutcome? = null,
)
