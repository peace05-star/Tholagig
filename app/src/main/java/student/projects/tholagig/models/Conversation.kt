package student.projects.tholagig.models

data class Conversation(
    val userId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val userType: String = "client" // or "freelancer"
)