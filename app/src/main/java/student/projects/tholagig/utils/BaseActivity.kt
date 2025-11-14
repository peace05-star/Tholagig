package student.projects.tholagig

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import student.projects.tholagig.utils.LanguageManager

open class BaseActivity : AppCompatActivity() {

    protected lateinit var languageManager: LanguageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageManager = LanguageManager(this)
        applySavedLanguage()
    }

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        val currentLanguage = languageManager.getCurrentLanguage()
        super.attachBaseContext(languageManager.updateResources(newBase, currentLanguage))
    }

    private fun applySavedLanguage() {
        val currentLanguage = languageManager.getCurrentLanguage()
        languageManager.setLanguage(currentLanguage)
    }

    protected fun showLanguageDialog() {
        val languages = languageManager.getAvailableLanguages()
        val languageNames = languages.map { it.second }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languageNames) { dialog, which ->
                val selectedLanguageCode = languages[which].first
                languageManager.setLanguage(selectedLanguageCode)
                recreate() // Restart activity to apply language changes
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}