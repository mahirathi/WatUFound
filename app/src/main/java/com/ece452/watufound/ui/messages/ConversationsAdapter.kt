package com.ece452.watufound.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ece452.watufound.R

// New data class for conversation list items
data class ConversationListItem(
    val conversationId: String,
    val posterName: String,
    val postTitle: String
)

class ConversationsAdapter(
    private val onConversationClick: (ConversationListItem) -> Unit,
    private val onDeleteClick: ((ConversationListItem) -> Unit)? = null,
    private val showDeleteButton: Boolean = false
) : ListAdapter<ConversationListItem, ConversationsAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_user, parent, false)
        return ConversationViewHolder(view, onConversationClick, onDeleteClick, showDeleteButton)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ConversationViewHolder(
        itemView: View,
        private val onConversationClick: (ConversationListItem) -> Unit,
        private val onDeleteClick: ((ConversationListItem) -> Unit)?,
        private val showDeleteButton: Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.textUserName)
        private val postTitle: TextView = itemView.findViewById(R.id.textPostTitle)
        private val buttonDelete: android.widget.ImageButton = itemView.findViewById(R.id.buttonDelete)

        fun bind(item: ConversationListItem) {
            userName.text = item.posterName
            postTitle.text = item.postTitle

            if (showDeleteButton && onDeleteClick != null) {
                buttonDelete.visibility = android.view.View.VISIBLE
                buttonDelete.setOnClickListener {
                    onDeleteClick?.let { it1 -> it1(item) }
                }
            } else {
                buttonDelete.visibility = android.view.View.GONE
            }

            itemView.setOnClickListener {
                onConversationClick(item)
            }
        }
    }

    private class ConversationDiffCallback : DiffUtil.ItemCallback<ConversationListItem>() {
        override fun areItemsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean {
            return oldItem.conversationId == newItem.conversationId
        }

        override fun areContentsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean {
            return oldItem == newItem
        }
    }
} 