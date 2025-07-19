package com.ece452.watufound.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.FoundItem
import com.ece452.watufound.models.LostItem
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _allPosts = MutableLiveData<List<FeedItem>>()
    val allPosts: LiveData<List<FeedItem>> = _allPosts

    private val _filteredPosts = MutableLiveData<List<FeedItem>>()
    val filteredPosts: LiveData<List<FeedItem>> = _filteredPosts

    enum class Filter { ALL, LOST, FOUND }
    private var currentFilter = Filter.ALL

    init {
        fetchPosts()
    }

    fun setFilter(filter: Filter) {
        currentFilter = filter
        filterPosts()
    }

    private fun filterPosts() {
        val posts = _allPosts.value ?: emptyList()
        _filteredPosts.value = when (currentFilter) {
            Filter.ALL -> posts
            Filter.LOST -> posts.filterIsInstance<FeedItem.Lost>()
            Filter.FOUND -> posts.filterIsInstance<FeedItem.Found>()
        }
    }

    private fun fetchPosts() {
        val lostTask = db.collection("lost_items").get()
        val foundTask = db.collection("found_items").get()
        lostTask.addOnSuccessListener { lostResult ->
            foundTask.addOnSuccessListener { foundResult ->
                val lostList = lostResult.documents.mapNotNull { doc ->
                    doc.toObject(LostItem::class.java)?.let { FeedItem.Lost(it) }
                }
                val foundList = foundResult.documents.mapNotNull { doc ->
                    doc.toObject(FoundItem::class.java)?.let { FeedItem.Found(it) }
                }
                val combined = (lostList + foundList).sortedByDescending {
                    when (it) {
                        is FeedItem.Lost -> it.item.dateTimeLost
                        is FeedItem.Found -> it.item.dateTimeFound
                    }
                }
                _allPosts.value = combined
                filterPosts()
            }
        }
    }
}