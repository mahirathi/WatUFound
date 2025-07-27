package com.ece452.watufound.ui.messages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentConversationBinding
import com.google.firebase.firestore.FirebaseFirestore

class ConversationFragment : Fragment() {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding!!
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var messagesAdapter: MessagesAdapter

    companion object {
        private const val ARG_OTHER_USER_ID = "otherUserId"
        private const val ARG_OTHER_USER_NAME = "otherUserName"

        fun newInstance(otherUserId: String, otherUserName: String): ConversationFragment {
            Log.d("ConversationFragment", "Creating new instance with user: $otherUserName ($otherUserId)")
            val fragment = ConversationFragment()
            val args = Bundle()
            args.putString(ARG_OTHER_USER_ID, otherUserId)
            args.putString(ARG_OTHER_USER_NAME, otherUserName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ConversationFragment", "onCreateView called")
        try {
            conversationViewModel = ViewModelProvider(this)[ConversationViewModel::class.java]
            _binding = FragmentConversationBinding.inflate(inflater, container, false)
            Log.d("ConversationFragment", "Binding successful")
            
            return binding.root
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error in onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ConversationFragment", "onViewCreated called")
        
        try {
            val conversationId = arguments?.getString("conversationId")
            val otherUserName = arguments?.getString("otherUserName") ?: "Unknown"
            val postTitle = arguments?.getString("postTitle") ?: ""
            val postImageUrl = arguments?.getString("postImageUrl") ?: ""
            val postId = arguments?.getString("postId") ?: ""

            Log.d("ConversationFragment", "=== RECEIVED ARGUMENTS ===")
            Log.d("ConversationFragment", "conversationId: '$conversationId'")
            Log.d("ConversationFragment", "otherUserName: '$otherUserName'")
            Log.d("ConversationFragment", "postTitle: '$postTitle'")
            Log.d("ConversationFragment", "postImageUrl: '$postImageUrl'")
            Log.d("ConversationFragment", "postId: '$postId'")
            Log.d("ConversationFragment", "==========================")

            // Set the conversation title and subtitle
            binding.textConversationTitle.text = if (otherUserName == "Anonymous") "Chat with Anonymous" else "Chat with $otherUserName"
            
            // Setup post preview
            setupPostPreview(postTitle, postImageUrl, postId)

            // Hide action bar and bottom navigation
            hideSystemUI()
            setupBackButton()
            setupRecyclerView()
            setupSendButton()
            observeMessages()

            // Initialize the conversation
            if (conversationId != null) {
                conversationViewModel.initializeAnonymousConversation(conversationId)
            } else {
                // fallback for legacy navigation
                val otherUserId = arguments?.getString("otherUserId") ?: ""
                conversationViewModel.initializeConversation(otherUserId)
            }

            Log.d("ConversationFragment", "Setup completed successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error in onViewCreated", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        Log.d("ConversationFragment", "Setting up RecyclerView")
        try {
            messagesAdapter = MessagesAdapter()
            binding.recyclerViewMessages.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                adapter = messagesAdapter
            }
            Log.d("ConversationFragment", "RecyclerView setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun hideSystemUI() {
        Log.d("ConversationFragment", "Hiding system UI")
        try {
            // Hide action bar
            (activity as? AppCompatActivity)?.supportActionBar?.hide()
            
            // Hide bottom navigation
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
            
            Log.d("ConversationFragment", "System UI hidden successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error hiding system UI", e)
        }
    }

    private fun setupBackButton() {
        Log.d("ConversationFragment", "Setting up back button")
        try {
            binding.buttonBack.setOnClickListener {
                Log.d("ConversationFragment", "Back button clicked")
                // Show system UI again before going back
                showSystemUI()
                // Use Navigation Component to go back
                findNavController().navigateUp()
            }
            Log.d("ConversationFragment", "Back button setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up back button", e)
            throw e
        }
    }

    private fun setupSendButton() {
        Log.d("ConversationFragment", "Setting up send button")
        try {
            // Make sure the input area is visible
            binding.editTextMessage.hint = "Type a message..."
            binding.editTextMessage.isEnabled = true
            binding.editTextMessage.isFocusable = true
            
            binding.buttonSend.setOnClickListener {
                val messageText = binding.editTextMessage.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    Log.d("ConversationFragment", "Sending message: $messageText")
                    conversationViewModel.sendMessage(messageText)
                    binding.editTextMessage.text.clear()
                }
            }
            
            Log.d("ConversationFragment", "Send button setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up send button", e)
            throw e
        }
    }

    private fun observeMessages() {
        Log.d("ConversationFragment", "Setting up message observer")
        try {
            conversationViewModel.messages.observe(viewLifecycleOwner) { messages ->
                Log.d("ConversationFragment", "Messages updated: ${messages.size} messages")
                messagesAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
            Log.d("ConversationFragment", "Message observer setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up message observer", e)
            throw e
        }
    }

    private fun showSystemUI() {
        Log.d("ConversationFragment", "Showing system UI")
        try {
            // Show action bar
            (activity as? AppCompatActivity)?.supportActionBar?.show()
            
            // Show bottom navigation
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
            
            Log.d("ConversationFragment", "System UI shown successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error showing system UI", e)
        }
    }

    private fun setupPostPreview(postTitle: String, postImageUrl: String, postId: String) {
        Log.d("ConversationFragment", "Setting up post preview: $postTitle, postId: $postId, imageUrl: $postImageUrl")
        try {
            // Set post title
            binding.postPreviewTitle.text = postTitle
            
            // Load post image if available
            if (postImageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(postImageUrl)
                    .placeholder(R.drawable.rounded_image_background)
                    .error(R.drawable.rounded_image_background)
                    .into(binding.postPreviewImage)
            } else {
                binding.postPreviewImage.setImageResource(R.drawable.rounded_image_background)
            }
            
            // Fetch and display post status
            if (postId.isNotEmpty()) {
                fetchPostStatusForPreview(postId)
            } else {
                // Try to fetch by title if no postId available
                fetchPostStatusByTitle(postTitle)
            }
            
            // Setup click listener to navigate to post details
            binding.postPreviewLayout.setOnClickListener {
                Log.d("ConversationFragment", "=== POST PREVIEW CLICKED ===")
                Log.d("ConversationFragment", "postId: '$postId'")
                Log.d("ConversationFragment", "postTitle: '$postTitle'")
                Log.d("ConversationFragment", "postImageUrl: '$postImageUrl'")
                
                if (postId.isNotEmpty() && postId != postTitle) {
                    Log.d("ConversationFragment", "Attempting to fetch post details for navigation: $postId")
                    // Fetch post details from Firestore before navigating
                    fetchPostDetailsAndNavigate(postId, postTitle, postImageUrl)
                } else {
                    Log.w("ConversationFragment", "No valid postId available, trying fallback navigation")
                    Log.w("ConversationFragment", "postId.isEmpty(): ${postId.isEmpty()}")
                    Log.w("ConversationFragment", "postId == postTitle: ${postId == postTitle}")
                    // Fallback: try to navigate with just the title
                    navigateWithFallback(postTitle, postImageUrl)
                }
            }
            
            Log.d("ConversationFragment", "Post preview setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up post preview", e)
        }
    }

    private fun navigateWithFallback(postTitle: String, postImageUrl: String) {
        Log.d("ConversationFragment", "=== FALLBACK NAVIGATION ===")
        Log.d("ConversationFragment", "Using fallback navigation with title: '$postTitle'")
        Log.d("ConversationFragment", "imageUrl: '$postImageUrl'")
        
        // Try to fetch post data by title since we don't have the document ID
        val firestore = FirebaseFirestore.getInstance()
        
        // First try lost_items collection
        firestore.collection("lost_items")
            .whereEqualTo("itemTitle", postTitle)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ConversationFragment", "Fallback: Found ${documents.size()} documents in lost_items")
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    Log.d("ConversationFragment", "Fallback: Post found in lost_items, navigating...")
                    val bundle = Bundle().apply {
                        putString("postType", "lost")
                        putString("itemTitle", document.getString("itemTitle") ?: postTitle)
                        putString("postId", document.id)
                        putString("imageUrl", document.getString("imageUrl") ?: postImageUrl)
                        putString("description", document.getString("description") ?: "")
                        putString("category", document.getString("category") ?: "")
                        putString("location", document.getString("lastSeenAt") ?: "")
                        putString("dateTime", document.getString("dateTimeLost") ?: "") // Use dateTimeLost for lost items
                        putString("dateTimeCreated", document.getString("dateCreated") ?: "")
                        putString("userId", document.getString("userId") ?: "")
                        putString("userName", document.getString("userName") ?: "")
                        putBoolean("isAnonymous", document.getBoolean("isAnonymous") ?: false)
                        putString("roomNumber", document.getString("roomNumber") ?: "") // Add roomNumber
                    }
                    Log.d("ConversationFragment", "Fallback: Created bundle for lost item, navigating...")
                    findNavController().navigate(R.id.navigation_post_details, bundle)
                } else {
                    // Try found_items collection
                    firestore.collection("found_items")
                        .whereEqualTo("itemTitle", postTitle)
                        .get()
                        .addOnSuccessListener { foundDocuments ->
                            Log.d("ConversationFragment", "Fallback: Found ${foundDocuments.size()} documents in found_items")
                            if (!foundDocuments.isEmpty) {
                                val document = foundDocuments.documents[0]
                                Log.d("ConversationFragment", "Fallback: Post found in found_items, navigating...")
                                val bundle = Bundle().apply {
                                    putString("postType", "found")
                                    putString("itemTitle", document.getString("itemTitle") ?: postTitle)
                                    putString("postId", document.id)
                                    putString("imageUrl", document.getString("imageUrl") ?: postImageUrl)
                                    putString("description", document.getString("description") ?: "")
                                    putString("category", document.getString("category") ?: "")
                                    putString("location", document.getString("foundAt") ?: "")
                                    putString("dateTime", document.getString("dateTimeFound") ?: "") // Use dateTimeFound for found items
                                    putString("dateTimeCreated", document.getString("dateCreated") ?: "")
                                    putString("userId", document.getString("userId") ?: "")
                                    putString("userName", document.getString("userName") ?: "")
                                    putBoolean("isAnonymous", document.getBoolean("isAnonymous") ?: false)
                                    putString("roomNumber", document.getString("roomNumber") ?: "") // Add roomNumber
                                }
                                Log.d("ConversationFragment", "Fallback: Created bundle for found item, navigating...")
                                findNavController().navigate(R.id.navigation_post_details, bundle)
                            } else {
                                Log.w("ConversationFragment", "Fallback: Post not found in either collection by title: $postTitle")
                                // Last resort: navigate with minimal data
                                val bundle = Bundle().apply {
                                    putString("postType", "lost")
                                    putString("itemTitle", postTitle)
                                    putString("postId", "")
                                    putString("imageUrl", postImageUrl)
                                    putString("description", "")
                                    putString("category", "")
                                    putString("location", "")
                                    putString("dateTime", "")
                                    putString("dateTimeCreated", "")
                                    putString("userId", "")
                                    putString("userName", "")
                                    putBoolean("isAnonymous", false)
                                    putString("roomNumber", "")
                                }
                                Log.d("ConversationFragment", "Fallback: Using minimal bundle, navigating...")
                                findNavController().navigate(R.id.navigation_post_details, bundle)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ConversationFragment", "Fallback: Error fetching from found_items", exception)
                            // Last resort navigation
                            val bundle = Bundle().apply {
                                putString("postType", "lost")
                                putString("itemTitle", postTitle)
                                putString("postId", "")
                                putString("imageUrl", postImageUrl)
                                putString("description", "")
                                putString("category", "")
                                putString("location", "")
                                putString("dateTime", "")
                                putString("dateTimeCreated", "")
                                putString("userId", "")
                                putString("userName", "")
                                putBoolean("isAnonymous", false)
                                putString("roomNumber", "")
                            }
                            findNavController().navigate(R.id.navigation_post_details, bundle)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationFragment", "Fallback: Error fetching from lost_items", exception)
                // Last resort navigation
                val bundle = Bundle().apply {
                    putString("postType", "lost")
                    putString("itemTitle", postTitle)
                    putString("postId", "")
                    putString("imageUrl", postImageUrl)
                    putString("description", "")
                    putString("category", "")
                    putString("location", "")
                    putString("dateTime", "")
                    putString("dateTimeCreated", "")
                    putString("userId", "")
                    putString("userName", "")
                    putBoolean("isAnonymous", false)
                    putString("roomNumber", "")
                }
                findNavController().navigate(R.id.navigation_post_details, bundle)
            }
    }

    private fun fetchPostDetailsAndNavigate(postId: String, postTitle: String, postImageUrl: String) {
        Log.d("ConversationFragment", "Fetching post details for navigation: postId='$postId', postTitle='$postTitle'")
        
        if (postId.isEmpty()) {
            Log.w("ConversationFragment", "No postId available, using fallback navigation")
            navigateWithFallback(postTitle, postImageUrl)
            return
        }
        
        val firestore = FirebaseFirestore.getInstance()
        
        // First try lost_items collection
        firestore.collection("lost_items")
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("ConversationFragment", "Post found in lost_items, navigating...")
                    val bundle = Bundle().apply {
                        putString("postType", "lost")
                        putString("itemTitle", document.getString("itemTitle") ?: postTitle)
                        putString("postId", postId)
                        putString("imageUrl", document.getString("imageUrl") ?: postImageUrl)
                        putString("description", document.getString("description") ?: "")
                        putString("category", document.getString("category") ?: "")
                        putString("location", document.getString("lastSeenAt") ?: "")
                        putString("dateTime", document.getString("dateTimeLost") ?: "") // Use dateTimeLost for lost items
                        putString("dateTimeCreated", document.getString("dateCreated") ?: "")
                        putString("userId", document.getString("userId") ?: "")
                        putString("userName", document.getString("userName") ?: "")
                        putBoolean("isAnonymous", document.getBoolean("isAnonymous") ?: false)
                        putString("roomNumber", document.getString("roomNumber") ?: "") // Add roomNumber
                    }
                    findNavController().navigate(R.id.navigation_post_details, bundle)
                } else {
                    // Try found_items collection
                    firestore.collection("found_items")
                        .document(postId)
                        .get()
                        .addOnSuccessListener { foundDocument ->
                            if (foundDocument.exists()) {
                                Log.d("ConversationFragment", "Post found in found_items, navigating...")
                                val bundle = Bundle().apply {
                                    putString("postType", "found")
                                    putString("itemTitle", foundDocument.getString("itemTitle") ?: postTitle)
                                    putString("postId", postId)
                                    putString("imageUrl", foundDocument.getString("imageUrl") ?: postImageUrl)
                                    putString("description", foundDocument.getString("description") ?: "")
                                    putString("category", foundDocument.getString("category") ?: "")
                                    putString("location", foundDocument.getString("foundAt") ?: "")
                                    putString("dateTime", foundDocument.getString("dateTimeFound") ?: "") // Use dateTimeFound for found items
                                    putString("dateTimeCreated", foundDocument.getString("dateCreated") ?: "")
                                    putString("userId", foundDocument.getString("userId") ?: "")
                                    putString("userName", foundDocument.getString("userName") ?: "")
                                    putBoolean("isAnonymous", foundDocument.getBoolean("isAnonymous") ?: false)
                                    putString("roomNumber", foundDocument.getString("roomNumber") ?: "") // Add roomNumber
                                }
                                findNavController().navigate(R.id.navigation_post_details, bundle)
                            } else {
                                Log.w("ConversationFragment", "Post not found in either collection by ID: $postId")
                                navigateWithFallback(postTitle, postImageUrl)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ConversationFragment", "Error fetching from found_items", exception)
                            navigateWithFallback(postTitle, postImageUrl)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationFragment", "Error fetching from lost_items", exception)
                navigateWithFallback(postTitle, postImageUrl)
            }
    }
    
    private fun fetchPostStatusForPreview(postId: String) {
        Log.d("ConversationFragment", "Fetching post status for preview - postId: $postId")
        
        val firestore = FirebaseFirestore.getInstance()
        
        // Try to find the post in both collections
        firestore.collection("lost_items").document(postId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isResolved = document.getBoolean("isResolved") ?: false
                    val isDeleted = document.getBoolean("isDeleted") ?: false
                    Log.d("ConversationFragment", "Found post in lost_items - Resolved: $isResolved, Deleted: $isDeleted")
                    displayPostStatus(isResolved, isDeleted)
                } else {
                    // Try found_items
                    firestore.collection("found_items").document(postId).get()
                        .addOnSuccessListener { foundDocument ->
                            if (foundDocument.exists()) {
                                val isResolved = foundDocument.getBoolean("isResolved") ?: false
                                val isDeleted = foundDocument.getBoolean("isDeleted") ?: false
                                Log.d("ConversationFragment", "Found post in found_items - Resolved: $isResolved, Deleted: $isDeleted")
                                displayPostStatus(isResolved, isDeleted)
                            } else {
                                Log.d("ConversationFragment", "Post not found in either collection")
                                displayPostStatus(false, false)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ConversationFragment", "Error fetching from found_items", exception)
                            displayPostStatus(false, false)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationFragment", "Error fetching from lost_items", exception)
                displayPostStatus(false, false)
            }
    }
    
    private fun fetchPostStatusByTitle(postTitle: String) {
        Log.d("ConversationFragment", "Fetching post status by title - postTitle: $postTitle")
        
        val firestore = FirebaseFirestore.getInstance()
        
        // First try lost_items collection
        firestore.collection("lost_items")
            .whereEqualTo("itemTitle", postTitle)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val isResolved = document.getBoolean("isResolved") ?: false
                    val isDeleted = document.getBoolean("isDeleted") ?: false
                    Log.d("ConversationFragment", "Found post in lost_items by title - Resolved: $isResolved, Deleted: $isDeleted")
                    displayPostStatus(isResolved, isDeleted)
                } else {
                    // Try found_items collection
                    firestore.collection("found_items")
                        .whereEqualTo("itemTitle", postTitle)
                        .get()
                        .addOnSuccessListener { foundDocuments ->
                            if (!foundDocuments.isEmpty) {
                                val document = foundDocuments.documents[0]
                                val isResolved = document.getBoolean("isResolved") ?: false
                                val isDeleted = document.getBoolean("isDeleted") ?: false
                                Log.d("ConversationFragment", "Found post in found_items by title - Resolved: $isResolved, Deleted: $isDeleted")
                                displayPostStatus(isResolved, isDeleted)
                            } else {
                                Log.d("ConversationFragment", "Post not found in either collection by title")
                                displayPostStatus(false, false)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ConversationFragment", "Error fetching from found_items by title", exception)
                            displayPostStatus(false, false)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationFragment", "Error fetching from lost_items by title", exception)
                displayPostStatus(false, false)
            }
    }
    
    private fun displayPostStatus(isResolved: Boolean, isDeleted: Boolean) {
        Log.d("ConversationFragment", "Displaying post status - Resolved: $isResolved, Deleted: $isDeleted")
        
        when {
            isDeleted -> {
                binding.postPreviewStatus.visibility = View.VISIBLE
                binding.postPreviewStatus.text = "🗑️ Deleted"
                binding.postPreviewStatus.setTextColor(requireContext().getColor(R.color.status_deleted))
                Log.d("ConversationFragment", "Showing DELETED status in preview")
            }
            isResolved -> {
                binding.postPreviewStatus.visibility = View.VISIBLE
                binding.postPreviewStatus.text = "✓ Resolved"
                binding.postPreviewStatus.setTextColor(requireContext().getColor(R.color.status_resolved))
                Log.d("ConversationFragment", "Showing RESOLVED status in preview")
            }
            else -> {
                binding.postPreviewStatus.visibility = View.GONE
                Log.d("ConversationFragment", "No status to show in preview")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ConversationFragment", "onDestroyView called")
        // Show system UI when leaving conversation
        showSystemUI()
        _binding = null
    }
} 