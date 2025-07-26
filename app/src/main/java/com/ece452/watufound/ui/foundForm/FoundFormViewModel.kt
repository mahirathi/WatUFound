package com.ece452.watufound.ui.foundForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.FoundItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class FoundFormViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance()

    fun submitFoundItem(item: FoundItem, imageUri: Uri? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val post = hashMapOf(
            "itemTitle" to item.itemTitle,
            "description" to item.description,
            "category" to item.category,
            "foundAt" to item.foundAt,
            "roomNumber" to item.roomNumber,
            "dateTimeFound" to item.dateTimeFound,
            "dateCreated" to item.dateCreated,
            "isAnonymous" to item.isAnonymous,
            "userId" to (currentUser?.uid ?: ""),
            "userName" to (currentUser?.displayName ?: currentUser?.email ?: "Unknown")
        )

        if (imageUri != null) {
            // Upload image to Firebase Storage
            val imageRef = storage.reference.child("gs://ece452-watufound.firebasestorage.app")
            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        post["imageUrl"] = uri.toString()
                        saveFoundItem(post)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FoundFormVM", "Image upload failed", e)
                    // Save post without image if upload fails
                    saveFoundItem(post)
                }
        } else {
            saveFoundItem(post)
        }
    }

    private fun saveFoundItem(post: Any) {
        db.collection("found_items")
            .add(post)
            .addOnSuccessListener {
                Log.d("FoundFormVM", "Found item posted with ID: ${it.id}")

                val user = FirebaseAuth.getInstance().currentUser
                user?.let { currentUser ->
                    val userRef = db.collection("users").document(currentUser.uid)

                    // Increment postsCreated
                    userRef.update("postsCreated", com.google.firebase.firestore.FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("FoundFormVM", "postsCreated incremented for user: ${currentUser.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FoundFormVM", "Failed to increment postsCreated", e)
                        }

                    // Add the post ID to the user's foundPosts array
                    userRef.update("foundPosts", com.google.firebase.firestore.FieldValue.arrayUnion(it.id))
                        .addOnSuccessListener {
                            Log.d("FoundFormVM", "Post ID added to foundPosts array for user: ${currentUser.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FoundFormVM", "Failed to add post ID to foundPosts array", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FoundFormVM", "Failed to post found item", e)
            }
    }
}
