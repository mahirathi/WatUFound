package com.ece452.watufound.ui.messages

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.Message
import com.ece452.watufound.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ConversationsViewModel : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _filteredUsers = MutableLiveData<List<User>>()
    val filteredUsers: LiveData<List<User>> = _filteredUsers

    private val _recentConversations = MutableLiveData<List<User>>()
    val recentConversations: LiveData<List<User>> = _recentConversations
    
    private val _isLoadingRecentConversations = MutableLiveData<Boolean>()
    val isLoadingRecentConversations: LiveData<Boolean> = _isLoadingRecentConversations

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = database.getReference("userProfiles")
    private val conversationsRef = database.getReference("conversations")
    private var usersListener: ValueEventListener? = null
    private var conversationsListener: ValueEventListener? = null

    init {
        loadUsers()
        loadRecentConversations()
        testFirebaseAuthAccess()
    }

    private fun testFirebaseAuthAccess() {
        val currentUser = auth.currentUser
        Log.d("ConversationsVM", "=== FIREBASE AUTH TEST ===")
        Log.d("ConversationsVM", "Current user UID: ${currentUser?.uid}")
        Log.d("ConversationsVM", "Current user email: ${currentUser?.email}")
        Log.d("ConversationsVM", "Current user display name: ${currentUser?.displayName}")
        Log.d("ConversationsVM", "Current user is email verified: ${currentUser?.isEmailVerified}")
        Log.d("ConversationsVM", "=== END FIREBASE AUTH TEST ===")
    }

    private fun loadUsers() {
        val currentUser = auth.currentUser
        Log.d("ConversationsVM", "Loading users. Current user: ${currentUser?.uid}")
        
        if (currentUser != null) {
            // Try to load users from Firestore first (more reliable)
            loadUsersFromFirestore(currentUser.uid)
            
            // Also try Realtime Database as backup
            loadUsersFromRealtimeDB(currentUser.uid)
        } else {
            Log.e("ConversationsVM", "No current user found")
        }
    }

    private fun loadUsersFromFirestore(currentUserId: String) {
        Log.d("ConversationsVM", "Loading users from Firestore")
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val usersList = mutableListOf<User>()
                for (document in documents) {
                    val uid = document.id
                    val name = document.getString("name") ?: "Unknown"
                    val email = document.getString("email") ?: "unknown@example.com"
                    
                    // Don't include the current user or "Unknown" users
                    if (uid != currentUserId && name != "Unknown") {
                        val user = User(uid, name, email)
                        Log.d("ConversationsVM", "Found Firestore user: $name ($email)")
                        usersList.add(user)
                    }
                }
                
                Log.d("ConversationsVM", "Firestore users found: ${usersList.size}")
                if (usersList.isNotEmpty()) {
                    _users.value = usersList
                    // Don't show all users initially - only show search results
                    _filteredUsers.value = emptyList()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationsVM", "Firestore read failed: ${exception.message}")
            }
    }

    private fun loadUsersFromRealtimeDB(currentUserId: String) {
        Log.d("ConversationsVM", "Loading users from Realtime Database")
        // Listen for real user profiles from Firebase
        usersListener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ConversationsVM", "Realtime DB data changed. Children count: ${snapshot.childrenCount}")
                val usersList = mutableListOf<User>()
                
                for (userSnapshot in snapshot.children) {
                    Log.d("ConversationsVM", "Processing Realtime DB user: ${userSnapshot.key}")
                    val user = userSnapshot.getValue(User::class.java)
                    // Don't include the current user or "Unknown" users
                    if (user != null && user.uid != currentUserId && user.displayName != "Unknown") {
                        Log.d("ConversationsVM", "Adding Realtime DB user to list: ${user.displayName}")
                        usersList.add(user)
                    }
                }
                
                Log.d("ConversationsVM", "Realtime DB users found: ${usersList.size}")
                // Only update if we don't have Firestore users
                if (_users.value.isNullOrEmpty()) {
                    _users.value = usersList
                    // Don't show all users initially - only show search results
                    _filteredUsers.value = emptyList()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ConversationsVM", "Realtime DB read failed: ${error.message}")
                // If no real users found, show sample users
                if (_users.value.isNullOrEmpty()) {
                    val sampleUsers = listOf(
                        User("user1", "Alice Johnson", "alice@example.com"),
                        User("user2", "Bob Smith", "bob@example.com"),
                        User("user3", "Charlie Brown", "charlie@example.com"),
                        User("user4", "Diana Prince", "diana@example.com"),
                        User("user5", "Eve Wilson", "eve@example.com")
                    )
                    Log.d("ConversationsVM", "Using sample users: ${sampleUsers.size}")
                    _users.value = sampleUsers
                    // Don't show all users initially - only show search results
                    _filteredUsers.value = emptyList()
                }
            }
        })
    }

    fun searchUsers(query: String) {
        Log.d("ConversationsVM", "Searching for: '$query'")
        val allUsers = _users.value ?: emptyList()
        Log.d("ConversationsVM", "Total users to search: ${allUsers.size}")
        
        if (query.isEmpty()) {
            Log.d("ConversationsVM", "Empty query, showing no users")
            _filteredUsers.value = emptyList()
        } else {
            val filtered = allUsers.filter { user ->
                val matchesName = user.displayName.contains(query, ignoreCase = true)
                val matchesEmail = user.email.contains(query, ignoreCase = true)
                Log.d("ConversationsVM", "User ${user.displayName}: name=$matchesName, email=$matchesEmail")
                matchesName || matchesEmail
            }
            Log.d("ConversationsVM", "Filtered results: ${filtered.size}")
            _filteredUsers.value = filtered
        }
    }

    private fun loadRecentConversations() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        Log.d("ConversationsVM", "Loading recent conversations for user: ${currentUser.uid}")
        _isLoadingRecentConversations.value = true
        
        conversationsListener = conversationsRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversationData = mutableListOf<ConversationData>()
                
                // First pass: collect all conversation data
                for (conversationSnapshot in snapshot.children) {
                    val conversationId = conversationSnapshot.key ?: continue
                    
                                            // Check if this conversation includes the current user
                        if (conversationId.contains(currentUser.uid)) {
                            // Extract the other user's ID from conversation ID
                            val userIds = conversationId.split("_")
                            
                            // Validate conversation ID format
                            if (userIds.size != 2) {
                                Log.w("ConversationsVM", "Invalid conversation ID format: $conversationId")
                                continue
                            }
                            
                            val otherUserId = if (userIds[0] == currentUser.uid) userIds[1] else userIds[0]
                        
                        // Get the latest message to determine if conversation is recent
                        var latestTimestamp = 0L
                        var otherUserName = "Unknown"
                        
                        for (messageSnapshot in conversationSnapshot.children) {
                            val message = messageSnapshot.getValue(Message::class.java)
                            if (message != null && message.timestamp > latestTimestamp) {
                                latestTimestamp = message.timestamp
                                // Get the other user's name from the message
                                if (message.senderId == otherUserId) {
                                    otherUserName = message.senderName
                                } else if (message.senderId != currentUser.uid) {
                                    // This message is from the other user
                                    otherUserName = message.senderName
                                }
                            }
                        }
                        
                        // Only add if we have a valid conversation with messages
                        if (latestTimestamp > 0 && otherUserId != currentUser.uid) {
                            conversationData.add(ConversationData(otherUserId, otherUserName, latestTimestamp))
                        }
                    }
                }
                
                // Sort by latest timestamp
                conversationData.sortByDescending { it.latestTimestamp }
                
                // Load names for users that need it
                val finalUsers = mutableListOf<User>()
                var loadedCount = 0
                val totalCount = conversationData.size
                
                for (conversation in conversationData) {
                    if (conversation.userName == "Unknown" || conversation.userName.isEmpty()) {
                        // Load name from Firestore
                        firestore.collection("users").document(conversation.userId).get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val name = document.getString("name") ?: "Unknown"
                                    if (name != "Unknown") {
                                        finalUsers.add(User(conversation.userId, name, ""))
                                    }
                                }
                                loadedCount++
                                if (loadedCount == totalCount) {
                                    _recentConversations.value = finalUsers
                                    _isLoadingRecentConversations.value = false
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("ConversationsVM", "Failed to load name for user ${conversation.userId}: ${exception.message}")
                                loadedCount++
                                if (loadedCount == totalCount) {
                                    _recentConversations.value = finalUsers
                                    _isLoadingRecentConversations.value = false
                                }
                            }
                    } else {
                        // Name is already available
                        finalUsers.add(User(conversation.userId, conversation.userName, ""))
                        loadedCount++
                        if (loadedCount == totalCount) {
                            _recentConversations.value = finalUsers
                            _isLoadingRecentConversations.value = false
                        }
                    }
                }
                
                // If no conversations, update immediately
                if (conversationData.isEmpty()) {
                    _recentConversations.value = emptyList()
                    _isLoadingRecentConversations.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ConversationsVM", "Failed to load recent conversations: ${error.message}")
            }
        })
    }
    
    private data class ConversationData(
        val userId: String,
        val userName: String,
        val latestTimestamp: Long
    )

    fun deleteConversation(otherUserId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        // Validate inputs
        if (otherUserId.isBlank() || otherUserId == currentUser.uid) {
            Log.e("ConversationsVM", "Invalid otherUserId for deletion: $otherUserId")
            return
        }

        Log.d("ConversationsVM", "Deleting conversation between ${currentUser.uid} and $otherUserId")
        
        // Create the conversation ID (sorted to ensure consistency)
        val userIds = listOf(currentUser.uid, otherUserId).sorted()
        val conversationId = "${userIds[0]}_${userIds[1]}"
        
        // Delete the entire conversation from Firebase
        conversationsRef.child(conversationId).removeValue()
            .addOnSuccessListener {
                Log.d("ConversationsVM", "Conversation deleted successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("ConversationsVM", "Failed to delete conversation: ${exception.message}")
            }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.let { usersRef.removeEventListener(it) }
        conversationsListener?.let { conversationsRef.removeEventListener(it) }
    }
} 