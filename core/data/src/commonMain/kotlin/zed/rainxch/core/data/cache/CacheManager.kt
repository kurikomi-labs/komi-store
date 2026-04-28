package zed.rainxch.core.data.cache

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import zed.rainxch.core.data.local.db.dao.CacheDao
import zed.rainxch.core.data.local.db.entities.CacheEntryEntity
import zed.rainxch.core.domain.telemetry.ProductTelemetry
import zed.rainxch.core.domain.telemetry.ProductTelemetryEvents
import zed.rainxch.core.domain.telemetry.ProductTelemetryProps
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class CacheManager(
    val cacheDao: CacheDao,
    @PublishedApi internal val productTelemetry: ProductTelemetry,
) {
    val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

    val memoryCache = HashMap<String, Pair<Long, String>>()

    fun now(): Long = Clock.System.now().toEpochMilliseconds()

    suspend inline fun <reified T> get(key: String): T? {
        val currentTime = now()

        memoryCache[key]?.let { (expiresAt, jsonData) ->
            if (expiresAt > currentTime) {
                return try {
                    val value = json.decodeFromString(serializer<T>(), jsonData)
                    fireCacheLookup(key, hit = true)
                    value
                } catch (_: Exception) {
                    memoryCache.remove(key)
                    null
                }
            } else {
                memoryCache.remove(key)
            }
        }

        val entry =
            cacheDao.getValid(key, currentTime) ?: run {
                fireCacheLookup(key, hit = false)
                return null
            }
        memoryCache[key] = entry.expiresAt to entry.jsonData

        return try {
            val value = json.decodeFromString(serializer<T>(), entry.jsonData)
            fireCacheLookup(key, hit = true)
            value
        } catch (_: Exception) {
            cacheDao.delete(key)
            memoryCache.remove(key)
            null
        }
    }

    @PublishedApi
    internal fun fireCacheLookup(key: String, hit: Boolean) {
        val cacheName = deriveCacheName(key) ?: return
        productTelemetry.fire(
            name = if (hit) ProductTelemetryEvents.CACHE_HIT else ProductTelemetryEvents.CACHE_MISS,
            props = mapOf(ProductTelemetryProps.CACHE_NAME to cacheName),
        )
    }

    @PublishedApi
    internal fun deriveCacheName(key: String): String? =
        when {
            key.startsWith("readme:") -> "readmes"
            key.startsWith("search:") -> "search_results"
            key.startsWith("repo:") ||
                key.startsWith("repo_id:") ||
                key.startsWith("releases:") ||
                key.startsWith("latest_release:") ||
                key.startsWith("repo_stats:") ||
                key.startsWith("user_profile:") -> "details"
            else -> null
        }

    suspend inline fun <reified T> getStale(key: String): T? {
        val entry = cacheDao.getAny(key) ?: return null
        return try {
            json.decodeFromString(serializer<T>(), entry.jsonData)
        } catch (_: Exception) {
            null
        }
    }

    suspend inline fun <reified T> put(
        key: String,
        value: T,
        ttlMillis: Long,
    ) {
        val currentTime = now()
        val jsonData = json.encodeToString(serializer<T>(), value)
        val expiresAt = currentTime + ttlMillis

        memoryCache[key] = expiresAt to jsonData

        cacheDao.put(
            CacheEntryEntity(
                key = key,
                jsonData = jsonData,
                cachedAt = currentTime,
                expiresAt = expiresAt,
            ),
        )
    }

    suspend fun invalidate(key: String) {
        memoryCache.remove(key)
        cacheDao.delete(key)
    }

    suspend fun invalidateByPrefix(prefix: String) {
        val keysToRemove = memoryCache.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { memoryCache.remove(it) }
        cacheDao.deleteByPrefix(prefix)
    }

    suspend fun clearAll() {
        memoryCache.clear()
        cacheDao.deleteAll()
    }

    suspend fun cleanupExpired() {
        val currentTime = now()
        val expiredKeys =
            memoryCache.entries
                .filter { it.value.first <= currentTime }
                .map { it.key }
        expiredKeys.forEach { memoryCache.remove(it) }
        cacheDao.deleteExpired(currentTime)
    }

    companion object CacheTtl {
        val HOME_REPOS = 12.hours.inWholeMilliseconds
        val REPO_DETAILS = 6.hours.inWholeMilliseconds
        val RELEASES = 6.hours.inWholeMilliseconds
        val README = 12.hours.inWholeMilliseconds
        val USER_PROFILE = 6.hours.inWholeMilliseconds
        val SEARCH_RESULTS = 1.hours.inWholeMilliseconds
        val REPO_STATS = 6.hours.inWholeMilliseconds
    }
}
