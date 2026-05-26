package zed.rainxch.core.data.download

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import zed.rainxch.core.data.network.MirrorRewriter
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.domain.model.DownloadProgress
import zed.rainxch.core.domain.model.TrafficKind
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.system.MultiSourceDownloader

@OptIn(ExperimentalCoroutinesApi::class)
class MultiSourceDownloaderImpl(
    private val downloader: Downloader,
) : MultiSourceDownloader {
    override fun download(
        githubUrl: String,
        suggestedFileName: String?,
    ): Flow<DownloadProgress> {
        val active = ProxyManager.currentMirror()
        if (active == null || TrafficKind.RELEASE_ASSET !in active.trafficKinds) {
            return downloader.download(githubUrl, suggestedFileName)
        }
        val mirrorUrl =
            MirrorRewriter.applyTemplate(active.template, githubUrl)
                ?: return downloader.download(githubUrl, suggestedFileName)
        return mirrorFirstWithFallback(mirrorUrl, githubUrl, suggestedFileName)
    }

    private fun mirrorFirstWithFallback(
        mirrorUrl: String,
        directUrl: String,
        suggestedFileName: String?,
    ): Flow<DownloadProgress> =
        channelFlow {
            try {
                downloader.download(mirrorUrl, suggestedFileName).collect { send(it) }
                return@channelFlow
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Logger.w(t) { "Mirror download failed, falling back to direct." }
            }
            var firstDirectEmit = true
            downloader
                .download(directUrl, suggestedFileName, bypassMirror = true)
                .collect { progress ->
                    if (firstDirectEmit) {
                        firstDirectEmit = false
                        send(progress.copy(restart = true))
                    } else {
                        send(progress)
                    }
                }
        }.flowOn(Dispatchers.IO)
}
