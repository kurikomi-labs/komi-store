package zed.rainxch.core.domain.network

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.DownloadProgress

interface SlowDownloadDetector {
    val suggestMirror: Flow<Unit>

    fun onProgress(progress: DownloadProgress)

    fun reset()
}
