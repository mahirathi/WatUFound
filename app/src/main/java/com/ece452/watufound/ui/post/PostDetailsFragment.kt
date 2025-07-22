package com.ece452.watufound.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import com.ece452.watufound.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth

class PostDetailsFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val tagStatus: TextView = view.findViewById(R.id.tagStatus)
        val tagCategory: TextView = view.findViewById(R.id.tagCategory)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textDescription: TextView = view.findViewById(R.id.textDescription)
        val textLocation: TextView = view.findViewById(R.id.textLocation)
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textPostedBy: TextView = view.findViewById(R.id.textPostedBy)
        val buttonContact: Button = view.findViewById(R.id.buttonContact)
        
        // Set up toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Get arguments from bundle
        val postType = arguments?.getString("postType")
        val itemTitle = arguments?.getString("itemTitle", "")
        val description = arguments?.getString("description", "")
        val category = arguments?.getString("category", "")
        val location = arguments?.getString("location", "")
        val dateTime = arguments?.getString("dateTime", "")
        val userId = arguments?.getString("userId", "")
        val userName = arguments?.getString("userName", "")
        val isAnonymous = arguments?.getBoolean("isAnonymous")
        
        // Debug logging
        android.util.Log.d("PostDetailsFragment", "Received data - userId: '$userId', userName: '$userName'")
        android.util.Log.d("PostDetailsFragment", "Arguments: $arguments")
        
        // Set Posted By
        textPostedBy.text = if (isAnonymous == true) "Anonymous" else userName
        
        // Check if this is the current user's post
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser?.uid == userId) {
            android.util.Log.d("PostDetailsFragment", "This is the current user's post")
            buttonContact.isEnabled = false
            buttonContact.text = "Your Post"
            buttonContact.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
        } else {
            android.util.Log.d("PostDetailsFragment", "This is another user's post")
            // Set contact button text based on anonymity
            buttonContact.text = if (isAnonymous == true) "Contact Anonymous" else "Contact $userName"
        }
        
        // Populate data based on post type
        when (postType) {
            "lost" -> {
                tagStatus.text = "Lost"
                tagStatus.setBackgroundResource(R.drawable.tag_lost_bg)
                tagStatus.setTextColor(requireContext().getColor(R.color.lost_text))
                toolbar.title = "Lost Item Details"
            }
            "found" -> {
                tagStatus.text = "Found"
                tagStatus.setBackgroundResource(R.drawable.tag_found_bg)
                tagStatus.setTextColor(requireContext().getColor(R.color.found_text))
                toolbar.title = "Found Item Details"
            }
        }
        
        // Set the data
        tagCategory.text = category
        tagCategory.setBackgroundResource(R.drawable.tag_category_bg)
        tagCategory.setTextColor(requireContext().getColor(R.color.category_text))
        textTitle.text = itemTitle
        textDescription.text = description
        textLocation.text = location
        textDateTime.text = TimeUtils.getRelativeTimeAgo(dateTime ?: "")
        
        // Set up contact button
        buttonContact.setOnClickListener {
            if (!userId.isNullOrEmpty() && !userName.isNullOrEmpty()) {
                // Check if the current user is the post owner
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser?.uid == userId) {
                    // This is the user's own post - disable contact
                    buttonContact.isEnabled = false
                    buttonContact.text = "Your Post"
                } else {
                    // Navigate to conversation with the post owner
                    val bundle = Bundle().apply {
                        putString("otherUserId", userId)
                        putString("otherUserName", userName)
                    }
                    findNavController().navigate(R.id.navigation_conversation, bundle)
                }
            } else {
                // Handle case where user information is not available
                buttonContact.isEnabled = false
                buttonContact.text = "Contact Unavailable"
            }
        }
    }
} 