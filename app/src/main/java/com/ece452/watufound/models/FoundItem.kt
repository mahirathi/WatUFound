package com.ece452.watufound.models

data class FoundItem(
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val foundAt: String = "",
    val roomNumber: String = "",
    val dateTimeFound: String = "",
    val dateCreated: String = "",
    val userId: String = "",
    val userName: String = "",
    val isAnonymous: Boolean = false,
    val imageUrl: String? = null,
    val isResolved: Boolean = false,
    val isDeleted: Boolean = false
)
