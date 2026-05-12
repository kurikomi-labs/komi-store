package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_16_17 =
    object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS hidden_repos (
                    repoId INTEGER NOT NULL PRIMARY KEY,
                    repoName TEXT NOT NULL,
                    repoOwner TEXT NOT NULL,
                    repoOwnerAvatarUrl TEXT NOT NULL,
                    hiddenAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }
