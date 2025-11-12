package student.projects.tholagig.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import student.projects.tholagig.models.OfflineJob
import student.projects.tholagig.models.OfflineApplication
import student.projects.tholagig.models.OfflineMessage

@Database(
    entities = [OfflineJob::class, OfflineApplication::class, OfflineMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tholagig_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}