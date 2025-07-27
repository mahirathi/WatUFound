package com.ece452.watufound.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _totalUsers = MutableLiveData<Int>()
    val totalUsers: LiveData<Int> = _totalUsers

    private val _totalPosts = MutableLiveData<Int>()
    val totalPosts: LiveData<Int> = _totalPosts

    private val _pendingReports = MutableLiveData<Int>()
    val pendingReports: LiveData<Int> = _pendingReports

    private val _activeUsers = MutableLiveData<Int>()
    val activeUsers: LiveData<Int> = _activeUsers

    private val _bannedUsers = MutableLiveData<Int>()
    val bannedUsers: LiveData<Int> = _bannedUsers

    private val _totalLostPosts = MutableLiveData<Int>()
    val totalLostPosts: LiveData<Int> = _totalLostPosts

    private val _totalFoundPosts = MutableLiveData<Int>()
    val totalFoundPosts: LiveData<Int> = _totalFoundPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshAdminStats()
    }

    fun refreshAdminStats() {
        _isLoading.value = true
        
        // Get total users count
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val totalUsers = documents.size()
                _totalUsers.value = totalUsers
                
                // Get banned users count
                val bannedUsers = documents.count { doc ->
                    doc.getBoolean("isBanned") == true
                }
                _bannedUsers.value = bannedUsers
                
                // Calculate active users (total users - banned users)
                _activeUsers.value = totalUsers - bannedUsers
            }

        // Get total posts count
        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                _totalPosts.value = documents.size()
            }

        // Get pending reports count
        db.collection("reports")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                _pendingReports.value = documents.size()
            }

        // Get total lost posts count from lost_items collection
        db.collection("lost_items")
            .get()
            .addOnSuccessListener { documents ->
                _totalLostPosts.value = documents.size()
            }

        // Get total found posts count from found_items collection
        db.collection("found_items")
            .get()
            .addOnSuccessListener { documents ->
                _totalFoundPosts.value = documents.size()
                _isLoading.value = false
            }
    }

    fun isUserAdmin(): Boolean {
        val currentUser = auth.currentUser
        return currentUser?.email?.endsWith("@admin.watufound.com") == true ||
               currentUser?.email == "admin@watufound.com"
    }

    fun getUserRole(): String {
        val currentUser = auth.currentUser
        return when {
            currentUser?.email?.endsWith("@admin.watufound.com") == true -> "ADMIN"
            currentUser?.email == "admin@watufound.com" -> "ADMIN"
            else -> "USER"
        }
    }
} 