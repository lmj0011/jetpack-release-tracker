package name.lmj0011.jetpackreleasetracker.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

@Dao
interface ProjectSyncDao: BaseDao {
    @Insert
    fun actualInsert(project: ProjectSync)

    fun insert(project: ProjectSync) {
        actualInsert(setTimestamps(project) as ProjectSync)
    }

    // try to update, then insert row if it does not exists
    fun upsert(project: ProjectSync) {
        when(get(project.id)) {
            is ProjectSync -> { update(project) }
            else -> { insert(project) }
        }
    }


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun actualInsertAll(projects: MutableList<ProjectSync>)

    fun insertAll(projects: MutableList<ProjectSync>) {
        val list = projects.map { setTimestamps(it) as ProjectSync }.toMutableList()
        actualInsertAll(list)
    }

    @Update
    fun actualUpdate(project: ProjectSync)

    fun update(project: ProjectSync) {
        actualUpdate(setUpdatedAt(project) as ProjectSync)
    }

    @Update
    fun actualUpdateAll(projects: MutableList<ProjectSync>)

    fun updateAll(projects: MutableList<ProjectSync>) {
        val list = projects.map { setUpdatedAt(it) as ProjectSync }.toMutableList()
        actualUpdateAll(list)
    }

    @Query("SELECT * from project_syncs_table WHERE id = :key")
    fun get(key: Long): ProjectSync?

    @Query("SELECT * FROM project_syncs_table ORDER BY id DESC")
    fun getAllProjectSyncs(): LiveData<MutableList<ProjectSync>>

    @Query("SELECT * FROM project_syncs_table ORDER BY id DESC")
    fun getAllProjectSyncsForWorker(): MutableList<ProjectSync>

    @Query("SELECT * FROM artifacts_table ORDER BY id DESC")
    fun getAllAndroidXArtifacts(): MutableList<AndroidXArtifact>

    @Query("DELETE from project_syncs_table WHERE id = :key")
    fun deleteByProjectSyncId(key: Long): Int

    @Query("DELETE FROM project_syncs_table")
    fun clear()
}