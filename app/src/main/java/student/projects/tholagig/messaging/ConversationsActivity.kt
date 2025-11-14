package student.projects.tholagig.messaging

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import student.projects.tholagig.Adapters.ConversationsAdapter
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.ClientDashboardActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.jobs.MyJobsActivity
import student.projects.tholagig.jobs.ApplicationsManagementActivity
import student.projects.tholagig.models.Conversation
import student.projects.tholagig.models.Message
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ClientProfileActivity
import student.projects.tholagig.profile.ProfileActivity
import student.projects.tholagig.utils.NotificationHelper
import android.app.NotificationManager


class ConversationsActivity : AppCompatActivity() {

    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var rvConversations: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnNewMessage: ImageButton
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnStartConversation: Button

    private lateinit var conversationsAdapter: ConversationsAdapter
    private val conversationsList = mutableListOf<Conversation>()

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private var currentUserId: String = ""
    private var currentUserType: String = ""
    private var isClient: Boolean = false
    private var isActivityVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        Log.d("ConversationsActivity", "🚀 Starting ConversationsActivity...")

        // Initialize services
        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()
        currentUserId = sessionManager.getUserId() ?: ""
        currentUserType = sessionManager.getUserType() ?: ""
        isClient = currentUserType == "client"
        notificationHelper = NotificationHelper(this)

        initializeViews()
        setupBottomNavigation()
        setupRecyclerView()
        setupClickListeners()

        handleNotificationClick(intent)

