package name.lmj0011.jetpackreleasetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AndroidXArtifact::class, AndroidXArtifactUpdate::class], version = 1,  exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract val androidXArtifactDao: AndroidXArtifactDao
    abstract val androidXArtifactUpdateDao: AndroidXArtifactUpdateDao

    companion object {

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
                    .build()
                }

                return instance
            }
        }
    }
}