package com.ece452.watufound.ui.messages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentConversationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var conversationsAdapter: ConversationsAdapter
    private var conversationsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("ConversationsFragment", "=== CONVERSATIONS FRAGMENT CREATED ===")
        android.util.Log.d("ConversationsFragment", "Fragment arguments: $arguments")

        conversationsAdapter = ConversationsAdapter(
            onConversationClick = { conversation ->
                val bundle = Bundle().apply {
                    putString("conversationId", conversation.conversationId)
                    putString("otherUserName", conversation.posterName)
                    putString("postTitle", conversation.postTitle)
                    putString("postImageUrl", conversation.postImageUrl ?: "")
                    putString("postId", conversation.postId ?: "")
                }
                android.util.Log.d("ConversationsFragment", "Navigating to conversation with bundle:")
                android.util.Log.d("ConversationsFragment", "  - conversationId: ${conversation.conversationId}")
                android.util.Log.d("ConversationsFragment", "  - otherUserName: ${conversation.posterName}")
                android.util.Log.d("ConversationsFragment", "  - postTitle: ${conversation.postTitle}")
                android.util.Log.d("ConversationsFragment", "  - postImageUrl: ${conversation.postImageUrl}")
                android.util.Log.d("ConversationsFragment", "  - postId: ${conversation.postId}")
                findNavController().navigate(R.id.navigation_conversation, bundle)
            },
            showDeleteButton = false
        )
        binding.recyclerViewRecent.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRecent.adapter = conversationsAdapter

        android.util.Log.d("ConversationsFragment", "Setting up conversation list view")
        
        // Fetch conversations from database
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            android.util.Log.d("ConversationsFragment", "User authenticated, fetching conversations")
            val db = FirebaseDatabase.getInstance().reference
            conversationsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Prevent crash if view is destroyed
                    val conversations = mutableListOf<ConversationListItem>()
                    val pendingStatusFetches = mutableListOf<() -> Unit>()
                    
                    for (child in snapshot.children) {
                        val conversationId = child.child("conversationId").getValue(String::class.java) ?: continue
                        val posterName = child.child("posterName").getValue(String::class.java) ?: ""
                        val postTitle = child.child("postTitle").getValue(String::class.java) ?: ""
                        val postImageUrl = child.child("postImageUrl").getValue(String::class.java)
                        val postId = child.child("postId").getValue(String::class.java) ?: ""
                        
                        // Debug logging for postId
                        android.util.Log.d("ConversationsFragment", "=== CONVERSATION DATA ===")
                        android.util.Log.d("ConversationsFragment", "Conversation: '$postTitle'")
                        android.util.Log.d("ConversationsFragment", "PostId: '$postId'")
                        android.util.Log.d("ConversationsFragment", "PosterName: '$posterName'")
                        android.util.Log.d("ConversationsFragment", "ConversationId: '$conversationId'")
                        android.util.Log.d("ConversationsFragment", "========================")
                        
                        // Debug logging for image URL
                        android.util.Log.d("ConversationsFragment", "Conversation: $postTitle, Image URL: '$postImageUrl'")
                        
                        // If no image URL in conversation data, try to fetch from Firestore
                        val finalImageUrl = if (postImageUrl.isNullOrBlank()) {
                            // This is a fallback for existing conversations that don't have image URLs
                            fetchPostImageUrlFromFirestore(postTitle) { imageUrl ->
                                // Update the conversation item with the fetched image URL
                                val updatedConversations = conversations.toMutableList()
                                val index = updatedConversations.indexOfFirst { it.postTitle == postTitle }
                                if (index != -1) {
                                    updatedConversations[index] = updatedConversations[index].copy(postImageUrl = imageUrl)
                                    conversationsAdapter.submitList(updatedConversations)
                                }
                            }
                            null // Return null initially, will be updated via callback
                        } else {
                            postImageUrl
                        }
                        
                        // Get creation timestamp as fallback
                        val createdAt = child.child("createdAt").getValue(Long::class.java)
                        val fallbackTimestamp = if (createdAt != null) {
                            getTimeAgo(createdAt)
                        } else {
                            ""
                        }
                        
                        // Create conversation item with initial status (will be updated later)
                        val conversationItem = ConversationListItem(
                            conversationId = conversationId,
                            posterName = posterName,
                            postTitle = postTitle,
                            postImageUrl = finalImageUrl,
                            timestamp = fallbackTimestamp,
                            postId = postId,
                            isResolved = false,
                            isDeleted = false
                        )
                        
                        conversations.add(conversationItem)
                        
                        // Add status fetch to pending list
                        if (!postId.isNullOrBlank()) {
                            pendingStatusFetches.add {
                                android.util.Log.d("ConversationsFragment", "Fetching status for postId: $postId, postTitle: $postTitle")
                                fetchPostStatus(postId) { isResolved, isDeleted ->
                                    android.util.Log.d("ConversationsFragment", "Status fetched for postId: $postId - Resolved: $isResolved, Deleted: $isDeleted")
                                    // Update the conversation item with status
                                    val updatedConversations = conversations.toMutableList()
                                    val index = updatedConversations.indexOfFirst { it.postId == postId }
                                    if (index != -1) {
                                        updatedConversations[index] = updatedConversations[index].copy(
                                            isResolved = isResolved,
                                            isDeleted = isDeleted
                                        )
                                        android.util.Log.d("ConversationsFragment", "Updated conversation item with status - Resolved: $isResolved, Deleted: $isDeleted")
                                        conversationsAdapter.submitList(updatedConversations)
                                    } else {
                                        android.util.Log.e("ConversationsFragment", "Could not find conversation with postId: $postId")
                                    }
                                }
                            }
                        } else {
                            android.util.Log.d("ConversationsFragment", "No postId available for conversation: $postTitle")
                        }
                    }
                    
                    // Submit initial list to adapter
                    conversations.sortByDescending { it.timestamp ?: "" }
                    conversationsAdapter.submitList(conversations)
                    
                    // Execute all pending status fetches
                    pendingStatusFetches.forEach { it() }
                    
                    binding.textNoResults.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            db.child("userConversations").child(currentUser.uid)
                .addValueEventListener(conversationsListener!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && conversationsListener != null) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("userConversations").child(currentUser.uid).removeEventListener(conversationsListener!!)
        }
        _binding = null
    }
    
    private fun getConversationTimestamp(conversationId: String, onTimestampFetched: (String) -> Unit) {
        // Fetch the latest message timestamp from the conversation
        val db = FirebaseDatabase.getInstance().reference
        db.child("conversations").child(conversationId).orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var latestTimestamp = 0L
                        for (messageSnapshot in snapshot.children) {
                            val timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            if (timestamp > latestTimestamp) {
                                latestTimestamp = timestamp
                            }
                        }
                        
                        if (latestTimestamp > 0) {
                            val timeAgo = getTimeAgo(latestTimestamp)
                            onTimestampFetched(timeAgo)
                        } else {
                            onTimestampFetched("")
                        }
                    } else {
                        onTimestampFetched("")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    onTimestampFetched("")
                }
            })
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timestamp
        
        val seconds = timeDifference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
    
    private fun fetchPostImageUrlFromFirestore(postTitle: String, onImageUrlFetched: (String?) -> Unit) {
        android.util.Log.d("ConversationsFragment", "Attempting to fetch image URL for post: $postTitle")
        
        // Search in both lost_items and found_items collections
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        // First try lost_items
        firestore.collection("lost_items")
            .whereEqualTo("itemTitle", postTitle)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val imageUrl = documents.documents[0].getString("imageUrl")
                    android.util.Log.d("ConversationsFragment", "Found image URL in lost_items: $imageUrl")
                    onImageUrlFetched(imageUrl)
                } else {
                    // If not found in lost_items, try found_items
                    firestore.collection("found_items")
                        .whereEqualTo("itemTitle", postTitle)
                        .get()
                        .addOnSuccessListener { foundDocuments ->
                            if (!foundDocuments.isEmpty) {
                                val imageUrl = foundDocuments.documents[0].getString("imageUrl")
                                android.util.Log.d("ConversationsFragment", "Found image URL in found_items: $imageUrl")
                                onImageUrlFetched(imageUrl)
                            } else {
                                android.util.Log.d("ConversationsFragment", "No image URL found for post: $postTitle")
                                onImageUrlFetched(null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("ConversationsFragment", "Error fetching from found_items", exception)
                            onImageUrlFetched(null)
                        }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("ConversationsFragment", "Error fetching from lost_items", exception)
                onImageUrlFetched(null)
            }
    }
    
    private fun fetchPostStatus(postId: String, onStatusFetched: (Boolean, Boolean) -> Unit) {
        android.util.Log.d("ConversationsFragment", "=== FETCHING POST STATUS ===")
        android.util.Log.d("ConversationsFragment", "postId: '$postId'")
        
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        // Try to find the post in both collections
        android.util.Log.d("ConversationsFragment", "Checking lost_items collection...")
        firestore.collection("lost_items").document(postId).get()
            .addOnSuccessListener { document ->
                android.util.Log.d("ConversationsFragment", "Lost items query completed")
                android.util.Log.d("ConversationsFragment", "Document exists: ${document.exists()}")
                if (document.exists()) {
                    val isResolved = document.getBoolean("isResolved") ?: false
                    val isDeleted = document.getBoolean("isDeleted") ?: false
                    android.util.Log.d("ConversationsFragment", "Found post in lost_items - Resolved: $isResolved, Deleted: $isDeleted")
                    onStatusFetched(isResolved, isDeleted)
                } else {
                    // Try found_items
                    android.util.Log.d("ConversationsFragment", "Post not found in lost_items, checking found_items...")
                    firestore.collection("found_items").document(postId).get()
                        .addOnSuccessListener { foundDocument ->
                            android.util.Log.d("ConversationsFragment", "Found items query completed")
                            android.util.Log.d("ConversationsFragment", "Document exists: ${foundDocument.exists()}")
                            if (foundDocument.exists()) {
                                val isResolved = foundDocument.getBoolean("isResolved") ?: false
                                val isDeleted = foundDocument.getBoolean("isDeleted") ?: false
                                android.util.Log.d("ConversationsFragment", "Found post in found_items - Resolved: $isResolved, Deleted: $isDeleted")
                                onStatusFetched(isResolved, isDeleted)
                            } else {
                                android.util.Log.d("ConversationsFragment", "Post not found in either collection")
                                onStatusFetched(false, false)
                            }
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("ConversationsFragment", "Error fetching from found_items", exception)
                            onStatusFetched(false, false)
                        }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("ConversationsFragment", "Error fetching from lost_items", exception)
                onStatusFetched(false, false)
            }
    }
} 