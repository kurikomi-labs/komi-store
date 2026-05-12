package zed.rainxch.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.data.local.db.entities.HiddenRepoEntity

@Dao
interface HiddenRepoDao {
    @Query("SELECT repoId FROM hidden_repos")
    fun getAllHiddenRepoIds(): Flow<List<Long>>

    @Query("SELECT * FROM hidden_repos ORDER BY hiddenAt DESC")
    fun getAllHiddenRepos(): Flow<List<HiddenRepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenRepoEntity)

    @Query("DELETE FROM hidden_repos WHERE repoId = :repoId")
    suspend fun deleteById(repoId: Long)

    @Query("DELETE FROM hidden_repos")
    suspend fun clearAll()
}
