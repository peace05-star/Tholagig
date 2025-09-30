package student.projects.tholagig.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("TholaGigPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "USER_ID"
        private const val KEY_USER_TYPE = "USER_TYPE"
        private const val KEY_EMAIL = "EMAIL"
        private const val KEY_USER_NAME = "USER_NAME" // Add this
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
    }

    // Update save method to include name
    fun saveUserSession(userId: String, userType: String, email: String, userName: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_TYPE, userType)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            userName?.let { putString(KEY_USER_NAME, it) }
            apply()
        }
    }

    // Add getUserName method
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    // Existing methods
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserType(): String? = prefs.getString(KEY_USER_TYPE, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }
}