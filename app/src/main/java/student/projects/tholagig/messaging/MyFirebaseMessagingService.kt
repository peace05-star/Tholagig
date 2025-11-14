package student.projects.tholagig.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "📱 Message received: ${remoteMessage.data}")

        // Handle FCM messages when app is in foreground
        if (remoteMessage.data.isNotEmpty()) {
            val messageData = remoteMessage.data
            val title = messageData["title"] ?: "New Message"
            val body = messageData["body"] ?: "You have a new message"
            val senderId = messageData["senderId"]

            createNotification(title, body, senderId)
        }

        // Also handle notification payload if present
        remoteMessage.notification?.let { notification ->
            createNotification(
                notification.title ?: "New Message",
                notification.body ?: "You have a new message",
                null
            )
        }
    }

    private fun createNotification(title: String, body: String, senderId: String?) {
        try {
            // Create and show system notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "MESSAGES_CHANNEL",
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New message notifications"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create intent to open ConversationsActivity
            val intent = Intent(this, ConversationsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Add sender ID as extra if available
                senderId?.let { putExtra("senderId", it) }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = NotificationCompat.Builder(this, "MESSAGES_CHANNEL")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(getNotificationIcon())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(1000, 1000)) // Vibrate pattern
                .build()

            // Show notification with unique ID
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d("FCM", "✅ Notification created: $title - $body")

        } catch (e: Exception) {
            Log.e("FCM", "❌ Error creating notification: ${e.message}")
        }
    }

    private fun getNotificationIcon(): Int {
        // Use your app's launcher icon or a message icon
        // Make sure this drawable exists in your resources
        return resources.getIdentifier("ic_message", "drawable", packageName)
            .takeIf { it != 0 } ?: android.R.drawable.ic_dialog_email
    }

    override fun onNewToken(token: String) {
        // Save this token to your server/database for sending notifications
        Log.d("FCM", "🆕 New FCM token: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        try {
            // Save token to user document in Firestore
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            userId?.let {
                Firebase.firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "✅ FCM token saved to Firestore for user: $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "❌ Failed to save FCM token: ${e.message}")
                    }
            } ?: Log.w("FCM", "⚠️ No user logged in, cannot save FCM token")

        } catch (e: Exception) {
            Log.e("FCM", "❌ Error saving FCM token: ${e.message}")
        }
    }
}