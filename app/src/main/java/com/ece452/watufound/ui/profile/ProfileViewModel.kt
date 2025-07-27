package com.ece452.watufound.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {

    private val _userName = MutableLiveData<String?>()
    val userName: MutableLiveData<String?> = _userName

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: MutableLiveData<String?> = _userEmail

    private val _postsCreated = MutableLiveData<Int>()
    val postsCreated: LiveData<Int> = _postsCreated

    private val _itemsReturned = MutableLiveData<Int>()
    val itemsReturned: LiveData<Int> = _itemsReturned

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateStatus = MutableLiveData<String>()
    val updateStatus: LiveData<String> = _updateStatus

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        Log.d("ProfileViewModel", "ViewModel initialized")
        loadUserProfile()
    }

    private fun loadUserProfile() {
        Log.d("ProfileViewModel", "loadUserProfile called")
        _isLoading.value = true
        
        val currentUser = auth.currentUser
        Log.d("ProfileViewModel", "Current user: ${currentUser?.uid}")
        
        if (currentUser == null) {
            Log.d("ProfileViewModel", "No current user found")
            _userName.value = "Not logged in"
            _userEmail.value = "No email available"
            _postsCreated.value = 0
            _itemsReturned.value = 0
            _isLoading.value = false
            return
        }

        // Get email from Firebase Auth
        val email = currentUser.email
        Log.d("ProfileViewModel", "Email from Auth: $email")
        _userEmail.value = email ?: "No email available"

        // Get display name from Firebase Auth first
        val displayName = currentUser.displayName
        Log.d("ProfileViewModel", "Display name from Auth: $displayName")
        
        if (!displayName.isNullOrEmpty()) {
            _userName.value = displayName.toString()
            // Still need to load statistics from Firestore
            loadUserStatistics(currentUser.uid)
        } else {
            Log.d("ProfileViewModel", "No display name in Auth, checking Firestore")
            // If no display name in Auth, try to get from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("ProfileViewModel", "Firestore document exists: ${document.exists()}")
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        Log.d("ProfileViewModel", "Name from Firestore: $name")
                        _userName.value = name ?: "No name available"
                        
                        // Load statistics from the same document
                        loadUserStatisticsFromDocument(document)
                    } else {
                        _userName.value = "No name available"
                        _postsCreated.value = 0
                        _itemsReturned.value = 0
                        _isLoading.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileViewModel", "Error loading from Firestore", exception)
                    _userName.value = "Error loading name"
                    _postsCreated.value = 0
                    _itemsReturned.value = 0
                    _isLoading.value = false
                }
        }
    }

    private fun loadUserStatistics(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    loadUserStatisticsFromDocument(document)
                } else {
                    _postsCreated.value = 0
                    _itemsReturned.value = 0
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileViewModel", "Error loading user statistics", exception)
                _postsCreated.value = 0
                _itemsReturned.value = 0
                _isLoading.value = false
            }
    }

    private fun loadUserStatisticsFromDocument(document: com.google.firebase.firestore.DocumentSnapshot) {
        val postsCreated = document.getLong("postsCreated")?.toInt() ?: 0
        val itemsReturned = document.getLong("itemsReturned")?.toInt() ?: 0
        
        Log.d("ProfileViewModel", "Posts created: $postsCreated, Items returned: $itemsReturned")
        
        _postsCreated.value = postsCreated
        _itemsReturned.value = itemsReturned
        _isLoading.value = false
    }

    fun updateProfile(newName: String?, newEmail: String?, password: String?) {
        _isLoading.value = true
        _updateStatus.value = "Updating profile..."
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _updateStatus.value = "Error: No user logged in"
            _isLoading.value = false
            return
        }

        var updateCount = 0
        var totalUpdates = 0

        // Update display name if provided
        if (!newName.isNullOrBlank()) {
            totalUpdates++
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ProfileViewModel", "Display name updated successfully")
                        _userName.value = newName
                        
                        // Also update in Firestore
                        db.collection("users").document(currentUser.uid)
                            .update("name", newName)
                            .addOnSuccessListener {
                                Log.d("ProfileViewModel", "Name updated in Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileViewModel", "Failed to update name in Firestore", e)
                            }
                    } else {
                        Log.e("ProfileViewModel", "Failed to update display name", task.exception)
                        _updateStatus.value = "Failed to update name: ${task.exception?.message}"
                    }
                    updateCount++
                    checkUpdateCompletion(totalUpdates, updateCount)
                }
        }

        // Update email if provided
        if (!newEmail.isNullOrBlank() && !password.isNullOrBlank()) {
            totalUpdates++
            // First re-authenticate the user
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(currentUser.email!!, password)

            currentUser.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        // Now update the email using the newer API
                        val emailUpdateTask = currentUser.updateEmail(newEmail)
                        emailUpdateTask.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                Log.d("ProfileViewModel", "Email updated successfully")
                                _userEmail.value = newEmail
                                
                                // Also update in Firestore
                                db.collection("users").document(currentUser.uid)
                                    .update("email", newEmail)
                                    .addOnSuccessListener {
                                        Log.d("ProfileViewModel", "Email updated in Firestore")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ProfileViewModel", "Failed to update email in Firestore", e)
                                    }
                            } else {
                                Log.e("ProfileViewModel", "Failed to update email", emailTask.exception)
                                _updateStatus.value = "Failed to update email: ${emailTask.exception?.message}"
                            }
                            updateCount++
                            checkUpdateCompletion(totalUpdates, updateCount)
                        }
                    } else {
                        Log.e("ProfileViewModel", "Re-authentication failed", reauthTask.exception)
                        _updateStatus.value = "Authentication failed: ${reauthTask.exception?.message}"
                        updateCount++
                        checkUpdateCompletion(totalUpdates, updateCount)
                    }
                }
        } else if (!newEmail.isNullOrBlank() && password.isNullOrBlank()) {
            _updateStatus.value = "Password required to update email"
            _isLoading.value = false
            return
        }

        // If no updates were requested
        if (totalUpdates == 0) {
            _updateStatus.value = "No changes to update"
            _isLoading.value = false
        }
    }

    private fun checkUpdateCompletion(totalUpdates: Int, completedUpdates: Int) {
        if (completedUpdates >= totalUpdates) {
            _isLoading.value = false
            if (completedUpdates > 0) {
                _updateStatus.value = "Profile updated successfully!"
            }
        }
    }
}