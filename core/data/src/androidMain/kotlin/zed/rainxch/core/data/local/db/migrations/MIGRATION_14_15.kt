package zed.rainxch.core.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15 =
    object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS external_links (
                    packageName TEXT NOT NULL PRIMARY KEY,
                    state TEXT NOT NULL,
                    repoOwner TEXT,
                    repoName TEXT,
                    matchSource TEXT,
                    matchConfidence REAL,
                    signingFingerprint TEXT,
                    installerKind TEXT,
                    firstSeenAt INTEGER NOT NULL,
                    lastReviewedAt INTEGER NOT NULL,
                    skipExpiresAt INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_external_links_repoOwner_repoName
                    ON external_links (repoOwner, repoName)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS signing_fingerprints (
                    fingerprint TEXT NOT NULL PRIMARY KEY,
                    repoOwner TEXT NOT NULL,
                    repoName TEXT NOT NULL,
                    source TEXT NOT NULL,
                    observedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_signing_fingerprints_repoOwner_repoName
                    ON signing_fingerprints (repoOwner, repoName)
                """.trimIndent(),
            )
        }
    }
