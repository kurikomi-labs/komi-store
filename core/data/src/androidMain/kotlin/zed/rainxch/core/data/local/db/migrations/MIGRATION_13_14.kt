package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 =
    object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN pendingInstallVersion TEXT")
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN pendingInstallAssetName TEXT")
        }
    }
