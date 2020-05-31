package name.lmj0011.jetpackreleasetracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artifacts_table")
data class AndroidXArtifact (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override var id: Long = 0L,

    @ColumnInfo(name = "created_at")
    override var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    override var updatedAt: String = "",

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "packageName")
    var packageName: String = "",

    @ColumnInfo(name = "releasePageUrl")
    var releasePageUrl: String = "",

    @ColumnInfo(name = "latestStableVersion")
    var latestStableVersion: String? = "",

    @ColumnInfo(name = "latestVersion")
    var latestVersion: String? = ""
) : BaseEntity()