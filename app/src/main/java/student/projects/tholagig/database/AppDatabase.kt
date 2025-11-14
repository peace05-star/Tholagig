package student.projects.tholagig.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import student.projects.tholagig.models.OfflineJob
import student.projects.tholagig.models.OfflineApplication
import student.projects.tholagig.models.OfflineMessage

@Database(
    entities = [OfflineJob::class, OfflineApplication::class, OfflineMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // ADD THIS LINE
abstract class AppDatabase : RoomDatabase() {
    abstract fun JobDao(): JobDao
    abstract fun ApplicationDao(): ApplicationDao
    abstract fun MessageDao(): MessageDao

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