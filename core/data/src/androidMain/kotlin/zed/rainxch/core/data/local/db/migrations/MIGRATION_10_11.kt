package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 =
    object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN preferredAssetVariant TEXT")
            db.execSQL(
                "ALTER TABLE installed_apps ADD COLUMN preferredVariantStale INTEGER NOT NULL DEFAULT 0",
            )
        }
    }
