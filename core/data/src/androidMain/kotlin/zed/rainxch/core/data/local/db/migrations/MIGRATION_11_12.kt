package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 =
    object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN preferredAssetTokens TEXT")
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN assetGlobPattern TEXT")
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN pickedAssetIndex INTEGER")
            db.execSQL("ALTER TABLE installed_apps ADD COLUMN pickedAssetSiblingCount INTEGER")
        }
    }
