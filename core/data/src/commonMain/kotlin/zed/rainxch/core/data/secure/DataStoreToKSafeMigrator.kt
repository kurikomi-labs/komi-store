package zed.rainxch.core.data.secure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.first

/**
 * One-shot copier: lift values from a legacy [DataStore] into a [KSafe]
 * vault. Idempotent — sets a marker in the KSafe vault so subsequent
 * launches skip the work.
 *
 * Each entry is a `(legacyKey, transform)` pair. The transform reads from
 * the legacy `Preferences` snapshot and writes into KSafe (decoupling
 * primitive vs. derived shapes such as enums or JSON objects).
 *
 * The legacy keys are removed after a successful copy so the next read
 * never sees the duplicate.
 */
suspend fun migrateDataStoreToKSafe(
    legacy: DataStore<Preferences>,
    ksafe: KSafe,
    markerKey: String,
    entries: List<MigrationEntry>,
) {
    val alreadyMigrated = runCatching { ksafe.get(markerKey, false) }.getOrDefault(false)
    if (alreadyMigrated) return

    val snapshotResult = runCatching { legacy.data.first() }
    val snapshot = snapshotResult.getOrNull()
    if (snapshot == null) {
        // Legacy file unreadable. Don't set the marker — retry next launch
        // so a transient I/O failure doesn't permanently skip migration.
        return
    }

    val movedKeys = mutableListOf<Preferences.Key<*>>()
    var anyFailure = false
    entries.forEach { entry ->
        val copyResult = runCatching { entry.copy(snapshot, ksafe) }
        when {
            copyResult.isFailure -> anyFailure = true
            copyResult.getOrNull() == true -> movedKeys += entry.legacyKeys
            // null/false → key absent or unsupported type; not a failure
        }
    }

    // Only delete legacy keys we know we copied successfully. Even on
    // partial failure, what we *did* copy is safe to remove.
    if (movedKeys.isNotEmpty()) {
        val deleteResult = runCatching {
            legacy.edit { prefs -> movedKeys.forEach { prefs.remove(it) } }
        }
        if (deleteResult.isFailure) anyFailure = true
    }

    // Only mark complete if every entry succeeded AND legacy cleanup
    // succeeded. Otherwise next launch retries the missing entries.
    if (!anyFailure) {
        runCatching { ksafe.put(markerKey, true) }
    }
}

class MigrationEntry(
    val legacyKeys: List<Preferences.Key<*>>,
    private val copyBlock: suspend (Preferences, KSafe) -> Boolean,
) {
    suspend fun copy(snapshot: Preferences, ksafe: KSafe): Boolean = copyBlock(snapshot, ksafe)

    companion object {
        operator fun invoke(
            legacyKey: Preferences.Key<*>,
            ksafeKey: String,
        ): MigrationEntry = MigrationEntry(
            legacyKeys = listOf(legacyKey),
            copyBlock = { snapshot, ksafe ->
                val value: Any? = snapshot[legacyKey]
                if (value == null) return@MigrationEntry false
                when (value) {
                    is Boolean -> ksafe.put<Boolean>(ksafeKey, value)
                    is Int -> ksafe.put<Int>(ksafeKey, value)
                    is Long -> ksafe.put<Long>(ksafeKey, value)
                    is Float -> ksafe.put<Float>(ksafeKey, value)
                    is Double -> ksafe.put<Double>(ksafeKey, value)
                    is String -> ksafe.put<String>(ksafeKey, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val asStrings = (value as Set<String>).toList()
                        ksafe.put<List<String>>(ksafeKey, asStrings)
                    }
                    else -> return@MigrationEntry false
                }
                true
            },
        )
    }
}
