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
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.models.Message
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ProfileActivity
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
    private var currentUserId: String = ""
    private var receiverId: String = ""
    private var receiverName: String = ""
    private var conversationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Initialize SessionManager first
        sessionManager = SessionManager(this)

        // Get intent data with fallbacks
        currentUserId = sessionManager.getUserId() ?: "user_${System.currentTimeMillis()}"
        receiverId = intent.getStringExtra("RECEIVER_ID") ?: "receiver_${System.currentTimeMillis()}"
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "Unknown User"
        conversationId = generateConversationId(currentUserId, receiverId)

        Log.d("MessagesActivity", "Starting with: CurrentUser=$currentUserId, Receiver=$receiverId, Name=$receiverName")

        initializeViews()
        setupBottomNavigation()
        setupRecyclerView()
        loadMessages()
    }

    private fun initializeViews() {
        try {
            rvMessages = findViewById(R.id.rvMessages)
            etMessage = findViewById(R.id.etMessage)
            btnSend = findViewById(R.id.btnSend)
            bottomNavigationView = findViewById(R.id.bottom_navigation)

            // Check if these views exist in your layout
            btnBack = findViewById(R.id.btnBack)
            tvRecipientName = findViewById(R.id.tvRecipientName)

            btnSend.setOnClickListener {
                sendMessage()
            }

            // Only set click listener if back button exists
            if (::btnBack.isInitialized) {
                btnBack.setOnClickListener {
                    onBackPressed()
                }
            }

            // Only set text if recipient name view exists
            if (::tvRecipientName.isInitialized) {
                tvRecipientName.text = receiverName
            }

        } catch (e: Exception) {
            Log.e("MessagesActivity", "Error initializing views: ${e.message}")
            Toast.makeText(this, "Error setting up messages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        val intent = Intent(this, FreelancerDashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_jobs -> {
                        val intent = Intent(this, JobBrowseActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_applications -> {
                        startActivity(Intent(this, MyApplicationsActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_messages -> {
                    //already here
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

        } catch (e: Exception) {
            Log.e("MessagesActivity", "Bottom navigation error: ${e.message}")
        }
    }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        try {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("MessagesActivity", "Navigation error: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messagesList, currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messagesAdapter
    }

    private fun loadMessages() {
        try {
            messagesList.clear()

            // Add sample messages
            messagesList.addAll(
                listOf(
                    Message(
                        "1",
                        conversationId,
                        receiverId,
                        currentUserId,
                        "Hi there!",
                        Date(System.currentTimeMillis() - 3600000),
                        true
                    ),
                    Message(
                        "2",
                        conversationId,
                        currentUserId,
                        receiverId,
                        "Hello! How can I help you?",
                        Date(System.currentTimeMillis() - 1800000),
                        true
                    )
                )
            )
            messagesAdapter.notifyDataSetChanged()

            if (messagesList.isNotEmpty()) {
                rvMessages.scrollToPosition(messagesList.size - 1)
            }

            Log.d("MessagesActivity", "Loaded ${messagesList.size} sample messages")

        } catch (e: Exception) {
            Log.e("MessagesActivity", "Error loading messages: ${e.message}")
        }
    }

    private fun sendMessage() {
        try {
            val content = etMessage.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return
            }

            val message = Message(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                timestamp = Date(),
                isRead = false
            )

            messagesList.add(message)
            messagesAdapter.notifyItemInserted(messagesList.size - 1)
            etMessage.text.clear()
            rvMessages.scrollToPosition(messagesList.size - 1)

            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()

            // Simulate reply
            simulateReply(content)

        } catch (e: Exception) {
            Log.e("MessagesActivity", "Error sending message: ${e.message}")
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simulateReply(userMessage: String) {
        try {
            btnSend.isEnabled = false

            android.os.Handler().postDelayed({
                val replyMessage = "Thanks for your message! I'll get back to you soon."

                val reply = Message(
                    messageId = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = receiverId,
                    receiverId = currentUserId,
                    content = replyMessage,
                    timestamp = Date(),
                    isRead = false
                )

                messagesList.add(reply)
                messagesAdapter.notifyItemInserted(messagesList.size - 1)
                rvMessages.scrollToPosition(messagesList.size - 1)
                btnSend.isEnabled = true

            }, 2000)
        } catch (e: Exception) {
            Log.e("MessagesActivity", "Error simulating reply: ${e.message}")
            btnSend.isEnabled = true
        }
    }

    private fun generateConversationId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        try {
            bottomNavigationView.selectedItemId = R.id.nav_messages
        } catch (e: Exception) {
            Log.e("MessagesActivity", "Error in onResume: ${e.message}")
        }
    }
}