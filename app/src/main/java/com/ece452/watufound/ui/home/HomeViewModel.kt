package com.ece452.watufound.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.FoundItem
import com.ece452.watufound.models.LostItem
import com.ece452.watufound.utils.CategoryUtils
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _allPosts = MutableLiveData<List<FeedItem>>()
    val allPosts: LiveData<List<FeedItem>> = _allPosts

    private val _filteredPosts = MutableLiveData<List<FeedItem>>()
    val filteredPosts: LiveData<List<FeedItem>> = _filteredPosts

    enum class Filter { ALL, LOST, FOUND }
    private var currentFilter = Filter.ALL
    private var filterData = FilterData()

    init {
        fetchPosts()
    }

    fun setFilter(filter: Filter) {
        currentFilter = filter
        filterPosts()
    }

    fun setSearchQuery(query: String) {
        filterData = filterData.copy(searchQuery = query.lowercase().trim())
        filterPosts()
    }

    fun setSelectedCategories(categories: Set<String>) {
        filterData = filterData.copy(selectedCategories = categories)
        filterPosts()
    }

    fun setDateTimeRange(startDateTime: Date?, endDateTime: Date?) {
        filterData = filterData.copy(startDateTime = startDateTime, endDateTime = endDateTime)
        filterPosts()
    }

    fun getFilterData(): FilterData = filterData

    fun clearAllFilters() {
        filterData = FilterData()
        filterPosts()
    }

    fun refreshPosts() {
        fetchPosts()
    }

    private fun filterPosts() {
        val posts = _allPosts.value ?: emptyList()
        
        var filtered = when (currentFilter) {
            Filter.ALL -> posts
            Filter.LOST -> posts.filterIsInstance<FeedItem.Lost>()
            Filter.FOUND -> posts.filterIsInstance<FeedItem.Found>()
        }

        // Apply search filter (title, description, location)
        if (filterData.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { post ->
                val title = when (post) {
                    is FeedItem.Lost -> post.item.itemTitle.lowercase()
                    is FeedItem.Found -> post.item.itemTitle.lowercase()
                }
                val description = when (post) {
                    is FeedItem.Lost -> post.item.description.lowercase()
                    is FeedItem.Found -> post.item.description.lowercase()
                }
                val location = when (post) {
                    is FeedItem.Lost -> post.item.lastSeenAt.lowercase()
                    is FeedItem.Found -> post.item.foundAt.lowercase()
                }
                title.contains(filterData.searchQuery) || 
                description.contains(filterData.searchQuery) || 
                location.contains(filterData.searchQuery)
            }
        }

        // Apply category filter
        if (filterData.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { post ->
                val category = when (post) {
                    is FeedItem.Lost -> post.item.category
                    is FeedItem.Found -> post.item.category
                }
                filterData.selectedCategories.contains(category)
            }
        }

        // Apply date/time range filter
        if (filterData.startDateTime != null || filterData.endDateTime != null) {
            filtered = filtered.filter { post ->
                val dateString = when (post) {
                    is FeedItem.Lost -> post.item.dateCreated
                    is FeedItem.Found -> post.item.dateCreated
                }
                
                try {
                    val postDate = parseDateString(dateString)
                    if (postDate == null) return@filter false
                    
                    val afterStart = filterData.startDateTime == null || 
                                   postDate.after(filterData.startDateTime) || 
                                   postDate.equals(filterData.startDateTime)
                    val beforeEnd = filterData.endDateTime == null || 
                                  postDate.before(filterData.endDateTime) || 
                                  postDate.equals(filterData.endDateTime)
                    
                    afterStart && beforeEnd
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error parsing date for filtering: $dateString", e)
                    false
                }
            }
        }

        _filteredPosts.value = filtered
    }

    private fun parseDateString(dateString: String): Date? {
        return try {
            when {
                dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}, \\d{2}:\\d{2}")) -> {
                    // Format: "2025-07-23, 14:30"
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
                    dateFormat.parse(dateString)
                }
                dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) -> {
                    // Format: "2025-07-23 14:30"
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    dateFormat.parse(dateString)
                }
                else -> {
                    // Try default format as fallback
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
                    dateFormat.parse(dateString)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error parsing date: $dateString", e)
            null
        }
    }

    private fun fetchPosts() {
        val lostTask = db.collection("lost_items").get()
        val foundTask = db.collection("found_items").get()
        lostTask.addOnSuccessListener { lostResult ->
            foundTask.addOnSuccessListener { foundResult ->
                val lostList = lostResult.documents.mapNotNull { doc ->

                    // Log raw Firestore data before deserialization
                    val raw = doc.data
                    android.util.Log.d("HomeViewModel", "RAW LOST Firestore data: $raw")

                    val item = doc.toObject(LostItem::class.java)
                    val isAnonymous = raw?.get("isAnonymous") as? Boolean ?: false
                    val isResolved = raw?.get("isResolved") as? Boolean ?: false
                    val isDeleted = raw?.get("isDeleted") as? Boolean ?: false

                    // Skip deleted posts
                    if (isDeleted) {
                        return@mapNotNull null
                    }

                    if (item != null) {
                        val fixedItem = item.copy(isAnonymous = isAnonymous, isResolved = isResolved, isDeleted = isDeleted)
                        android.util.Log.d("HomeViewModel", " LOST Item is: $fixedItem")
                        FeedItem.Lost(fixedItem)
                    } else null
                }
                val foundList = foundResult.documents.mapNotNull { doc ->
                    val raw = doc.data
                    android.util.Log.d("HomeViewModel", "RAW FOUND Firestore data: $raw")


                    val item = doc.toObject(FoundItem::class.java)
                    val isAnonymous = raw?.get("isAnonymous") as? Boolean ?: false
                    val isResolved = raw?.get("isResolved") as? Boolean ?: false
                    val isDeleted = raw?.get("isDeleted") as? Boolean ?: false

                    // Skip deleted posts
                    if (isDeleted) {
                        return@mapNotNull null
                    }

                    if (item != null) {
                        val fixedItem = item.copy(isAnonymous = isAnonymous, isResolved = isResolved, isDeleted = isDeleted)
                        android.util.Log.d("HomeViewModel", "FOUND Item is: $fixedItem")
                        FeedItem.Found(fixedItem)
                    } else null
                }
                val combined = (lostList + foundList).sortedByDescending { post ->
                    val dateString = when (post) {
                        is FeedItem.Lost -> post.item.dateCreated
                        is FeedItem.Found -> post.item.dateCreated
                    }
                    
                    // Parse the date string to get a proper Date object for comparison
                    try {
                        val timestamp = when {
                            dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}, \\d{2}:\\d{2}")) -> {
                                // Format: "2025-07-23, 14:30"
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
                                dateFormat.parse(dateString)?.time ?: 0L
                            }
                            dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) -> {
                                // Format: "2025-07-23 14:30"
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                dateFormat.parse(dateString)?.time ?: 0L
                            }
                            else -> {
                                // Try default format as fallback
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
                                dateFormat.parse(dateString)?.time ?: 0L
                            }
                        }
                        
                        // Debug logging
                        android.util.Log.d("HomeViewModel", "Post: ${when (post) {
                            is FeedItem.Lost -> post.item.itemTitle
                            is FeedItem.Found -> post.item.itemTitle
                        }}, Date string: $dateString, Timestamp: $timestamp")
                        
                        timestamp
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error parsing date: $dateString", e)
                        0L // Use 0 for invalid dates (they'll appear at the end)
                    }
                }
                _allPosts.value = combined
                filterPosts()
            }
        }
    }
}