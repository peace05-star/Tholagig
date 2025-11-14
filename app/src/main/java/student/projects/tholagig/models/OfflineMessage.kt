package student.projects.tholagig.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "offline_messages",
    indices = [Index(value = ["conversationId"]), Index(value = ["senderId"])]
)
data class OfflineMessage(
    @PrimaryKey
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isSynced: Boolean = false
)
// No TypeConverter needed here since you're using Long for timestamp