package com.ece452.watufound.models

data class LostItem(
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val lastSeenAt: String = "",
    val dateTimeLost: String = "",
    val dateCreated: String = ""
)