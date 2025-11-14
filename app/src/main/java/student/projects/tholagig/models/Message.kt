package student.projects.tholagig.models

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Date = Date(),

    // Map Firestore's "read" field to Kotlin's "isRead"
    @PropertyName("read")
    val isRead: Boolean = false
) {
    // empty constructor for Firestore
    constructor() : this("", "", "", "", "", Date(), false)
}