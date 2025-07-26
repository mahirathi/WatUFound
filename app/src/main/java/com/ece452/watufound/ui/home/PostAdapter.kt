package com.ece452.watufound.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ece452.watufound.R
import com.ece452.watufound.utils.TimeUtils

class PostAdapter(private var posts: List<FeedItem>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_card, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<FeedItem>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    class PostViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val imagePost: ImageView = itemView.findViewById(R.id.imagePost)
        private val tagStatus: TextView = itemView.findViewById(R.id.tagStatus)
        private val tagCategory: TextView = itemView.findViewById(R.id.tagCategory)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val textLocation: TextView = itemView.findViewById(R.id.textBuilding)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val buttonArrow: ImageButton = itemView.findViewById(R.id.buttonArrow)

        fun bind(item: FeedItem) {
            // Create navigation bundle based on item type
            val bundle = when (item) {
                is FeedItem.Lost -> Bundle().apply {
                    putString("postType", "lost")
                    putString("postId", item.item.documentId)
                    putString("itemTitle", item.item.itemTitle)
                    putString("description", item.item.description)
                    putString("category", item.item.category)
                    putString("location", item.item.lastSeenAt)
                    putString("dateTime", item.item.dateTimeLost)
                    putString("dateTimeCreated", item.item.dateCreated)
                    putString("userId", item.item.userId)
                    putString("userName", item.item.userName)
                    putBoolean("isAnonymous", item.item.isAnonymous)
                    putString("imageUrl", item.item.imageUrl)
                    putString("roomNumber", item.item.roomNumber)
                }
                is FeedItem.Found -> Bundle().apply {
                    putString("postType", "found")
                    putString("postId", item.item.documentId)
                    putString("itemTitle", item.item.itemTitle)
                    putString("description", item.item.description)
                    putString("category", item.item.category)
                    putString("location", item.item.foundAt)
                    putString("dateTime", item.item.dateTimeFound)
                    putString("dateTimeCreated", item.item.dateCreated)
                    putString("userId", item.item.userId)
                    putString("userName", item.item.userName)
                    putBoolean("isAnonymous", item.item.isAnonymous)
                    putString("imageUrl", item.item.imageUrl)
                    putString("roomNumber", item.item.roomNumber)
                }
            }

            // Set up click listeners for both card and arrow
            val cardClickListener = View.OnClickListener {
//                android.util.Log.d("PostAdapter", "Navigating with userName=${item.item.userName}, isAnonymous=${item.item.isAnonymous}")
                itemView.findNavController().navigate(R.id.navigation_post_details, bundle)
            }

            // Make entire card clickable
            itemView.setOnClickListener(cardClickListener)

            // Arrow button also navigates
            buttonArrow.setOnClickListener(cardClickListener)

            // Set up resolve and delete button click listeners
            // These buttons are now handled in PostDetailsFragment, so no listeners are set here.

            val isResolved = when (item) {
                is FeedItem.Lost -> item.item.isResolved
                is FeedItem.Found -> item.item.isResolved
            }
            if (isResolved) {
                // buttonResolve.visibility = View.GONE
                // buttonDelete.visibility = View.GONE
            } else {
                // buttonResolve.visibility = View.VISIBLE
                // buttonDelete.visibility = View.VISIBLE
            }

            // Load image if available
            val imageUrl = when (item) {
                is FeedItem.Lost -> item.item.imageUrl
                is FeedItem.Found -> item.item.imageUrl
            }
            if (!imageUrl.isNullOrBlank()) {
                imagePost.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(imagePost)
            } else {
                imagePost.visibility = View.GONE
            }

            when (item) {
                is FeedItem.Lost -> {
                    tagStatus.text = "Lost"
                    tagStatus.setBackgroundResource(R.drawable.tag_lost_bg)
                    tagStatus.setTextColor(itemView.context.getColor(R.color.lost_text))
                    tagCategory.text = item.item.category
                    tagCategory.setBackgroundResource(R.drawable.tag_category_bg)
                    tagCategory.setTextColor(itemView.context.getColor(R.color.category_text))
                    textTitle.text = item.item.itemTitle
                    textDescription.text = item.item.description
                    textLocation.text = item.item.lastSeenAt
                    textTime.text = TimeUtils.getRelativeTimeAgo(item.item.dateCreated)
                }
                is FeedItem.Found -> {
                    tagStatus.text = "Found"
                    tagStatus.setBackgroundResource(R.drawable.tag_found_bg)
                    tagStatus.setTextColor(itemView.context.getColor(R.color.found_text))
                    tagCategory.text = item.item.category
                    tagCategory.setBackgroundResource(R.drawable.tag_category_bg)
                    tagCategory.setTextColor(itemView.context.getColor(R.color.category_text))
                    textTitle.text = item.item.itemTitle
                    textDescription.text = item.item.description
                    textLocation.text = item.item.foundAt
                    textTime.text = TimeUtils.getRelativeTimeAgo(item.item.dateCreated)
                }
            }
        }
    }
}