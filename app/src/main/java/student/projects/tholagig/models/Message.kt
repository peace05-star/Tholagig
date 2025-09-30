package student.projects.tholagig.models

import java.util.Date

data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false
)