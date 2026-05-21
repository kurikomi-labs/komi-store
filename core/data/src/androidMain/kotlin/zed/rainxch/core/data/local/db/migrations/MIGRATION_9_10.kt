package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 =
    object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN assetFilterRegex TEXT")
            db.execSQL(
                "ALTER TABLE installed_apps ADD COLUMN fallbackToOlderReleases INTEGER NOT NULL DEFAULT 0",
            )
        }
    }
