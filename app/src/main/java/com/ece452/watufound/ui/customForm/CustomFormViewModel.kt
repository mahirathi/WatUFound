package com.ece452.watufound.ui.customForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CustomFormViewModel : ViewModel() {
    private val db = Firebase.firestore

    fun submitPerson(first: String, last: String, born: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val person = hashMapOf(
            "first" to first,
            "last" to last,
            "born" to born
        )

        db.collection("users")
            .add(person)
            .addOnSuccessListener {
                Log.d("CustomFormVM", "User added: ${it.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w("CustomFormVM", "Failed to add user", e)
                onFailure(e)
            }
    }
}