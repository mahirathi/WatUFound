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

        // Fetch conversations from database
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseDatabase.getInstance().reference
            conversationsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Prevent crash if view is destroyed
                    val conversations = mutableListOf<ConversationListItem>()
                    
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
                        
                        // Get timestamp from the conversation messages
                        getConversationTimestamp(conversationId) { realTimestamp ->
                            // Update the conversation item with the real timestamp
                            val updatedConversations = conversations.toMutableList()
                            val index = updatedConversations.indexOfFirst { it.conversationId == conversationId }
                            if (index != -1) {
                                updatedConversations[index] = updatedConversations[index].copy(timestamp = realTimestamp)
                                // Sort by timestamp (most recent first)
                                updatedConversations.sortByDescending { it.timestamp }
                                conversationsAdapter.submitList(updatedConversations)
                            }
                        }
                        
                        // Get creation timestamp as fallback
                        val createdAt = child.child("createdAt").getValue(Long::class.java)
                        val fallbackTimestamp = if (createdAt != null) {
                            getTimeAgo(createdAt)
                        } else {
                            ""
                        }
                        
                        conversations.add(ConversationListItem(
                            conversationId = conversationId,
                            posterName = posterName,
                            postTitle = postTitle,
                            postImageUrl = finalImageUrl,
                            timestamp = fallbackTimestamp, // Use creation timestamp as fallback
                            postId = postId
                        ))
                    }
                    
                    // Sort conversations by timestamp (most recent first)
                    conversations.sortByDescending { it.timestamp }
                    conversationsAdapter.submitList(conversations)
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
} 