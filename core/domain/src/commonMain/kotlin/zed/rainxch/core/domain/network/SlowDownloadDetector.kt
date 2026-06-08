package zed.rainxch.core.domain.network

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.installation.DownloadProgress

interface SlowDownloadDetector {
    val suggestMirror: Flow<Unit>

    suspend fun onProgress(progress: DownloadProgress)

    suspend fun reset()
}
