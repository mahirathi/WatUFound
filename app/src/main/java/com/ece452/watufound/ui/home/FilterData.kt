package com.ece452.watufound.ui.home

import java.util.Date

data class FilterData(
    val selectedCategories: Set<String> = emptySet(),
    val startDateTime: Date? = null,
    val endDateTime: Date? = null,
    val searchQuery: String = ""
) {
    fun hasActiveFilters(): Boolean {
        return selectedCategories.isNotEmpty() || startDateTime != null || endDateTime != null || searchQuery.isNotEmpty()
    }
    
    fun clearAll(): FilterData {
        return FilterData()
    }
} 