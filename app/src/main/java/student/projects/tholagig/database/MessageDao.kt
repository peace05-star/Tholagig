package student.projects.tholagig.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import student.projects.tholagig.models.OfflineMessage

@Dao
interface MessageDao {
    @Query("SELECT * FROM offline_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<OfflineMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: OfflineMessage)

    @Query("SELECT * FROM offline_messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<OfflineMessage>

    @Query("UPDATE offline_messages SET isSynced = 1 WHERE messageId = :messageId")
    suspend fun markMessageAsSynced(messageId: String)

    @Query("DELETE FROM offline_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)
}