        setupRealTimeMessageListener()
        loadRealConversations()
    }

    private fun initializeViews() {
        rvConversations = findViewById(R.id.rvConversations)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvTitle = findViewById(R.id.tvTitle)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        btnNewMessage = findViewById(R.id.btnNewMessage)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        btnStartConversation = findViewById(R.id.btnStartConversation)

        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        // Update title based on user type
        tvTitle.text = if (isClient) "Client Messages" else "My Messages"

        // Hide the old empty state TextView since we have a new layout
        tvEmptyState.visibility = View.GONE
    }

    private fun setupBottomNavigation() {
        if (isClient) {
            // Client bottom navigation
            bottomNavigationView.menu.clear()
            bottomNavigationView.inflateMenu(R.menu.menu_bottom_navigation_client)

            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> {
                        navigateToActivity(ClientDashboardActivity::class.java)
                        true
                    }
                    R.id.nav_jobs -> {
                        navigateToActivity(MyJobsActivity::class.java)
                        true
                    }
                    R.id.nav_applications -> {
                        navigateToActivity(ApplicationsManagementActivity::class.java)
                        true
                    }
                    R.id.nav_messages -> {
                        // Already on messages page
                        true
                    }
                    R.id.nav_profile -> {
                        navigateToActivity(ClientProfileActivity::class.java)
                        true
                    }
                    else -> false
                }
            }
        } else {
            // Freelancer bottom navigation
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        navigateToActivity(FreelancerDashboardActivity::class.java)
                        true
                    }
                    R.id.nav_jobs -> {
                        navigateToActivity(JobBrowseActivity::class.java)
                        true
                    }
                    R.id.nav_applications -> {
                        navigateToActivity(MyApplicationsActivity::class.java)
                        true
                    }
                    R.id.nav_messages -> {
                        // Already on messages page
                        true
                    }
                    R.id.nav_profile -> {
                        navigateToActivity(ProfileActivity::class.java)
                        true
                    }
                    else -> false
                }
            }
        }
        bottomNavigationView.selectedItemId = if (isClient) R.id.nav_messages else R.id.nav_messages
    }

    private fun setupClickListeners() {
        btnNewMessage.setOnClickListener {
            showNewMessageOptions()
        }

        btnStartConversation.setOnClickListener {
            showNewMessageOptions()
        }
    }

    private fun handleNotificationClick(intent: Intent?) {
        val senderId = intent?.getStringExtra("NOTIFICATION_SENDER_ID")
        if (senderId != null && senderId != "unknown") {
            Log.d("NOTIFICATIONS", "🎯 Opening conversation from notification with: $senderId")

            // Find the conversation and open it
            CoroutineScope(Dispatchers.IO).launch {
                val userResult = firebaseService.getUserById(senderId)
                withContext(Dispatchers.Main) {
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        val userName = user?.fullName ?: "Unknown User"

                        val messagesIntent = Intent(this@ConversationsActivity, MessagesActivity::class.java).apply {
                            putExtra("RECEIVER_ID", senderId)
                            putExtra("RECEIVER_NAME", userName)
                        }
                        startActivity(messagesIntent)
                    }
                }
            }
        }
    }
    private fun showNewMessageOptions() {
        if (isClient) {
            showClientNewMessageOptions()
        } else {
            showFreelancerNewMessageOptions()
        }
    }

    private fun showClientNewMessageOptions() {
        val options = arrayOf(
            "Message Hired Freelancers",
            "Contact Applicants",
            "Search Users",
            "Create Group Chat"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("New Message")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openHiredFreelancersList()
                    1 -> openApplicantsList()
                    2 -> searchUsers()
                    3 -> createGroupChat()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFreelancerNewMessageOptions() {
        val options = arrayOf(
            "Message Clients I Applied To",
            "Search Clients",
            "Contact Support"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("New Message")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openAppliedClientsList()
                    1 -> searchClients()
                    2 -> contactSupport()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openHiredFreelancersList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val applicationsResult = firebaseService.getApplicationsByClient(currentUserId)

                withContext(Dispatchers.Main) {
                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        val hiredFreelancers = applications.filter {
                            it.status.equals("accepted", ignoreCase = true) ||
                                    it.status.equals("hired", ignoreCase = true)
                        }

                        if (hiredFreelancers.isEmpty()) {
                            showNoHiredFreelancersDialog()
                        } else {
                            showFreelancerSelectionDialog(hiredFreelancers, "Hired Freelancers")
                        }
                    } else {
                        Toast.makeText(this@ConversationsActivity, "Failed to load freelancers", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConversationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openApplicantsList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val applicationsResult = firebaseService.getApplicationsByClient(currentUserId)

                withContext(Dispatchers.Main) {
                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        val pendingApplicants = applications.filter {
                            it.status.equals("pending", ignoreCase = true)
                        }

                        if (pendingApplicants.isEmpty()) {
                            showNoApplicantsDialog()
                        } else {
                            showFreelancerSelectionDialog(pendingApplicants, "Pending Applicants")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConversationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAppliedClientsList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val applicationsResult = firebaseService.getApplicationsByFreelancer(currentUserId)

                withContext(Dispatchers.Main) {
                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        val uniqueClients = applications.map { it.clientId to it.clientName }.distinctBy { it.first }

                        if (uniqueClients.isEmpty()) {
                            showNoClientsDialog()
                        } else {
                            showClientSelectionDialog(uniqueClients, "Clients You Applied To")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConversationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFreelancerSelectionDialog(applications: List<student.projects.tholagig.models.JobApplication>, title: String) {
        val freelancerList = applications.map { it.freelancerId to it.freelancerName }.distinctBy { it.first }

        val freelancerNames = freelancerList.map { it.second }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(freelancerNames) { dialog, which ->
                val selectedFreelancer = freelancerList[which]
                openNewConversation(selectedFreelancer.first, selectedFreelancer.second)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClientSelectionDialog(clients: List<Pair<String, String>>, title: String) {
        val clientNames = clients.map { it.second }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(clientNames) { dialog, which ->
                val selectedClient = clients[which]
                openNewConversation(selectedClient.first, selectedClient.second)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNoHiredFreelancersDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No Hired Freelancers")
            .setMessage("You haven't hired any freelancers yet. Hire freelancers to start conversations with them.")
            .setPositiveButton("View Applications") { dialog, which ->
                startActivity(Intent(this, ApplicationsManagementActivity::class.java))
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun showNoApplicantsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No Pending Applications")
            .setMessage("You don't have any pending applications. Applicants will appear here when they apply to your jobs.")
            .setPositiveButton("Create Job") { dialog, which ->
                startActivity(Intent(this, student.projects.tholagig.jobs.CreateJobActivity::class.java))
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun showNoClientsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No Clients Found")
            .setMessage("You haven't applied to any jobs yet. Apply to jobs to start conversations with clients.")
            .setPositiveButton("Browse Jobs") { dialog, which ->
                startActivity(Intent(this, JobBrowseActivity::class.java))
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun searchUsers() {
        Toast.makeText(this, "User search feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun searchClients() {
        Toast.makeText(this, "Client search feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun createGroupChat() {
        Toast.makeText(this, "Group chat feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun contactSupport() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@tholagig.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Support Request - TholaGig Messaging")
            putExtra(Intent.EXTRA_TEXT, "Hello TholaGig Support Team,\n\nI need help with messaging features:\n\n")
        }
        try {
            startActivity(Intent.createChooser(intent, "Contact Support"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        conversationsAdapter = ConversationsAdapter(conversationsList) { conversation ->
            openMessages(conversation)
        }
        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = conversationsAdapter
    }

    private fun loadRealConversations() {
        if (currentUserId.isEmpty()) {
            Log.e("Conversations", "❌ No user ID found")
            showEmptyState()
            return
        }

        Log.d("Conversations", "📥 Loading real conversations for user: $currentUserId (Type: $currentUserType)")

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all messages for current user
                val messagesResult = firebaseService.getUserConversations(currentUserId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (messagesResult.isSuccess) {
                        val messages = messagesResult.getOrNull() ?: emptyList()
                        Log.d("Conversations", "✅ Loaded ${messages.size} messages from Firestore")
                        processConversations(messages)
                    } else {
                        Log.e("Conversations", "❌ Failed to load messages")
                        showEmptyState()
                        Toast.makeText(this@ConversationsActivity, "Failed to load conversations", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Conversations", "💥 Error loading conversations: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmptyState()
                    Toast.makeText(this@ConversationsActivity, "Error loading conversations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processConversations(messages: List<Message>) {
        val conversationsMap = mutableMapOf<String, Conversation>()

        Log.d("CONVERSATIONS_DEBUG", "🔄 Processing ${messages.size} messages for user: $currentUserId")

        messages.forEach { message ->
            val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId

            Log.d("CONVERSATIONS_DEBUG", "💬 Message - Sender: ${message.senderId}, Receiver: ${message.receiverId}, IsRead: ${message.isRead}")

            if (!conversationsMap.containsKey(otherUserId)) {
                // 🆕 FIX: Calculate unread count properly
                val unreadCount = if (!message.isRead && message.receiverId == currentUserId) 1 else 0

                conversationsMap[otherUserId] = Conversation(
                    userId = otherUserId,
                    userName = "Loading...",
                    lastMessage = message.content,
                    timestamp = message.timestamp.time,
                    unreadCount = unreadCount,
                    userType = if (message.senderId == currentUserId) "receiver" else "sender"
                )

                Log.d("CONVERSATIONS_DEBUG", "📝 New conversation with $otherUserId - Unread: $unreadCount")
                loadUserName(otherUserId, conversationsMap)

            } else {
                val existing = conversationsMap[otherUserId]!!
                if (message.timestamp.time > existing.timestamp) {
                    // 🆕 FIX: Only count unread messages where current user is receiver
                    val unreadCount = if (!message.isRead && message.receiverId == currentUserId) 1 else 0

                    conversationsMap[otherUserId] = existing.copy(
                        lastMessage = message.content,
                        timestamp = message.timestamp.time,
                        unreadCount = unreadCount
                    )
                    Log.d("CONVERSATIONS_DEBUG", "🔄 Updated conversation with $otherUserId - Unread: $unreadCount")
                } else {
                    // 🆕 FIX: Still update unread count for older unread messages
                    if (!message.isRead && message.receiverId == currentUserId) {
                        val newUnreadCount = existing.unreadCount + 1
                        conversationsMap[otherUserId] = existing.copy(unreadCount = newUnreadCount)
                        Log.d("CONVERSATIONS_DEBUG", "➕ Increased unread count for $otherUserId: $newUnreadCount")
                    }
                }
            }
        }

        // 🆕 REMOVED DUPLICATE CODE - Only update once
        conversationsList.clear()
        conversationsList.addAll(conversationsMap.values.sortedByDescending { it.timestamp })
        conversationsAdapter.notifyDataSetChanged()

        updateEmptyState()

        // 🆕 Debug: Log final unread counts
        conversationsList.forEach { conv ->
            Log.d("CONVERSATIONS_DEBUG", "📊 Final - User: ${conv.userId}, Unread: ${conv.unreadCount}")
        }

        Log.d("Conversations", "✅ Displaying ${conversationsList.size} conversations")
    }

    private fun loadUserName(userId: String, conversationsMap: MutableMap<String, Conversation>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userResult = firebaseService.getUserById(userId)

                withContext(Dispatchers.Main) {
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        val userName = user?.fullName ?: "Unknown User"
                        val userType = user?.userType ?: "user"

                        // Update the conversation with the real name
                        val conversation = conversationsMap[userId]
                        if (conversation != null) {
                            val updatedConversation = conversation.copy(
                                userName = userName,
                                userType = userType
                            )
                            conversationsMap[userId] = updatedConversation

                            // Update the list and refresh
                            val index = conversationsList.indexOfFirst { it.userId == userId }
                            if (index != -1) {
                                conversationsList[index] = updatedConversation
                                conversationsAdapter.notifyItemChanged(index)
                            } else {
                                // If not in list yet, add it (shouldn't happen but safe)
                                conversationsList.add(updatedConversation)
                                conversationsAdapter.notifyDataSetChanged()
                            }
                        }
                    } else {
                        // Fallback to user ID as name
                        val conversation = conversationsMap[userId]
                        if (conversation != null) {
                            val updatedConversation = conversation.copy(userName = "User $userId")
                            conversationsMap[userId] = updatedConversation

                            val index = conversationsList.indexOfFirst { it.userId == userId }
                            if (index != -1) {
                                conversationsList[index] = updatedConversation
                                conversationsAdapter.notifyItemChanged(index)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Conversations", "❌ Error loading user name: ${e.message}")
            }
        }
    }

    private fun updateEmptyState() {
        if (conversationsList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        layoutEmptyState.visibility = View.VISIBLE
        rvConversations.visibility = View.GONE

        val emptyTitle = if (isClient) {
            "No messages yet"
        } else {
            "No messages yet"
        }

        val emptyMessage = if (isClient) {
            "Start conversations with freelancers who apply to your jobs or that you've hired."
        } else {
            "Start conversations with clients you've applied to or been hired by."
        }

        tvEmptyTitle.text = emptyTitle
        tvEmptyMessage.text = emptyMessage
    }

    private fun hideEmptyState() {
        layoutEmptyState.visibility = View.GONE
        rvConversations.visibility = View.VISIBLE
    }

    private fun openNewConversation(receiverId: String, receiverName: String) {
        Log.d("Conversations", "💬 Starting new conversation with: $receiverName")

        val intent = Intent(this, MessagesActivity::class.java).apply {
            putExtra("RECEIVER_ID", receiverId)
            putExtra("RECEIVER_NAME", receiverName)
        }
        startActivity(intent)
    }

    private fun openMessages(conversation: Conversation) {
        Log.d("Conversations", "💬 Opening conversation with: ${conversation.userName}")

        // Mark messages as read when opening conversation
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.markConversationAsRead(conversation.userId, currentUserId)
        }

        val intent = Intent(this, MessagesActivity::class.java).apply {
            putExtra("RECEIVER_ID", conversation.userId)
            putExtra("RECEIVER_NAME", conversation.userName)
        }
        startActivity(intent)
    }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // 🆕 Simple message listener for notifications (you can expand this later)
    private fun onNewMessageReceived(senderName: String, message: String) {
        // Only show notification if app is in background
        if (!isActivityVisible) {
            notificationHelper.showMessageNotification(senderName, message)
        }
    }

    private fun setupRealTimeMessageListener() {
        Log.d("NOTIFICATIONS", "🔔 Setting up real-time message listener...")

        // Listen for new messages in Firestore
        firebaseService.listenForNewMessages(currentUserId) { newMessage ->
            Log.d("NOTIFICATIONS", "📨 New message received from ${newMessage.senderId}")

            // Show notification if app is in background
            if (!isActivityVisible) {
                Log.d("NOTIFICATIONS", "📱 App is in background, showing notification")

                // Get sender name
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val senderResult = firebaseService.getUserById(newMessage.senderId)

                        withContext(Dispatchers.Main) {
                            val senderName = if (senderResult.isSuccess) {
                                senderResult.getOrNull()?.fullName ?: "Unknown User"
                            } else {
                                "Unknown User"
                            }

                            // Show the notification
                            notificationHelper.showMessageNotification(senderName, newMessage.content)
                            Log.d("NOTIFICATIONS", "✅ Notification sent: $senderName - ${newMessage.content}")
                        }
                    } catch (e: Exception) {
                        Log.e("NOTIFICATIONS", "❌ Error getting sender name: ${e.message}")

                        // Show notification with default name
                        withContext(Dispatchers.Main) {
                            notificationHelper.showMessageNotification("New Message", newMessage.content)
                        }
                    }
                }
            } else {
                Log.d("NOTIFICATIONS", "📱 App is in foreground, refreshing conversations")
                // Refresh the conversations list
                loadRealConversations()
            }
        }
    }
    // ADD THIS METHOD - Prevents duplicate notifications
    private fun showUniqueNotification(senderName: String, message: String, messageId: String, senderId: String) {
        try {
            val notificationId = messageId.hashCode()

            // 🆕 FIX: Open MessagesActivity directly with the conversation
            val intent = Intent(this, MessagesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("RECEIVER_ID", senderId)  // The person who sent the message
                putExtra("RECEIVER_NAME", senderName)
                putExtra("FROM_NOTIFICATION", true) // Mark that we came from notification
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(this, "MESSAGES_CHANNEL")
                .setContentTitle("💬 New message from $senderName")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(1000, 1000))
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
            Log.d("NOTIFICATIONS", "✅ Notification shown for $senderName")

        } catch (e: Exception) {
            Log.e("NOTIFICATIONS", "❌ Error showing notification: ${e.message}")
        }
    }

    // 🆕 Helper method to get sender ID (you'll need to implement based on your data structure)
    private fun getSenderIdFromMessage(messageId: String): String {
        // This depends on your data structure - you might need to query Firestore
        // or store senderId in the notification intent
        return "unknown" // Implement this based on your app
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        Log.d("Conversations", "🔄 onResume: Refreshing conversations")
        loadRealConversations()
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}