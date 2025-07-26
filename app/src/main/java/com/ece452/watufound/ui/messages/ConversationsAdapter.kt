package com.ece452.watufound.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ece452.watufound.R

// Updated data class for conversation list items
data class ConversationListItem(
    val conversationId: String,
    val posterName: String,
    val postTitle: String,
    val postImageUrl: String? = null,
    val timestamp: String? = null,
    val postId: String? = null
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
        private val postImage: ImageView = itemView.findViewById(R.id.imagePost)
        private val timestamp: TextView = itemView.findViewById(R.id.textTimestamp)
        private val buttonDelete: android.widget.ImageButton = itemView.findViewById(R.id.buttonDelete)

        fun bind(item: ConversationListItem) {
            userName.text = item.posterName
            postTitle.text = item.postTitle

            // Show timestamp only if it's not empty
            if (!item.timestamp.isNullOrBlank()) {
                timestamp.visibility = View.VISIBLE
                timestamp.text = item.timestamp
            } else {
                timestamp.visibility = View.GONE
            }

            // Debug logging for image loading
            android.util.Log.d("ConversationsAdapter", "Loading image for post: ${item.postTitle}, URL: '${item.postImageUrl}'")

            // Load post image if available
            if (!item.postImageUrl.isNullOrBlank()) {
                postImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(item.postImageUrl)
                    .placeholder(R.drawable.rounded_image_background)
                    .error(R.drawable.rounded_image_background)
                    .centerCrop()
                    .into(postImage)
            } else {
                postImage.visibility = View.VISIBLE
                postImage.setImageResource(R.drawable.rounded_image_background)
            }

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