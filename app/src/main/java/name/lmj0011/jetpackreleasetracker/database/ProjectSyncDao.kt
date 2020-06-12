package name.lmj0011.jetpackreleasetracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProjectSyncDao: BaseDao {
    @Insert
    fun actualInsert(artifactUpdate: ProjectSync)

    fun insert(artifactUpdate: ProjectSync) {
        actualInsert(setTimestamps(artifactUpdate) as ProjectSync)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAll(artifactUpdates: MutableList<ProjectSync>)

    fun insertAll(artifactUpdates: MutableList<ProjectSync>) {
        val list = artifactUpdates.map { setTimestamps(it) as ProjectSync }.toMutableList()
        actualInsertAll(list)
    }

    @Update
    fun actualUpdate(artifactUpdate: ProjectSync)

    fun update(artifactUpdate: ProjectSync) {
        actualUpdate(setUpdatedAt(artifactUpdate) as ProjectSync)
    }

    @Update
    fun actualUpdateAll(artifactUpdates: MutableList<ProjectSync>)

    fun updateAll(artifactUpdates: MutableList<ProjectSync>) {
        val list = artifactUpdates.map { setUpdatedAt(it) as ProjectSync }.toMutableList()
        actualUpdateAll(list)
    }

    @Query("SELECT * from project_syncs_table WHERE id = :key")
    fun get(key: Long): ProjectSync?

    @Query("SELECT * FROM project_syncs_table ORDER BY id DESC")
    fun getAllProjectSyncs(): LiveData<MutableList<ProjectSync>>

    @Query("DELETE from project_syncs_table WHERE id = :key")
    fun deleteByProjectSyncId(key: Long): Int

    @Query("DELETE FROM project_syncs_table")
    fun clear()
}