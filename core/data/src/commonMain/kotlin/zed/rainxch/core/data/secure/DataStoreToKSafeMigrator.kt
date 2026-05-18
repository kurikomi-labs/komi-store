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

    val snapshot = runCatching { legacy.data.first() }.getOrNull() ?: run {
        runCatching { ksafe.put(markerKey, true) }
        return
    }

    val movedKeys = mutableListOf<Preferences.Key<*>>()
    entries.forEach { entry ->
        runCatching {
            if (entry.copy(snapshot, ksafe)) {
                movedKeys += entry.legacyKeys
            }
        }
    }

    if (movedKeys.isNotEmpty()) {
        runCatching {
            legacy.edit { prefs ->
                movedKeys.forEach { prefs.remove(it) }
            }
        }
    }

    runCatching { ksafe.put(markerKey, true) }
}

class MigrationEntry(
    val legacyKeys: List<Preferences.Key<*>>,
    private val copyBlock: suspend (Preferences, KSafe) -> Boolean,
) {
    suspend fun copy(snapshot: Preferences, ksafe: KSafe): Boolean = copyBlock(snapshot, ksafe)

    companion object {
        operator fun <T : Any> invoke(
            legacyKey: Preferences.Key<T>,
            ksafeKey: String,
        ): MigrationEntry = MigrationEntry(
            legacyKeys = listOf(legacyKey),
            copyBlock = { snapshot, ksafe ->
                val value = snapshot[legacyKey]
                if (value != null) {
                    @Suppress("UNCHECKED_CAST")
                    when (value) {
                        is Boolean -> ksafe.put(ksafeKey, value)
                        is Int -> ksafe.put(ksafeKey, value)
                        is Long -> ksafe.put(ksafeKey, value)
                        is Float -> ksafe.put(ksafeKey, value)
                        is Double -> ksafe.put(ksafeKey, value)
                        is String -> ksafe.put(ksafeKey, value)
                        is Set<*> -> ksafe.put(ksafeKey, (value as Set<String>).toList())
                        else -> return@MigrationEntry false
                    }
                    true
                } else {
                    false
                }
            },
        )
    }
}
