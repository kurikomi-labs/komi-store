package zed.rainxch.core.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import zed.rainxch.core.data.local.DesktopAppDataPaths
import java.io.File

fun initDatabase(): AppDatabase {
    // SQLite WAL mode keeps two side files alongside the .db. Migrate sidecars
    // FIRST so the .db never lands at the new location without its WAL — if
    // the WAL/SHM copies fail, we abort before touching the .db and let the
    // user retry next launch (the legacy files are still in tmp).
    DesktopAppDataPaths.migrateFromTmpIfNeeded("github_store.db-wal")
    DesktopAppDataPaths.migrateFromTmpIfNeeded("github_store.db-shm")
    DesktopAppDataPaths.migrateFromTmpIfNeeded("github_store.db")

    val dbFile = File(DesktopAppDataPaths.appDataDir(), "github_store.db")
    return Room
        .databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
