package name.lmj0011.jetpackreleasetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AndroidXArtifact::class, AndroidXArtifactUpdate::class, ProjectSync::class], version = 2,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val androidXArtifactDao: AndroidXArtifactDao
    abstract val androidXArtifactUpdateDao: AndroidXArtifactUpdateDao
    abstract val projectSyncDao: ProjectSyncDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // create ProjectSync table
                database.execSQL("CREATE TABLE IF NOT EXISTS `project_syncs_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL,"+
                        "`name` TEXT NOT NULL, `upToDateCount` INTEGER NOT NULL, `outdatedCount` INTEGER NOT NULL,"+
                        "`depsListUrl` TEXT NOT NULL, `stableVersionsOnly` INTEGER NOT NULL, `androidxArtifactsIds` TEXT NOT NULL )")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                }

                return instance
            }
        }
    }
}