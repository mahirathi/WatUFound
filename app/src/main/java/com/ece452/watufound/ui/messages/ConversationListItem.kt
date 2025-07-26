package com.ece452.watufound.ui.messages

// New data class for conversation list items
data class ConversationListItem(
    val conversationId: String,
    val posterName: String,
    val postTitle: String,
    val postImageUrl: String? = null,
    val timestamp: String? = null,
    val postId: String? = null
)
