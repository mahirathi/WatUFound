package com.ece452.watufound.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ece452.watufound.R
import com.ece452.watufound.models.User

class ConversationsAdapter(
    private val onUserClick: (User) -> Unit,
    private val onDeleteClick: ((User) -> Unit)? = null,
    private val showDeleteButton: Boolean = false
) : ListAdapter<User, ConversationsAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view, onUserClick, onDeleteClick, showDeleteButton)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        itemView: View,
        private val onUserClick: (User) -> Unit,
        private val onDeleteClick: ((User) -> Unit)?,
        private val showDeleteButton: Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val userName: TextView = itemView.findViewById(R.id.textUserName)
        private val userEmail: TextView = itemView.findViewById(R.id.textUserEmail)
        private val buttonDelete: android.widget.ImageButton = itemView.findViewById(R.id.buttonDelete)

        fun bind(user: User) {
            userName.text = user.displayName
            userEmail.text = user.email
            
            // Show/hide delete button based on adapter settings
            if (showDeleteButton && onDeleteClick != null) {
                buttonDelete.visibility = android.view.View.VISIBLE
                buttonDelete.setOnClickListener {
                    onDeleteClick?.let { it1 -> it1(user) }
                }
            } else {
                buttonDelete.visibility = android.view.View.GONE
            }
            
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
} 