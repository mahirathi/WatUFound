package com.ece452.watufound.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ece452.watufound.R
import com.ece452.watufound.models.LostItemModel
import com.ece452.watufound.models.FoundItemModel
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

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagStatus: TextView = itemView.findViewById(R.id.tagStatus)
        private val tagCategory: TextView = itemView.findViewById(R.id.tagCategory)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val textLocation: TextView = itemView.findViewById(R.id.textLocation)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val buttonArrow: ImageButton = itemView.findViewById(R.id.buttonArrow)

        fun bind(item: FeedItem) {
            // Create navigation bundle based on item type
            val bundle = when (item) {
                is FeedItem.Lost -> Bundle().apply {
                    putString("postType", "lost")
                    putString("itemTitle", item.item.itemTitle)
                    putString("description", item.item.description)
                    putString("category", item.item.category)
                    putString("location", item.item.lastSeenAt)
                    putString("dateTime", item.item.dateTimeLost)
                }
                is FeedItem.Found -> Bundle().apply {
                    putString("postType", "found")
                    putString("itemTitle", item.item.itemTitle)
                    putString("description", item.item.description)
                    putString("category", item.item.category)
                    putString("location", item.item.foundAt)
                    putString("dateTime", item.item.dateTimeFound)
                }
            }
            
            // Set up click listeners for both card and arrow
            val cardClickListener = View.OnClickListener {
                itemView.findNavController().navigate(R.id.navigation_post_details, bundle)
            }
            
            // Make entire card clickable
            itemView.setOnClickListener(cardClickListener)
            
            // Arrow button also navigates
            buttonArrow.setOnClickListener(cardClickListener)
            
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
                    textTime.text = TimeUtils.getRelativeTimeAgo(item.item.dateTimeLost)
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
                    textTime.text = TimeUtils.getRelativeTimeAgo(item.item.dateTimeFound)
                }
            }
        }
    }
} 