package student.projects.tholagig

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
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

        // ADD THESE LINES:
        diagnoseNotificationIssue()
        requestNotificationPermission()
    }

    // ADD THIS METHOD:
    private fun diagnoseNotificationIssue() {
        Log.d("NOTIF_DIAGNOSE", "=== 🔍 NOTIFICATION DIAGNOSIS STARTED ===")

        // 1. Check FCM Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("NOTIF_DIAGNOSE", "✅ FCM TOKEN OBTAINED: ${token.take(20)}...")
                Log.d("NOTIF_DIAGNOSE", "📱 Full token length: ${token.length} characters")

                // Save token to Firestore (you'll need user ID after login)
                // saveFcmTokenToFirestore(token)

            } else {
                val exception = task.exception
                Log.e("NOTIF_DIAGNOSE", "❌ FCM TOKEN FAILED: ${exception?.message}")
                exception?.printStackTrace()

                // Specific error handling
                when {
                    exception?.message?.contains("SERVICE_NOT_AVAILABLE") == true -> {
                        Log.e("NOTIF_DIAGNOSE", "🔧 Issue: Google Play Services not available")
                    }
                    exception?.message?.contains("AUTHENTICATION_FAILED") == true -> {
                        Log.e("NOTIF_DIAGNOSE", "🔧 Issue: Authentication failed - check SHA-1 fingerprint")
                    }
                    else -> {
                        Log.e("NOTIF_DIAGNOSE", "🔧 Issue: Unknown FCM error")
                    }
                }
            }
        }

        // 2. Check Notification Channels
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = notificationManager.notificationChannels
            Log.d("NOTIF_DIAGNOSE", "📱 Notification Channels found: ${channels.size}")
            channels.forEach { channel ->
                Log.d("NOTIF_DIAGNOSE", "   - Channel: ${channel.id}, Importance: ${channel.importance}")
            }
        }

        // 3. Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("NOTIF_DIAGNOSE", "📋 Notification Permission: ${if (hasPermission) "GRANTED" else "NOT GRANTED"}")
        }

        // 4. Test if we can create a local notification
        testLocalNotification()

        Log.d("NOTIF_DIAGNOSE", "=== 🔍 NOTIFICATION DIAGNOSIS COMPLETE ===")
    }

    // ADD THIS METHOD:
    private fun testLocalNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            // Create test channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "TEST_CHANNEL_ID",
                    "Test Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for testing notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Build test notification
            val notification = androidx.core.app.NotificationCompat.Builder(this, "TEST_CHANNEL_ID")
                .setContentTitle("Test Notification")
                .setContentText("If you see this, local notifications work!")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(9999, notification)
            Log.d("NOTIF_DIAGNOSE", "✅ LOCAL NOTIFICATION TEST: Notification sent successfully")

        } catch (e: Exception) {
            Log.e("NOTIF_DIAGNOSE", "❌ LOCAL NOTIFICATION TEST FAILED: ${e.message}")
        }
    }

    // ADD THIS METHOD (you'll use it later after login):
    private fun saveFcmTokenToFirestore(token: String) {
        // You'll implement this after user login
        // For now, just log it
        Log.d("NOTIF_DIAGNOSE", "💾 Would save token to Firestore: ${token.take(10)}...")
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("APP_CRASH", "Uncaught exception in thread: ${thread.name}", throwable)
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                Log.d("NOTIF_DIAGNOSE", "📋 Requesting notification permission...")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Log.d("NOTIF_DIAGNOSE", "📋 Notification permission already granted")
            }
        }
    }

    // Add this to handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NOTIF_DIAGNOSE", "✅ Notification permission granted by user")
            } else {
                Log.w("NOTIF_DIAGNOSE", "⚠️ Notification permission denied by user")
            }
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