package zed.rainxch.core.domain.system

interface DownloadProgressNotifier {

    fun notifyProgress(
        packageName: String,
        appName: String,
        versionTag: String,
        percent: Int?,
        bytesDownloaded: Long,
        totalBytes: Long?,
    )

    fun clearProgress(packageName: String)
}
