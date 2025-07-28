package com.ece452.watufound.utils

object CategoryUtils {
    // Centralized list of categories - easy to expand
    val ALL_CATEGORIES = listOf(
        "Accessories",
        "Clothing", 
        "Documents",
        "Electronics",
        "WatCard",
        "Other"

    )
    
    // Categories that have image restrictions
    val RESTRICTED_CATEGORIES = listOf("WatCard")
    
    // Get categories excluding the placeholder
    fun getSelectableCategories(): List<String> = ALL_CATEGORIES
    
    // Check if a category has image restrictions
    fun isImageRestricted(category: String): Boolean = RESTRICTED_CATEGORIES.contains(category)
} 