package com.ece452.watufound.ui.lostForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.LostItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LostFormViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun submitLostItem(item: LostItemModel) {
        val post = hashMapOf(
            "itemTitle" to item.itemTitle,
            "description" to item.description,
            "category" to item.category,
            "lastSeenAt" to item.lastSeenAt,
            "dateTimeLost" to item.dateTimeLost
        )

        db.collection("lost_items")
            .add(post)
            .addOnSuccessListener {
                Log.d("LostFormVM", "Item posted successfully with ID: ${it.id}")
               
               
                // Increment postsCreated for the current user
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    db.collection("users").document(user.uid)
                        .update("postsCreated", com.google.firebase.firestore.FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("LostFormVM", "postsCreated incremented for user: ${user.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("LostFormVM", "Failed to increment postsCreated", e)
                        }
                }


            }
            .addOnFailureListener { e ->
                Log.w("LostFormVM", "Failed to post item", e)
            }
    }
}