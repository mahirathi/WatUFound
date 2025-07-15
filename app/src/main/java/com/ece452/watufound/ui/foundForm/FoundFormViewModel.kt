package com.ece452.watufound.ui.foundForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.FoundItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FoundFormViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun submitFoundItem(item: FoundItemModel) {
        val post = hashMapOf(
            "itemTitle" to item.itemTitle,
            "description" to item.description,
            "category" to item.category,
            "foundAt" to item.foundAt,
            "dateTimeFound" to item.dateTimeFound
        )

        db.collection("found_items")
            .add(post)
            .addOnSuccessListener {
                Log.d("FoundFormVM", "Found item posted with ID: ${it.id}")


                // Increment postsCreated for the current user
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    db.collection("users").document(user.uid)
                        .update("postsCreated", com.google.firebase.firestore.FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("FoundFormVM", "postsCreated incremented for user: ${user.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FoundFormVM", "Failed to increment postsCreated", e)
                        }
                }

                
            }
            .addOnFailureListener { e ->
                Log.w("FoundFormVM", "Failed to post found item", e)
            }
    }
}
