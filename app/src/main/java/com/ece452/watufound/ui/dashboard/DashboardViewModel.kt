package com.ece452.watufound.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        Log.d("DashboardViewModel", "ViewModel initialized")
        loadUserProfile()
    }

    private fun loadUserProfile() {
        Log.d("DashboardViewModel", "loadUserProfile called")
        _isLoading.value = true
        
        val currentUser = auth.currentUser
        Log.d("DashboardViewModel", "Current user: ${currentUser?.uid}")
        
        if (currentUser == null) {
            Log.d("DashboardViewModel", "No current user found")
            _userName.value = "Not logged in"
            _userEmail.value = "No email available"
            _isLoading.value = false
            return
        }

        // Get email from Firebase Auth
        val email = currentUser.email
        Log.d("DashboardViewModel", "Email from Auth: $email")
        _userEmail.value = email ?: "No email available"

        // Get display name from Firebase Auth first
        val displayName = currentUser.displayName
        Log.d("DashboardViewModel", "Display name from Auth: $displayName")
        
        if (!displayName.isNullOrEmpty()) {
            _userName.value = displayName.toString()
            _isLoading.value = false
            Log.d("DashboardViewModel", "Using display name from Auth")
        } else {
            Log.d("DashboardViewModel", "No display name in Auth, checking Firestore")
            // If no display name in Auth, try to get from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("DashboardViewModel", "Firestore document exists: ${document.exists()}")
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        Log.d("DashboardViewModel", "Name from Firestore: $name")
                        _userName.value = name ?: "No name available"
                    } else {
                        _userName.value = "No name available"
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    Log.e("DashboardViewModel", "Error loading from Firestore", exception)
                    _userName.value = "Error loading name"
                    _isLoading.value = false
                }
        }
    }
}