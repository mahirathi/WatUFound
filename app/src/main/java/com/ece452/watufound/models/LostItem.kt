package com.ece452.watufound.models

data class LostItem(
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val lastSeenAt: String = "",
    val dateTimeLost: String = "",
    val dateCreated: String = "",
    val userId: String = "",
    val userName: String = "",
    val isAnonymous: Boolean = false,
    val imageUrl: String? = null
)