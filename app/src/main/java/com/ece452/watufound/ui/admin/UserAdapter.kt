package com.ece452.watufound.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ece452.watufound.R
import com.ece452.watufound.models.User
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter(
    private val users: MutableList<User> = mutableListOf(),
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun filterUsers(query: String) {
        // This will be handled by the ViewModel
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
        private val userName: TextView = itemView.findViewById(R.id.textUserName)
        private val userEmail: TextView = itemView.findViewById(R.id.userEmail)
        private val userStatus: TextView = itemView.findViewById(R.id.userStatus)
        private val yourProfileLabel: TextView = itemView.findViewById(R.id.yourProfileLabel)
        private val banStatusIcon: ImageView = itemView.findViewById(R.id.banStatusIcon)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(users[position])
                }
            }
        }

        fun bind(user: User) {
            userName.text = user.displayName.ifEmpty { "Unknown User" }
            userEmail.text = user.email.ifEmpty { "No email" }
            
            // Check if this is the current user's profile
            val currentUser = FirebaseAuth.getInstance().currentUser
            val isOwnProfile = currentUser?.uid == user.uid
            
            // Show "Your Profile" label if it's the current user
            yourProfileLabel.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
            
            // Set background for "Your Profile" label
            if (isOwnProfile) {
                yourProfileLabel.background = itemView.context.getDrawable(R.drawable.status_your_profile_bg)
            }
            
            // Set user status with colored symbols
            if (user.isBanned) {
                userStatus.text = "Banned"
                userStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                userStatus.background = itemView.context.getDrawable(R.drawable.status_banned_bg)
                banStatusIcon.visibility = View.VISIBLE
            } else {
                userStatus.text = "Active"
                userStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                userStatus.background = itemView.context.getDrawable(R.drawable.status_active_bg)
                banStatusIcon.visibility = View.GONE
            }
        }
    }
} 