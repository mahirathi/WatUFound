package com.ece452.watufound.ui.foundForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.FoundItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FoundFormViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun submitFoundItem(item: FoundItem) {
        val post = hashMapOf(
            "itemTitle" to item.itemTitle,
            "description" to item.description,
            "category" to item.category,
            "foundAt" to item.foundAt,
            "dateTimeFound" to item.dateTimeFound,
            "dateCreated" to item.dateCreated
        )

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
