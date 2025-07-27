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
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ece452.watufound.utils.BuildingLocationUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.LinearLayout

class PostDetailsFragment : Fragment(), OnMapReadyCallback {
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var lastPin: LatLng? = null
    private var locationName: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_post_details, container, false)
        mapView = view.findViewById(R.id.mapViewPreview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val imagePost: ImageView = view.findViewById(R.id.imagePost)
        val tagStatus: TextView = view.findViewById(R.id.tagStatus)
        val tagCategory: TextView = view.findViewById(R.id.tagCategory)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textDescription: TextView = view.findViewById(R.id.textDescription)
        val textBuilding: TextView = view.findViewById(R.id.textBuilding)
        val textRoom: TextView = view.findViewById(R.id.textRoom)
        val labelRoom: TextView = view.findViewById(R.id.labelRoom)
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textPostedBy: TextView = view.findViewById(R.id.textPostedBy)
        val adminIndicator: TextView = view.findViewById(R.id.adminIndicator)
        val buttonContact: LinearLayout = view.findViewById(R.id.buttonContact)
        val labelDateTime: TextView = view.findViewById(R.id.labelDateTime)
        val textDateTimeCreated: TextView = view.findViewById(R.id.textDateTimeCreated)
        val layoutActionButtons: View = view.findViewById(R.id.layoutActionButtons)
        val buttonResolve: Button = view.findViewById(R.id.buttonResolve)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
        val textResolved: TextView = view.findViewById(R.id.textResolved)
        val buttonContactText: TextView = buttonContact.findViewById(R.id.buttonContactText)

        // Ensure button is visible
        buttonContact.visibility = View.VISIBLE
        
        // Set up toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Get arguments from bundle
        val postType = arguments?.getString("postType")
        val postId = arguments?.getString("postId", "")
        val itemTitle = arguments?.getString("itemTitle", "")
        val description = arguments?.getString("description", "")
        val category = arguments?.getString("category", "")
        val building = arguments?.getString("location", "")
        val dateTime = arguments?.getString("dateTime", "")
        val userId = arguments?.getString("userId", "")
        val userName = arguments?.getString("userName", "")
        val isAnonymous = arguments?.getBoolean("isAnonymous")
        val imageUrl = arguments?.getString("imageUrl")
        val roomNumber = arguments?.getString("roomNumber", "")
        val dateTimeCreated = arguments?.getString("dateTimeCreated", "")

        // Check if post is resolved (we'll need to check this from Firestore)
        checkIfPostIsResolved(postType, itemTitle, userId, dateTimeCreated) { isResolved ->
            if (isResolved) {
                textResolved.visibility = View.VISIBLE
                // For resolved posts, show action buttons but only enable delete
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser?.uid == userId) {
                    layoutActionButtons.visibility = View.VISIBLE
                    buttonResolve.visibility = View.GONE  // Hide resolve button for resolved posts
                    buttonDelete.visibility = View.VISIBLE  // Show delete button for resolved posts
                }
            } else {
                textResolved.visibility = View.GONE
                // Show action buttons only for post owner and unresolved posts
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser?.uid == userId) {
                    layoutActionButtons.visibility = View.VISIBLE
                    buttonResolve.visibility = View.VISIBLE  // Show resolve button for unresolved posts
                    buttonDelete.visibility = View.VISIBLE  // Show delete button for unresolved posts
                }
            }
        }

        // Load image if available
        if (!imageUrl.isNullOrBlank()) {
            imagePost.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(imagePost)
        } else {
            imagePost.visibility = View.GONE
        }

        if(!roomNumber.isNullOrBlank()) {
            textRoom.visibility = View.VISIBLE
            labelRoom.visibility = View.VISIBLE
            textRoom.text = roomNumber
        } else {
            textRoom.visibility = View.GONE
            labelRoom.visibility = View.GONE
        }

        // Debug logging
        android.util.Log.d("PostDetailsFragment", "Received data - userId: '$userId', userName: '$userName'")
        android.util.Log.d("PostDetailsFragment", "Arguments: $arguments")
        
        // Check if current user is admin
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isAdmin = currentUser?.email?.endsWith("@admin.watufound.com") == true ||
                     currentUser?.email == "admin@watufound.com"

        android.util.Log.d("PostDetailsFragment", "Current user: ${currentUser?.email}, isAdmin: $isAdmin")

        // Set Posted By - show actual user name for admins even if posted anonymously
        if (isAnonymous == true && isAdmin) {
            textPostedBy.text = "Anonymous (Posted by: $userName)"
            adminIndicator.visibility = View.VISIBLE
            android.util.Log.d("PostDetailsFragment", "Showing admin view for anonymous post by: $userName")
        } else if (isAnonymous == true) {
            textPostedBy.text = "Anonymous"
            adminIndicator.visibility = View.GONE
            android.util.Log.d("PostDetailsFragment", "Showing regular anonymous view")
        } else {
            textPostedBy.text = userName
            adminIndicator.visibility = View.GONE
            android.util.Log.d("PostDetailsFragment", "Showing regular user view for: $userName")
        }

        // Show delete button for admin users, but hide if admin is viewing their own post
        val buttonDeletePost: Button = view.findViewById(R.id.buttonDeletePost)
        if (isAdmin && currentUser?.uid != userId) {
            buttonDeletePost.visibility = View.VISIBLE
            android.util.Log.d("PostDetailsFragment", "Showing delete button for admin viewing another user's post")
        } else {
            buttonDeletePost.visibility = View.GONE
            android.util.Log.d("PostDetailsFragment", "Hiding delete button - not admin or admin viewing own post")
        }

        // Check if this is the current user's post
        if (currentUser?.uid == userId) {
            android.util.Log.d("PostDetailsFragment", "This is the current user's post")
            buttonContact.isEnabled = false
            buttonContactText.text = "Your Post"
            buttonContact.setBackgroundResource(R.drawable.contact_button_disabled_background)

            // Hide the chat icon for "Your Post" button
            val chatIcon = buttonContact.findViewById<android.widget.ImageView>(R.id.chatIcon)
            chatIcon?.visibility = android.view.View.GONE
            // Action buttons visibility will be handled by the resolved check
        } else {
            android.util.Log.d("PostDetailsFragment", "This is another user's post")
            // Hide action buttons for other users' posts
            layoutActionButtons.visibility = View.GONE
            // Set contact button text to "Start Conversation"
            buttonContactText.text = "Start Conversation"

            // Show the chat icon for "Start Conversation" button
            val chatIcon = buttonContact.findViewById<android.widget.ImageView>(R.id.chatIcon)
            chatIcon?.visibility = android.view.View.VISIBLE
            // Set contact button text based on anonymity and admin status
            if (isAnonymous == true && isAdmin) {
                buttonContactText.text = "Contact $userName"
            } else if (isAnonymous == true) {
                buttonContactText.text = "Contact Anonymous"
            } else {
                buttonContactText.text = "Contact $userName"
            }
        }
        
        // Populate data based on post type
        when (postType) {
            "lost" -> {
                tagStatus.text = "Lost"
                tagStatus.setBackgroundResource(R.drawable.tag_lost_bg)
                tagStatus.setTextColor(requireContext().getColor(R.color.lost_text))
                toolbar.title = "Lost Item Details"
                labelDateTime.text = "Reported Lost at"
            }
            "found" -> {
                tagStatus.text = "Found"
                tagStatus.setBackgroundResource(R.drawable.tag_found_bg)
                tagStatus.setTextColor(requireContext().getColor(R.color.found_text))
                toolbar.title = "Found Item Details"
                labelDateTime.text = "Time Posted"
            }
        }
        
        // Set toolbar title text color to black for light theme
        toolbar.setTitleTextColor(requireContext().getColor(R.color.black))

        // Set the data
        tagCategory.text = category
        tagCategory.setBackgroundResource(R.drawable.tag_category_bg)
        tagCategory.setTextColor(requireContext().getColor(R.color.category_text))
        textTitle.text = itemTitle
        textDescription.text = description
        locationName = building
        textBuilding.text = building
        textDateTimeCreated.text = TimeUtils.getRelativeTimeAgo(dateTimeCreated ?: "")

        // Format the date/time in the desired format
        textDateTime.text = formatDateTime(dateTime ?: "")
        
        // Set up contact button
        buttonContact.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                android.util.Log.e("PostDetailsFragment", "Contact failed: currentUser is null")
                return@setOnClickListener
            }
            if (userId.isNullOrEmpty()) {
                android.util.Log.e("PostDetailsFragment", "Contact failed: userId is null or empty")
                return@setOnClickListener
            }
            if (userName.isNullOrEmpty()) {
                android.util.Log.e("PostDetailsFragment", "Contact failed: userName is null or empty")
                return@setOnClickListener
            }
            if (itemTitle.isNullOrEmpty()) {
                android.util.Log.e("PostDetailsFragment", "Contact failed: itemTitle is null or empty")
                return@setOnClickListener
            }
            if (currentUser.uid == userId) {
                buttonContact.isEnabled = false
                buttonContactText.text = "Your Post"
                buttonContact.setBackgroundResource(R.drawable.contact_button_disabled_background)

                // Hide the chat icon for "Your Post" button
                val chatIcon = buttonContact.findViewById<android.widget.ImageView>(R.id.chatIcon)
                chatIcon?.visibility = android.view.View.GONE
                return@setOnClickListener
            }
            // For conversation, always use "Anonymous" for anonymous posts (even for admins)
            // This maintains privacy in conversations while still showing admin info in UI
            val posterName = if (isAnonymous == true) "Anonymous" else userName
            val postTitleForConv = itemTitle
            val db = FirebaseDatabase.getInstance().reference

            // Debug logging for image URL
            android.util.Log.d("PostDetailsFragment", "Creating conversation with imageUrl: '$imageUrl'")

            val userConversationsRef = db.child("userConversations").child(currentUser.uid)
            userConversationsRef.get().addOnSuccessListener { snapshot ->
                var existingConversationId: String? = null
                for (child in snapshot.children) {
                    val poster = child.child("posterName").getValue(String::class.java)
                    val postTitle = child.child("postTitle").getValue(String::class.java)
                    if (poster == posterName && postTitle == postTitleForConv) {
                        existingConversationId = child.child("conversationId").getValue(String::class.java)
                        break
                    }
                }
                val conversationId = existingConversationId ?: if (isAnonymous == true) java.util.UUID.randomUUID().toString() else "${currentUser.uid}_${userId}_${postTitleForConv}"
                if (existingConversationId == null) {
                    val currentTime = System.currentTimeMillis()
                    val metaForCurrentUser = mapOf(
                        "conversationId" to conversationId,
                        "posterName" to posterName,
                        "postTitle" to postTitleForConv,
                        "postImageUrl" to (imageUrl ?: ""),
                        "postId" to postId,
                        "createdAt" to currentTime
                    )
                    val senderName = currentUser.displayName ?: currentUser.email ?: "You"
                    val metaForPoster = mapOf(
                        "conversationId" to conversationId,
                        "posterName" to senderName,
                        "postTitle" to postTitleForConv,
                        "postImageUrl" to (imageUrl ?: ""),
                        "postId" to postId,
                        "createdAt" to currentTime
                    )
                    db.child("userConversations").child(currentUser.uid).child(conversationId).setValue(metaForCurrentUser)
                    db.child("userConversations").child(userId).child(conversationId).setValue(metaForPoster)
                }
                val bundle = Bundle().apply {
                    putString("otherUserId", userId)
                    putString("otherUserName", posterName)
                    putString("postTitle", postTitleForConv)
                    putString("postId", postId)
                    putString("postImageUrl", imageUrl ?: "")
                    putString("conversationId", conversationId)
                    if (isAnonymous == true) {
                        putString("anonConversationId", conversationId)
                    }
                }
                findNavController().navigate(R.id.navigation_conversation, bundle)
            }
        }

        // Set up delete button for admin users
        buttonDeletePost.setOnClickListener {
            if (!isAdmin) {
                return@setOnClickListener
            }

            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    deletePost(userId, postType, itemTitle)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Set up resolve and delete button click listeners
        buttonResolve.setOnClickListener {
            handleResolvePost(postType, itemTitle, userId, dateTimeCreated)
        }

        buttonDelete.setOnClickListener {
            handleDeletePost(postType, itemTitle, userId, dateTimeCreated)
        }
    }

    private fun deletePost(userId: String?, postType: String?, itemTitle: String?) {
        if (userId.isNullOrEmpty() || postType.isNullOrEmpty() || itemTitle.isNullOrEmpty()) {
            android.util.Log.e("PostDetailsFragment", "Cannot delete post: missing required data")
            android.widget.Toast.makeText(requireContext(), "Cannot delete post: missing data", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("PostDetailsFragment", "Attempting to delete post - userId: $userId, postType: $postType, itemTitle: $itemTitle")

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val collectionName = if (postType == "lost") "lost_items" else "found_items"

        android.util.Log.d("PostDetailsFragment", "Looking in collection: $collectionName")

        // Find and delete the post from Firestore
        db.collection(collectionName)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemTitle", itemTitle)
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("PostDetailsFragment", "Found ${querySnapshot.size()} matching posts")

                if (querySnapshot.isEmpty) {
                    android.util.Log.e("PostDetailsFragment", "No posts found matching criteria")
                    android.widget.Toast.makeText(requireContext(), "Post not found in database", android.widget.Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Delete the first matching post (should be unique based on userId + itemTitle)
                val documentToDelete = querySnapshot.documents.first()
                documentToDelete.reference.delete()
                    .addOnSuccessListener {
                        android.util.Log.d("PostDetailsFragment", "Post deleted successfully")
                        android.widget.Toast.makeText(requireContext(), "Post deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                        // Navigate back to feed
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e("PostDetailsFragment", "Failed to delete post", exception)
                        android.widget.Toast.makeText(requireContext(), "Failed to delete post: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("PostDetailsFragment", "Failed to find post for deletion", exception)
                android.widget.Toast.makeText(requireContext(), "Failed to access database: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDateTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            if (date != null) {
                val outputFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                outputFormat.format(date)
            } else {
                dateTimeString // Return original if parsing fails
            }
        } catch (e: Exception) {
            dateTimeString // Return original if parsing fails
        }
    }

    private fun updateMapForLocation(location: String?) {
        if (location.isNullOrBlank() || location == "Select Location" || location == "Other") {
            googleMap?.clear()
            return
        }
        val latLng = BuildingLocationUtils.getBuildingLatLng(requireContext(), location!!)
        if (latLng != null) {
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title(location))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            lastPin = latLng
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Enable zoom controls
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        // Apply purple theme styling
        val purpleStyle = """
            [
                {
                    "featureType": "all",
                    "elementType": "geometry.fill",
                    "stylers": [
                        {
                            "color": "#f3e5f5"
                        }
                    ]
                },
                {
                    "featureType": "water",
                    "elementType": "geometry",
                    "stylers": [
                        {
                            "color": "#e1bee7"
                        }
                    ]
                },
                {
                    "featureType": "road",
                    "elementType": "geometry",
                    "stylers": [
                        {
                            "color": "#f8f0ff"
                        }
                    ]
                }
            ]
        """.trimIndent()
        
        try {
            val mapStyleOptions = com.google.android.gms.maps.model.MapStyleOptions(purpleStyle)
            googleMap?.setMapStyle(mapStyleOptions)
        } catch (e: Exception) {
            android.util.Log.e("PostDetailsFragment", "Map styling failed", e)
        }
        
        updateMapForLocation(locationName)
    }

    private fun checkIfPostIsResolved(postType: String?, itemTitle: String?, userId: String?, dateTimeCreated: String?, callback: (Boolean) -> Unit) {
        val db = Firebase.firestore
        val collectionName = when (postType) {
            "lost" -> "lost_items"
            "found" -> "found_items"
            else -> {
                callback(false)
                return
            }
        }

        if (itemTitle.isNullOrEmpty() || userId.isNullOrEmpty() || dateTimeCreated.isNullOrEmpty()) {
            callback(false)
            return
        }

        val query = db.collection(collectionName)
            .whereEqualTo("itemTitle", itemTitle)
            .whereEqualTo("userId", userId)
            .whereEqualTo("dateCreated", dateTimeCreated)

        query.get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val document = documents.documents[0]
                val isResolved = document.getBoolean("isResolved") ?: false
                callback(isResolved)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun handleResolvePost(postType: String?, itemTitle: String?, userId: String?, dateTimeCreated: String?) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Resolve Post")
            .setMessage("Are you sure you want to mark this post as resolved?")
            .setPositiveButton("Resolve") { _, _ ->
                resolvePostInFirestore(postType, itemTitle, userId, dateTimeCreated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleDeletePost(postType: String?, itemTitle: String?, userId: String?, dateTimeCreated: String?) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePostFromFirestore(postType, itemTitle, userId, dateTimeCreated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resolvePostInFirestore(postType: String?, itemTitle: String?, userId: String?, dateTimeCreated: String?) {
        val db = Firebase.firestore
        val collectionName = when (postType) {
            "lost" -> "lost_items"
            "found" -> "found_items"
            else -> return
        }

        if (itemTitle.isNullOrEmpty() || userId.isNullOrEmpty() || dateTimeCreated.isNullOrEmpty()) {
            android.util.Log.e("PostDetailsFragment", "Missing required data for resolve")
            return
        }

        val query = db.collection(collectionName)
            .whereEqualTo("itemTitle", itemTitle)
            .whereEqualTo("userId", userId)
            .whereEqualTo("dateCreated", dateTimeCreated)

        query.get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val document = documents.documents[0]
                document.reference.update("isResolved", true)
                    .addOnSuccessListener {
                        android.util.Log.d("PostDetailsFragment", "Post resolved successfully")
                        // Navigate back to home
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("PostDetailsFragment", "Error resolving post", e)
                    }
            }
        }
    }

    private fun deletePostFromFirestore(postType: String?, itemTitle: String?, userId: String?, dateTimeCreated: String?) {
        val db = com.google.firebase.Firebase.firestore
        val collectionName = when (postType) {
            "lost" -> "lost_items"
            "found" -> "found_items"
            else -> return
        }

        if (itemTitle.isNullOrEmpty() || userId.isNullOrEmpty() || dateTimeCreated.isNullOrEmpty()) {
            android.util.Log.e("PostDetailsFragment", "Missing required data for delete")
            return
        }

        val query = db.collection(collectionName)
            .whereEqualTo("itemTitle", itemTitle)
            .whereEqualTo("userId", userId)
            .whereEqualTo("dateCreated", dateTimeCreated)

        query.get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val document = documents.documents[0]
                // Soft delete by setting isDeleted to true
                document.reference.update("isDeleted", true)
                    .addOnSuccessListener {
                        android.util.Log.d("PostDetailsFragment", "Post soft deleted successfully")
                        // Navigate back to home
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("PostDetailsFragment", "Error soft deleting post", e)
                    }
            }
        }
    }

    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onDestroyView() { super.onDestroyView(); mapView?.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
} 