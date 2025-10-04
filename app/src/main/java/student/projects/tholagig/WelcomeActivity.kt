package student.projects.tholagig

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.auth.UserTypeSelectionActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up a safer crash handler
        setupCrashHandler()

        setContentView(R.layout.activity_welcome)

        Log.d("WELCOME_DEBUG", "WelcomeActivity created successfully")

        initializeViews()
        setupClickListeners()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("APP_CRASH", "Uncaught exception in thread: ${thread.name}", throwable)

            // Log to file or analytics service instead of showing UI
            // Don't try to show Toasts or use UI thread during crash

            // Call the original handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeViews() {
        try {
            btnSignIn = findViewById(R.id.btnSignIn)
            btnSignUp = findViewById(R.id.btnSignUp)
            Log.d("WELCOME_DEBUG", "Views initialized successfully")
        } catch (e: Exception) {
            Log.e("WELCOME_DEBUG", "Error initializing views: ${e.message}")
            // Don't show Toast here during initialization issues
        }
    }

    private fun setupClickListeners() {
        btnSignIn.setOnClickListener {
            Log.d("WELCOME_DEBUG", "Sign In button clicked")
            navigateToLogin()
        }

        btnSignUp.setOnClickListener {
            Log.d("WELCOME_DEBUG", "Sign Up button clicked")
            navigateToUserTypeSelection()
        }
    }

    private fun navigateToLogin() {
        try {
            Log.d("WELCOME_DEBUG", "Navigating to LoginActivity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Optional: add finish() if you don't want to come back to welcome
        } catch (e: Exception) {
            Log.e("WELCOME_DEBUG", "Error navigating to LoginActivity: ${e.message}", e)
        }
    }

    private fun navigateToUserTypeSelection() {
        try {
            Log.d("WELCOME_DEBUG", "Navigating to UserTypeSelectionActivity")
            val intent = Intent(this, UserTypeSelectionActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WELCOME_DEBUG", "Error navigating to UserTypeSelection: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("WELCOME_DEBUG", "WelcomeActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d("WELCOME_DEBUG", "WelcomeActivity paused")
    }
}