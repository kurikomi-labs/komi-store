package zed.rainxch.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.data.local.db.entities.ExternalLinkEntity

@Dao
interface ExternalLinkDao {
    @Query("SELECT * FROM external_links WHERE packageName = :packageName")
    suspend fun get(packageName: String): ExternalLinkEntity?

    @Query("SELECT * FROM external_links")
    fun observeAll(): Flow<List<ExternalLinkEntity>>

    @Query("SELECT * FROM external_links WHERE state = 'PENDING_REVIEW'")
    fun observePendingReview(): Flow<List<ExternalLinkEntity>>

    @Query("SELECT COUNT(*) FROM external_links WHERE state = 'PENDING_REVIEW'")
    fun observePendingReviewCount(): Flow<Int>

    @Query("SELECT packageName FROM external_links WHERE state IN ('MATCHED','NEVER_ASK')")
    suspend fun getDoNotRescanPackageNames(): List<String>

    @Query("SELECT packageName FROM external_links WHERE state = 'SKIPPED' AND skipExpiresAt > :now")
    suspend fun getActiveSkippedPackageNames(now: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: ExternalLinkEntity)

    @Query("DELETE FROM external_links WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM external_links WHERE state = 'SKIPPED' AND skipExpiresAt < :now")
    suspend fun pruneExpiredSkips(now: Long)

    @Query("DELETE FROM external_links WHERE state = 'PENDING_REVIEW' AND packageName NOT IN (:livePackages)")
    suspend fun prunePendingReviewNotIn(livePackages: Set<String>)
}
