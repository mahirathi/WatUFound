package com.ece452.watufound.ui.messages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class ConversationViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var messagesRef = database.getReference("conversations")
    private var messagesListener: ValueEventListener? = null
    private var currentConversationId: String = ""

    fun initializeConversation(otherUserId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Validate inputs
            if (otherUserId.isBlank() || otherUserId == currentUser.uid) {
                android.util.Log.e("ConversationVM", "Invalid otherUserId: $otherUserId")
                return
            }
            
            // Create a unique conversation ID (sorted to ensure consistency)
            val userIds = listOf(currentUser.uid, otherUserId).sorted()
            currentConversationId = "${userIds[0]}_${userIds[1]}"
            
            android.util.Log.d("ConversationVM", "Initializing conversation: $currentConversationId")
            messagesRef = database.getReference("conversations").child(currentConversationId)
            loadMessages()
        }
    }

    fun initializeAnonymousConversation(conversationId: String) {
        currentConversationId = conversationId
        messagesRef = database.getReference("conversations").child(currentConversationId)
        loadMessages()
    }

    private fun loadMessages() {
        messagesListener = messagesRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messagesList = mutableListOf<Message>()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { messagesList.add(it) }
                    }
                    _messages.value = messagesList
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun sendMessage(text: String) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentConversationId.isNotEmpty()) {
            val message = Message(
                id = UUID.randomUUID().toString(),
                text = text,
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: currentUser.email ?: "Unknown",
                timestamp = System.currentTimeMillis()
            )

            messagesRef.child(message.id).setValue(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.let { messagesRef.removeEventListener(it) }
    }
} 