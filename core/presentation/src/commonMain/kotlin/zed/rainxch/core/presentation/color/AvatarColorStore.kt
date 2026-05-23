package zed.rainxch.core.presentation.color

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import coil3.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object AvatarColorStore {
    private val cache: SnapshotStateMap<String, Color> = mutableStateMapOf()
    private val inflight: MutableSet<String> = mutableSetOf()
    private val mutex = Mutex()

    fun colorFor(url: String): Color? = cache[url]

    suspend fun computeIfAbsent(url: String, image: Image) {
        mutex.withLock {
            if (cache.containsKey(url) || url in inflight) return
            inflight.add(url)
        }
        val color = withContext(Dispatchers.Default) {
            runCatching { computeDominantFromImage(image) }.getOrNull()
        }
        mutex.withLock {
            inflight.remove(url)
            if (color != null) cache[url] = color
        }
    }
}

expect fun computeDominantFromImage(image: Image): Color?
