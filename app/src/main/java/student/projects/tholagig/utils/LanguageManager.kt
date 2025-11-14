package student.projects.tholagig.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

class LanguageManager(private val context: Context) {

    companion object {
        const val ENGLISH = "en"
        const val ZULU = "zu"
        const val AFRIKAANS = "af"
        const val LANGUAGE_PREFERENCE = "language_preference"
        const val SELECTED_LANGUAGE = "selected_language"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(LANGUAGE_PREFERENCE, Context.MODE_PRIVATE)

    fun setLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Save selected language
        sharedPreferences.edit().putString(SELECTED_LANGUAGE, languageCode).apply()
    }

    fun getCurrentLanguage(): String {
        return sharedPreferences.getString(SELECTED_LANGUAGE, ENGLISH) ?: ENGLISH
    }

    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            ENGLISH -> "English"
            ZULU -> "isiZulu"
            AFRIKAANS -> "Afrikaans"
            else -> "English"
        }
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            Pair(ENGLISH, getLanguageName(ENGLISH)),
            Pair(ZULU, getLanguageName(ZULU)),
            Pair(AFRIKAANS, getLanguageName(AFRIKAANS))
        )
    }

    fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
        } else {
            configuration.locale = locale
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
    }
}