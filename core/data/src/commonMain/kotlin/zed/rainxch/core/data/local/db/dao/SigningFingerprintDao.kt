package zed.rainxch.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import zed.rainxch.core.data.local.db.entities.SigningFingerprintEntity

@Dao
interface SigningFingerprintDao {
    @Query("SELECT * FROM signing_fingerprints WHERE fingerprint = :fingerprint")
    suspend fun lookup(fingerprint: String): SigningFingerprintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<SigningFingerprintEntity>)

    @Query("SELECT MAX(observedAt) FROM signing_fingerprints")
    suspend fun lastSyncTimestamp(): Long?

    @Query("DELETE FROM signing_fingerprints WHERE source = 'user_link'")
    suspend fun clearUserLinks()
}
