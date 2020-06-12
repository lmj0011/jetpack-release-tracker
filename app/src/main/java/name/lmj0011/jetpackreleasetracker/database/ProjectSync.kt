package name.lmj0011.jetpackreleasetracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_syncs_table")
data class ProjectSync (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override var id: Long = 0L,

    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "upToDateCount")
    var upToDateCount: Int = 0,

    @ColumnInfo(name = "outdatedCount")
    var outdatedCount: Int = 0,

    // url pointing to this Project's dependencies list
    @ColumnInfo(name = "depsListUrl")
    var depsListUrl: String = "",

    // Used to determine whether or not to sync this project against stabled versions of it's dependencies
    @ColumnInfo(name = "stableVersionsOnly")
    var stableVersionsOnly: Boolean = false,

    @ColumnInfo(name = "androidxArtifactsIds")
    var androidxArtifactsIds: MutableList<Long> = mutableListOf()
) : BaseEntity()