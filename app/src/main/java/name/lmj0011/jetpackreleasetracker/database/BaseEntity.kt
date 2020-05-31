package name.lmj0011.jetpackreleasetracker.database

abstract class BaseEntity {
    abstract var id: Long

    abstract var createdAt: String

    abstract var updatedAt: String
}