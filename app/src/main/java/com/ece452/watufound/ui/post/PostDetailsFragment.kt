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
        val textLocation: TextView = view.findViewById(R.id.textLocation)
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textPostedBy: TextView = view.findViewById(R.id.textPostedBy)
        val buttonContact: Button = view.findViewById(R.id.buttonContact)
        
                // Ensure button is visible
        buttonContact.visibility = View.VISIBLE
        
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
        val imageUrl = arguments?.getString("imageUrl")
        
        // Load image if available
        if (!imageUrl.isNullOrBlank()) {
            imagePost.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(imagePost)
        } else {
            imagePost.visibility = View.GONE
        }
        
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
            buttonContact.setBackgroundResource(R.drawable.contact_button_disabled_background)
            buttonContact.setCompoundDrawables(null, null, null, null)
        } else {
            android.util.Log.d("PostDetailsFragment", "This is another user's post")
            // Set contact button text to "Start Conversation"
            buttonContact.text = "Start Conversation"
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
        
        // Set toolbar title text color to black for light theme
        toolbar.setTitleTextColor(requireContext().getColor(R.color.black))
        
        // Set the data
        tagCategory.text = category
        tagCategory.setBackgroundResource(R.drawable.tag_category_bg)
        tagCategory.setTextColor(requireContext().getColor(R.color.category_text))
        textTitle.text = itemTitle
        textDescription.text = description
        locationName = location
        textLocation.text = location
        textDateTime.text = TimeUtils.getRelativeTimeAgo(dateTime ?: "")
        
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
                buttonContact.text = "Your Post"
                buttonContact.setBackgroundResource(R.drawable.contact_button_disabled_background)
                buttonContact.setCompoundDrawables(null, null, null, null)
                return@setOnClickListener
            }
            val posterName = if (isAnonymous == true) "Anonymous" else userName
            val postTitleForConv = itemTitle
            val db = FirebaseDatabase.getInstance().reference
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
                    val metaForCurrentUser = mapOf(
                        "conversationId" to conversationId,
                        "posterName" to posterName,
                        "postTitle" to postTitleForConv
                    )
                    val senderName = currentUser.displayName ?: currentUser.email ?: "You"
                    val metaForPoster = mapOf(
                        "conversationId" to conversationId,
                        "posterName" to senderName,
                        "postTitle" to postTitleForConv
                    )
                    db.child("userConversations").child(currentUser.uid).child(conversationId).setValue(metaForCurrentUser)
                    db.child("userConversations").child(userId).child(conversationId).setValue(metaForPoster)
                }
                val bundle = Bundle().apply {
                    putString("otherUserId", userId)
                    putString("otherUserName", posterName)
                    putString("postTitle", postTitleForConv)
                    putString("conversationId", conversationId)
                    if (isAnonymous == true) {
                        putString("anonConversationId", conversationId)
                    }
                }
                findNavController().navigate(R.id.navigation_conversation, bundle)
            }
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

    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onDestroyView() { super.onDestroyView(); mapView?.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
} 