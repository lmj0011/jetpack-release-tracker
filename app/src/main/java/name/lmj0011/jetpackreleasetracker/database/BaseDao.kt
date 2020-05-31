package name.lmj0011.jetpackreleasetracker.database

import androidx.room.Dao

@Dao
interface BaseDao {

    fun setCreatedAt(entity: BaseEntity): BaseEntity {
        val iso8061Date = java.time.ZonedDateTime.now().toOffsetDateTime().toString()
        entity.createdAt = iso8061Date
        return entity
    }


    fun setUpdatedAt(entity: BaseEntity): BaseEntity {
        val iso8061Date = java.time.ZonedDateTime.now().toOffsetDateTime().toString()
        entity.updatedAt = iso8061Date
        return entity
    }

    // sets both created_at and updated_at
    fun setTimestamps(entity: BaseEntity): BaseEntity {
        val iso8061Date = java.time.ZonedDateTime.now().toOffsetDateTime().toString()
        entity.createdAt = iso8061Date
        entity.updatedAt = iso8061Date
        return entity
    }
}