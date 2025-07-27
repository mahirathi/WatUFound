package com.ece452.watufound.ui.lostForm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ece452.watufound.models.LostItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class LostFormViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance()

    fun submitLostItem(item: LostItem, imageUri: Uri? = null, onBanned: () -> Unit = {}) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("LostFormVM", "No authenticated user")
            return
        }
        
        // Check if user is banned before allowing post
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isBanned = document.getBoolean("isBanned") ?: false
                    if (isBanned) {
                        Log.w("LostFormVM", "User is banned from posting")
                        onBanned()
                        return@addOnSuccessListener
                    }
                }
                
                // User is not banned, proceed with posting
                val post = hashMapOf(
                    "itemTitle" to item.itemTitle,
                    "description" to item.description,
                    "category" to item.category,
                    "lastSeenAt" to item.lastSeenAt,
                    "roomNumber" to item.roomNumber,
                    "dateTimeLost" to item.dateTimeLost,
                    "dateCreated" to item.dateCreated,
                    "isAnonymous" to item.isAnonymous,
                    "userId" to (currentUser.uid),
                    "userName" to (currentUser.displayName ?: currentUser.email ?: "Unknown")
                )

                if (imageUri != null) {
                    // Upload image to Firebase Storage
                    val imageRef = storage.reference.child("gs://ece452-watufound.firebasestorage.app")
                    imageRef.putFile(imageUri)
                        .addOnSuccessListener { taskSnapshot ->
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                post["imageUrl"] = uri.toString()
                                saveLostItem(post)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("LostFormVM", "Image upload failed", e)
                            // Save post without image if upload fails
                            saveLostItem(post)
                        }
                } else {
                    saveLostItem(post)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LostFormVM", "Failed to check user ban status", exception)
            }
    }

    private fun saveLostItem(post: Any) {
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