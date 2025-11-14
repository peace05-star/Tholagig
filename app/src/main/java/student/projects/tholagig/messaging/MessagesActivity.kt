package student.projects.tholagig.messaging

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.models.Message
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ProfileActivity
import student.projects.tholagig.utils.NotificationHelper
import java.util.*

class MessagesActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnBack: ImageButton
    private lateinit var tvRecipientName: TextView

    private lateinit var messagesAdapter: MessagesAdapter
    private val messagesList = mutableListOf<Message>()

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private var currentUserId: String = ""
    private var receiverId: String = ""
    private var receiverName: String = ""
    private var conversationId: String = ""

    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        Log.d("MessagesActivity", "🚀 Starting MessagesActivity...")

        // Initialize services
        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        // Initialize FCM (for demonstration)
        NotificationHelper(this).initializeFCM()

        // Get current user ID
        currentUserId = sessionManager.getUserId() ?: "demo_user_${System.currentTimeMillis()}"

        // Get receiver data from intent
        receiverId = intent.getStringExtra("RECEIVER_ID") ?: "demo_receiver_${System.currentTimeMillis()}"
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "Demo User"

        // Generate conversation ID
        conversationId = generateConversationId(currentUserId, receiverId)

        Log.d("MessagesActivity", "📱 Current User: $currentUserId")
        Log.d("MessagesActivity", "📱 Receiver: $receiverId ($receiverName)")
        Log.d("MessagesActivity", "📱 Conversation: $conversationId")

        initializeViews()
        setupBottomNavigation()
        setupRecyclerView()
        setupRealTimeMessages()
    }

    private fun initializeViews() {
        try {
            rvMessages = findViewById(R.id.rvMessages)
            etMessage = findViewById(R.id.etMessage)
            btnSend = findViewById(R.id.btnSend)
            bottomNavigationView = findViewById(R.id.bottom_navigation)

            // Set recipient name
            tvRecipientName = findViewById(R.id.tvRecipientName)
            tvRecipientName.text = receiverName

            // Set up back button
            btnBack = findViewById(R.id.btnBack)
            btnBack.setOnClickListener {
                onBackPressed()
            }

            // Set up send button
            btnSend.setOnClickListener {
                sendMessage()
            }

            Log.d("MessagesActivity", "✅ Views initialized successfully")

        } catch (e: Exception) {
            Log.e("MessagesActivity", "❌ Error initializing views: ${e.message}")
            Toast.makeText(this, "Error setting up messages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(Intent(this, FreelancerDashboardActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_jobs -> {
                        startActivity(Intent(this, JobBrowseActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_applications -> {
                        startActivity(Intent(this, MyApplicationsActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_messages -> {
                        // Already here
                        true
                    }
                    R.id.nav_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }

            bottomNavigationView.selectedItemId = R.id.nav_messages
            Log.d("MessagesActivity", "✅ Bottom navigation setup")

        } catch (e: Exception) {
            Log.e("MessagesActivity", "❌ Bottom navigation error: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messagesList, currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messagesAdapter
        Log.d("MessagesActivity", "✅ RecyclerView setup")
    }

    private fun setupRealTimeMessages() {
        Log.d("MessagesActivity", "🔍 Setting up real-time messages listener...")

        messagesListener = firebaseService.listenToMessages(
            conversationId = conversationId,
            onMessagesReceived = { messages ->
                Log.d("MessagesActivity", "📨 Received ${messages.size} messages")

                runOnUiThread {
                    messagesList.clear()
                    messagesList.addAll(messages)
                    messagesAdapter.notifyDataSetChanged()

                    if (messagesList.isNotEmpty()) {
                        rvMessages.scrollToPosition(messagesList.size - 1)
                    }

                    Log.d("MessagesActivity", "✅ Updated UI with ${messages.size} messages")
                }
            },
            onError = { error ->
                Log.e("MessagesActivity", "❌ Error in real-time messages: ${error.message}")
                runOnUiThread {
                    Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Log.d("MessagesActivity", "✅ Real-time listener setup complete")
    }

    private fun sendMessage() {
        try {
            val content = etMessage.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("MessagesActivity", "📤 Sending message: $content")

            val message = Message(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                timestamp = Date(),
                isRead = false
            )

            // OPTION 1: Use FirebaseService's sendMessage (with coroutines)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = firebaseService.sendMessage(message)

                    // Switch to Main thread for UI updates
                    CoroutineScope(Dispatchers.Main).launch {
                        if (result.isSuccess) {
                            etMessage.text.clear()
                            Log.d("MessagesActivity", "✅ Message sent successfully")
                            Toast.makeText(this@MessagesActivity, "Message sent!", Toast.LENGTH_SHORT).show()

                            // Create a demo notification
                            createDemoNotification(content)

                        } else {
                            Toast.makeText(this@MessagesActivity, "Error sending message", Toast.LENGTH_SHORT).show()
                            Log.e("MessagesActivity", "❌ Failed to send message")
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@MessagesActivity, "Error sending message", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MessagesActivity", "❌ Error in coroutine: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("MessagesActivity", "❌ Error sending message: ${e.message}")
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDemoNotification(messageContent: String) {
        // Use coroutine for the notification
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a demo notification in Firestore
                firebaseService.createDemoNotification(
                    receiverId = receiverId,
                    title = "New message from ${getCurrentUserName()}",
                    message = messageContent,
                    conversationId = conversationId
                )

                Log.d("MessagesActivity", "📢 Demo notification created")

            } catch (e: Exception) {
                Log.e("MessagesActivity", "❌ Error creating demo notification: ${e.message}")
            }
        }
    }

    // ALTERNATIVE SIMPLE METHOD (if you prefer not using coroutines):
    private fun sendMessageSimple() {
        try {
            val content = etMessage.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("MessagesActivity", "📤 Sending message: $content")

            val message = Message(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                timestamp = Date(),
                isRead = false
            )

            // Use the public getter to access Firestore directly
            firebaseService.getFirestore().collection("messages")
                .document(message.messageId)
                .set(message)
                .addOnSuccessListener {
                    // Message sent successfully
                    runOnUiThread {
                        etMessage.text.clear()
                        Log.d("MessagesActivity", "✅ Message sent successfully")
                        Toast.makeText(this@MessagesActivity, "Message sent!", Toast.LENGTH_SHORT).show()

                        // Create demo notification
                        createSimpleNotification(content)
                    }
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        Toast.makeText(this@MessagesActivity, "Error sending message", Toast.LENGTH_SHORT).show()
                        Log.e("MessagesActivity", "❌ Failed to send message: ${e.message}")
                    }
                }

        } catch (e: Exception) {
            Log.e("MessagesActivity", "❌ Error sending message: ${e.message}")
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createSimpleNotification(messageContent: String) {
        val notificationData = hashMapOf(
            "receiverId" to receiverId,
            "title" to "New message from ${getCurrentUserName()}",
            "message" to messageContent,
            "conversationId" to conversationId,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        firebaseService.getFirestore().collection("demo_notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("MessagesActivity", "📢 Demo notification created")
            }
            .addOnFailureListener { e ->
                Log.e("MessagesActivity", "❌ Error creating notification: ${e.message}")
            }
    }

    private fun getCurrentUserName(): String {
        // In a real app, you'd get this from user data
        return "You" // This would be the current user's actual name
    }

    private fun generateConversationId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        Log.d("MessagesActivity", "🧹 Cleaned up resources")
    }

    override fun onResume() {
        super.onResume()
        try {
            bottomNavigationView.selectedItemId = R.id.nav_messages
        } catch (e: Exception) {
            Log.e("MessagesActivity", "❌ Error in onResume: ${e.message}")
        }
    }
}