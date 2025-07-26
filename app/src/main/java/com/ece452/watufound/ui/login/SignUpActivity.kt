package com.ece452.watufound.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ece452.watufound.MainActivity
import com.ece452.watufound.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val realtimeDb = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            val uid = firebaseUser.uid
                            Log.d("FirebaseAuth", "User created successfully with UID: $uid")

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            // Required to pull the display name and other fields on diff pages for the authenticated user
                            firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        Log.d("FirebaseAuth", "User display name updated in Auth profile.")
                                    } else {
                                        Log.w("FirebaseAuth", "Failed to update display name in Auth profile.", profileTask.exception)
                                    }
                                }

                            // Extractable to a function to keep the register function lean
                            val userProfile = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "postsCreated" to 0,
                                "itemsReturned" to 0,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            // Create a document in the 'users' collection with the UID as the document ID
                            db.collection("users").document(uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "User profile successfully written for UID: $uid")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error writing user profile for UID: $uid", e)
                                }

                            // Create user profile in Realtime Database for messaging
                            val messagingUserProfile = hashMapOf(
                                "uid" to uid,
                                "displayName" to name,
                                "email" to email
                            )

                            realtimeDb.getReference("userProfiles").child(uid)
                                .setValue(messagingUserProfile)
                                .addOnSuccessListener {
                                    Log.d("RealtimeDB", "User profile created for messaging: $uid")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("RealtimeDB", "Error creating user profile for messaging: $uid", e)
                                }
                            // End of extractable

                            // Redirect to MainActivity since user is already authenticated
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.w("FirebaseAuth", "User creation failed.", task.exception)
                    }
                }
        }

        binding.goToLoginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
