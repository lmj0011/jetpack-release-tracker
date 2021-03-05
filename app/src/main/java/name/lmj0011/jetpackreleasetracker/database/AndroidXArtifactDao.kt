package name.lmj0011.jetpackreleasetracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AndroidXArtifactDao: BaseDao {
    @Insert
    fun actualInsert(artifact: AndroidXArtifact)

    fun insert(artifact: AndroidXArtifact) {
       actualInsert(setTimestamps(artifact) as AndroidXArtifact)
    }

    @Insert
    fun actualInsertArtifactUpdate(artifactUpdate: AndroidXArtifactUpdate)

    fun insertArtifactUpdate(artifactUpdate: AndroidXArtifactUpdate) {
        actualInsertArtifactUpdate(setTimestamps(artifactUpdate) as AndroidXArtifactUpdate)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAll(artifacts: MutableList<AndroidXArtifact>)

    fun insertAll(artifacts: MutableList<AndroidXArtifact>) {
        val list = artifacts.map { setTimestamps(it) as AndroidXArtifact }.toMutableList()
        actualInsertAll(list)
    }

    @Update
    fun actualUpdate(artifact: AndroidXArtifact)

    fun update(artifact: AndroidXArtifact) {
        actualUpdate(setUpdatedAt(artifact) as AndroidXArtifact)
    }

    @Update
    fun actualUpdateAll(artifacts: MutableList<AndroidXArtifact>)

    fun updateAll(artifacts: MutableList<AndroidXArtifact>) {
        val list = artifacts.map { setUpdatedAt(it) as AndroidXArtifact }.toMutableList()
        actualUpdateAll(list)
    }

    // try to update, then insert row if it does not exists
    fun upsert(artifact: AndroidXArtifact) {
        when(get(artifact.id)) {
            is AndroidXArtifact -> { update(artifact) }
            else -> { insert(artifact) }
        }
    }

    @Query("SELECT * from artifacts_table WHERE id = :key")
    fun get(key: Long): AndroidXArtifact?

    @Query("SELECT * FROM artifacts_table ORDER BY id DESC")
    fun getAllAndroidXArtifactsObserverable(): LiveData<MutableList<AndroidXArtifact>>

    @Query("SELECT * FROM artifacts_table ORDER BY id DESC")
    fun getAllAndroidXArtifacts(): MutableList<AndroidXArtifact>

    @Query("DELETE from artifacts_table WHERE id = :key")
    fun deleteByAndroidXArtifactId(key: Long): Int

    @Query("DELETE FROM artifacts_table")
    fun clear()

}