package com.ece452.watufound.ui.lostForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.LostItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LostFormViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun submitLostItem(item: LostItem) {
        val post = hashMapOf(
            "itemTitle" to item.itemTitle,
            "description" to item.description,
            "category" to item.category,
            "lastSeenAt" to item.lastSeenAt,
            "dateTimeLost" to item.dateTimeLost,
            "dateCreated" to item.dateCreated
        )

        db.collection("lost_items")
            .add(post)
            .addOnSuccessListener {
                Log.d("LostFormVM", "Item posted successfully with ID: ${it.id}")

                val user = FirebaseAuth.getInstance().currentUser
                user?.let { currentUser ->
                    val userRef = db.collection("users").document(currentUser.uid)

                    // Increment postsCreated
                    userRef.update("postsCreated", com.google.firebase.firestore.FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("LostFormVM", "postsCreated incremented for user: ${currentUser.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("LostFormVM", "Failed to increment postsCreated", e)
                        }

                    // Add the post ID to the user's lostPosts array
                    userRef.update("lostPosts", com.google.firebase.firestore.FieldValue.arrayUnion(it.id))
                        .addOnSuccessListener {
                            Log.d("LostFormVM", "Post ID added to lostPosts array for user: ${currentUser.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("LostFormVM", "Failed to add post ID to lostPosts array", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("LostFormVM", "Failed to post item", e)
            }
    }
}