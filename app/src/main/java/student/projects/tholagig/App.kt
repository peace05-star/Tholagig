package student.projects.tholagig

import android.app.Application
import student.projects.tholagig.database.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize database (creates it on first access)
        AppDatabase.getInstance(this)
    }
}