package com.ece452.watufound.ui.lostForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.LostItemModel
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
            }
            .addOnFailureListener { e ->
                Log.w("LostFormVM", "Failed to post item", e)
            }
    }
}