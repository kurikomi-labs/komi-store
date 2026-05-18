package zed.rainxch.core.data.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
        // The multi-source race targets release-asset downloads. A mirror that
        // doesn't list RELEASE_ASSET (e.g. jsDelivr, raw-files only) can't
        // serve these URLs — fall through to a Direct download.
        if (active == null || TrafficKind.RELEASE_ASSET !in active.trafficKinds) {
            return downloader.download(githubUrl, suggestedFileName)
        }
        val mirrorUrl =
            MirrorRewriter.applyTemplate(active.template, githubUrl)
                ?: return downloader.download(githubUrl, suggestedFileName)
        return raceDownloads(githubUrl, mirrorUrl, suggestedFileName)
    }

    private fun raceDownloads(
        directUrl: String,
        mirrorUrl: String,
        suggestedFileName: String?,
    ): Flow<DownloadProgress> =
        channelFlow {
            val winnerSignal = CompletableDeferred<String>()

            val directJob =
                launch {
                    try {
                        downloader
                            .download(directUrl, suggestedFileName, bypassMirror = true)
                            .collect { progress ->
                                if (winnerSignal.complete("direct") || winnerSignal.getCompleted() == "direct") {
                                    send(progress)
                                } else {
                                    return@collect
                                }
                            }
                    } catch (t: Throwable) {
                        if (winnerSignal.isCompleted && winnerSignal.getCompleted() == "direct") throw t
                    }
                }

            val mirrorJob =
                launch {
                    try {
                        downloader
                            .download(mirrorUrl, suggestedFileName)
                            .collect { progress ->
                                if (winnerSignal.complete("mirror") || winnerSignal.getCompleted() == "mirror") {
                                    send(progress)
                                } else {
                                    return@collect
                                }
                            }
                    } catch (t: Throwable) {
                        if (winnerSignal.isCompleted && winnerSignal.getCompleted() == "mirror") throw t
                    }
                }

            val winner = winnerSignal.await()
            if (winner == "direct") mirrorJob.cancelAndJoin() else directJob.cancelAndJoin()
        }.flowOn(Dispatchers.IO)
}
