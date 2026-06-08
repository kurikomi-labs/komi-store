package zed.rainxch.core.domain.system

enum class DownloadStage {

    Queued,

    Downloading,

    Installing,

    AwaitingInstall,

    Completed,

    Cancelled,

    Failed,
}
