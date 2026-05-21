package zed.rainxch.core.data.services

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

class AndroidFileLocationsProvider(
    private val context: Context,
) : zed.rainxch.core.data.services.FileLocationsProvider {
    @Volatile
    private var _cachedDownloadsDir: String? = null

    override fun appDownloadsDir(): String {
        _cachedDownloadsDir?.let { return it }
        synchronized(this) {
            _cachedDownloadsDir?.let { return it }
            val resolved = resolveDownloadsDir()
            _cachedDownloadsDir = resolved
            return resolved
        }
    }

    private fun resolveDownloadsDir(): String {
        val candidates =
            listOf(
                { runCatching { context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) }.getOrNull() },
                { runCatching { context.getExternalFilesDir(null)?.let { File(it, "downloads") } }.getOrNull() },
                { File(context.filesDir, "downloads") },
            )
        for (factory in candidates) {
            val dir = factory.invoke() ?: continue
            val ready = ensureUsable(dir)
            if (ready != null) {
                if (dir != context.filesDir) {
                    Log.i(TAG, "Downloads dir resolved: ${ready.absolutePath}")
                }
                return ready.absolutePath
            }
            Log.w(TAG, "Downloads dir candidate unusable: ${dir.absolutePath}")
        }

        val fallback = File(context.filesDir, "downloads")
        fallback.mkdirs()
        return fallback.absolutePath
    }

    private fun ensureUsable(dir: File): File? {
        return try {
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
                return null
            }
            if (!dir.canWrite()) return null
            dir
        } catch (t: Throwable) {
            Log.w(TAG, "Failed probing ${dir.absolutePath}: ${t.message}")
            null
        }
    }

    override fun userDownloadsDir(): String {
        return appDownloadsDir()
    }

    override fun setExecutableIfNeeded(path: String) {

    }

    override fun getCacheSizeBytes(): Long {
        val dir = File(appDownloadsDir())
        return calculateDirSize(dir)
    }

    override fun clearCacheFiles(): Boolean {
        val dir = File(appDownloadsDir())
        return deleteDirectoryContents(dir)
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirectoryContents(dir: File): Boolean {
        if (!dir.exists()) return true
        var allDeleted = true
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!deleteDirectoryContents(file)) allDeleted = false
                if (!file.delete()) allDeleted = false
            } else {
                if (!file.delete()) allDeleted = false
            }
        }
        return allDeleted
    }

    private companion object {
        const val TAG = "AndroidFileLocations"
    }
}
