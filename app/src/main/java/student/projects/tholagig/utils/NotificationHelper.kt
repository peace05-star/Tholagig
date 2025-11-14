package student.projects.tholagig.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import student.projects.tholagig.R
import student.projects.tholagig.messaging.ConversationsActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val MESSAGES_CHANNEL_ID = "MESSAGES_CHANNEL"
        const val MESSAGES_NOTIFICATION_ID = 1001

        fun getFCMToken(context: Context): String? {
            val sharedPref = context.getSharedPreferences("tholagig_prefs", Context.MODE_PRIVATE)
            return sharedPref.getString("fcm_token", null)
        }
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
        initializeFCM()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MESSAGES_CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "✅ Notification channel created")
        }
    }

    fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("NotificationHelper", "❌ FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("NotificationHelper", "✅ FCM Token: ${token.take(20)}...")
            storeFCMToken(token)
        }
    }

    private fun storeFCMToken(token: String) {
        val sharedPref = context.getSharedPreferences("tholagig_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            apply()
        }
        Log.d("NotificationHelper", "💾 FCM token stored locally")
    }

    // Show notification that opens ConversationsActivity
    fun showMessageNotification(senderName: String, message: String) {
        try {
            Log.d("NotificationHelper", "📲 Creating notification: $senderName - $message")

            // Create intent to open ConversationsActivity
            val intent = Intent(context, ConversationsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // You can add extras if needed
                putExtra("notification_sender", senderName)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
                .setContentTitle("💬 New message from $senderName")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_email) // Using system icon for now
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(1000, 1000))
                .build()

            // Show notification with unique ID
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d("NotificationHelper", "✅ Notification shown: $senderName - $message")

        } catch (e: Exception) {
            Log.e("NotificationHelper", "❌ Error showing notification: ${e.message}")
        }
    }

    // Test notification
    fun testLocalNotification() {
        showMessageNotification("Test User", "This is a test message from TholaGig!")
    }
}