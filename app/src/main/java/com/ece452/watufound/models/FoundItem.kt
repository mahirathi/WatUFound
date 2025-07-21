package com.ece452.watufound.models

data class FoundItem(
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val foundAt: String = "",
    val dateTimeFound: String = "",
    val dateCreated: String = "",
    val userId: String = "",
    val userName: String = "",
    val isAnonymous: Boolean = false
)
