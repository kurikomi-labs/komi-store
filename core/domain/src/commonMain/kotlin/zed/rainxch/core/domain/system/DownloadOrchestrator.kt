package zed.rainxch.core.domain.system

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import zed.rainxch.core.domain.model.GithubAsset

interface DownloadOrchestrator {

    val downloads: StateFlow<Map<String, OrchestratedDownload>>

    fun observe(packageName: String): Flow<OrchestratedDownload?>

    suspend fun enqueue(spec: DownloadSpec): String

    suspend fun downgradeToDeferred(packageName: String)

    suspend fun cancel(packageName: String)

    suspend fun installPending(packageName: String): InstallOutcome?

    fun dismiss(packageName: String)
}

data class DownloadSpec(

    val packageName: String,

    val repoOwner: String,

    val repoName: String,

    val asset: GithubAsset,

    val displayAppName: String,

    val installPolicy: InstallPolicy,

    val releaseTag: String,
)

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

enum class DownloadStage {

    Queued,

    Downloading,

    Installing,

    AwaitingInstall,

    Completed,

    Cancelled,

    Failed,
}

enum class InstallPolicy {

    AlwaysInstall,

    InstallWhileForeground,

    DeferUntilUserAction,
}
