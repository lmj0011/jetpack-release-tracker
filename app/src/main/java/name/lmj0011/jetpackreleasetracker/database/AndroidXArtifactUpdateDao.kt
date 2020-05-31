package name.lmj0011.jetpackreleasetracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AndroidXArtifactUpdateDao: BaseDao {
    @Insert
    fun actualInsert(artifactUpdate: AndroidXArtifactUpdate)

    fun insert(artifactUpdate: AndroidXArtifactUpdate) {
       actualInsert(setTimestamps(artifactUpdate) as AndroidXArtifactUpdate)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAll(artifactUpdates: MutableList<AndroidXArtifactUpdate>)

    fun insertAll(artifactUpdates: MutableList<AndroidXArtifactUpdate>) {
        val list = artifactUpdates.map { setTimestamps(it) as AndroidXArtifactUpdate }.toMutableList()
        actualInsertAll(list)
    }

    @Update
    fun actualUpdate(artifactUpdate: AndroidXArtifactUpdate)

    fun update(artifactUpdate: AndroidXArtifactUpdate) {
        actualUpdate(setUpdatedAt(artifactUpdate) as AndroidXArtifactUpdate)
    }

    @Update
    fun actualUpdateAll(artifactUpdates: MutableList<AndroidXArtifactUpdate>)

    fun updateAll(artifactUpdates: MutableList<AndroidXArtifactUpdate>) {
        val list = artifactUpdates.map { setUpdatedAt(it) as AndroidXArtifactUpdate }.toMutableList()
        actualUpdateAll(list)
    }

    // try to update, then insert row if it does not exists
    fun upsert(artifactUpdate: AndroidXArtifactUpdate) {
        when(get(artifactUpdate.id)) {
            is AndroidXArtifactUpdate -> { update(artifactUpdate) }
            else -> { insert(artifactUpdate) }
        }
    }

    @Query("SELECT * from artifact_update_table WHERE id = :key")
    fun get(key: Long): AndroidXArtifactUpdate?

    @Query("SELECT * FROM artifact_update_table ORDER BY id DESC")
    fun getAllAndroidXArtifactUpdates(): LiveData<MutableList<AndroidXArtifactUpdate>>

    @Query("DELETE from artifact_update_table WHERE id = :key")
    fun deleteByAndroidXArtifactUpdateId(key: Long): Int

    @Query("DELETE FROM artifact_update_table")
    fun clear()

}