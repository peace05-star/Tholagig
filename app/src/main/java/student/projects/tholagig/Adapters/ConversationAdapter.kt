package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.models.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)

        fun bind(conversation: Conversation) {
            tvUserName.text = conversation.userName
            tvLastMessage.text = conversation.lastMessage
            tvTimestamp.text = formatTimestamp(conversation.timestamp)

            if (conversation.unreadCount > 0) {
                tvUnreadCount.text = conversation.unreadCount.toString()
                tvUnreadCount.visibility = View.VISIBLE
            } else {
                tvUnreadCount.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val now = Date()
            val diff = now.time - timestamp

            return when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
                diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }
}