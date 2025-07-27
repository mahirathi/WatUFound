package com.ece452.watufound.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentAdminUsersBinding
import com.ece452.watufound.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView

class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adminViewModel: AdminViewModel
    private lateinit var userAdapter: UserAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adminViewModel = ViewModelProvider(this).get(AdminViewModel::class.java)
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupSearch()
        loadUsers()

        return root
    }

    private fun setupRecyclerView() {
        try {
            val layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewUsers.layoutManager = layoutManager
            binding.recyclerViewUsers.setHasFixedSize(true)
            binding.recyclerViewUsers.isNestedScrollingEnabled = false
            
            userAdapter = UserAdapter(mutableListOf()) { user ->
                showUserDetailDialog(user)
            }
            binding.recyclerViewUsers.adapter = userAdapter
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error setting up RecyclerView: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        try {
            binding.searchViewUsers.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { searchUsers(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { 
                        if (it.length >= 2) {
                            searchUsers(it)
                        } else if (it.isEmpty()) {
                            loadUsers()
                        }
                    }
                    return true
                }
            })
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error setting up search: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUsers() {
        try {
            showLoading(true)
            val db = FirebaseFirestore.getInstance()
            
            db.collection("users")
                .get()
                .addOnSuccessListener { documents ->
                    try {
                                        val users = documents.mapNotNull { doc ->
                    User(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        banReason = doc.getString("banReason") ?: "",
                        bannedAt = doc.get("bannedAt")?.toString()?.toLongOrNull() ?: 0,
                        bannedBy = doc.getString("bannedBy") ?: "",
                        createdAt = doc.get("createdAt")?.toString()?.toLongOrNull() ?: doc.get("dateCreated")?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                        lastLoginAt = doc.get("lastLoginAt")?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                }
                        
                        userAdapter.updateUsers(users)
                        showLoading(false)
                        updateEmptyState(users.isEmpty())
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Error processing users: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to load users: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Error initializing user loading: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchUsers(query: String) {
        try {
            showLoading(true)
            val db = FirebaseFirestore.getInstance()
            
            db.collection("users")
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        val users = documents.mapNotNull { doc ->
                            val email = doc.getString("email") ?: ""
                            val name = doc.getString("displayName") ?: doc.getString("name") ?: ""
                            
                            if (email.contains(query, ignoreCase = true) || 
                                name.contains(query, ignoreCase = true)) {
                                User(
                                    uid = doc.id,
                                    displayName = name,
                                    email = email,
                                    isBanned = doc.getBoolean("isBanned") ?: false,
                                    banReason = doc.getString("banReason") ?: "",
                                    bannedAt = doc.get("bannedAt")?.toString()?.toLongOrNull() ?: 0,
                                    bannedBy = doc.getString("bannedBy") ?: "",
                                    createdAt = doc.get("createdAt")?.toString()?.toLongOrNull() ?: doc.get("dateCreated")?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                                    lastLoginAt = doc.get("lastLoginAt")?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                                )
                            } else null
                        }
                        
                        userAdapter.updateUsers(users)
                        showLoading(false)
                        updateEmptyState(users.isEmpty())
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Error processing search results: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to search users: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Error initializing search: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUserDetailDialog(user: User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_detail, null)
        
        // Get current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isOwnProfile = currentUser?.uid == user.uid
        
        // Set user information
        dialogView.findViewById<TextView>(R.id.dialogUserName).text = user.displayName.ifEmpty { "Unknown User" }
        dialogView.findViewById<TextView>(R.id.dialogUserEmail).text = user.email.ifEmpty { "No email" }
        dialogView.findViewById<TextView>(R.id.dialogUserId).text = user.uid
        
        val dateCreated = dateFormat.format(Date(user.createdAt))
        dialogView.findViewById<TextView>(R.id.dialogUserCreated).text = dateCreated
        
        // Set user status in the avatar section
        val userStatusText = dialogView.findViewById<TextView>(R.id.dialogUserStatus)
        val banReasonSection = dialogView.findViewById<View>(R.id.banReasonSection)
        val banReasonText = dialogView.findViewById<TextView>(R.id.dialogBanReason)
        val banButton = dialogView.findViewById<Button>(R.id.dialogBanButton)
        val unbanButton = dialogView.findViewById<Button>(R.id.dialogUnbanButton)
        val banReasonInput = dialogView.findViewById<EditText>(R.id.dialogBanReasonInput)
        val banReasonInputSection = dialogView.findViewById<View>(R.id.banReasonInputSection)
        
        // Hide ban/unban functionality if viewing own profile
        if (isOwnProfile) {
            banButton.visibility = View.GONE
            unbanButton.visibility = View.GONE
            banReasonInputSection.visibility = View.GONE
            banReasonSection.visibility = View.GONE
        } else {
            // Show ban/unban functionality for other users
            if (user.isBanned) {
                // Update status display near avatar
                userStatusText.text = "Banned"
                userStatusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                userStatusText.background = requireContext().getDrawable(R.drawable.status_banned_bg)
                
                banButton.visibility = View.GONE
                unbanButton.visibility = View.VISIBLE
                
                // Show ban reason section if available
                if (user.banReason.isNotEmpty()) {
                    banReasonText.text = user.banReason
                    banReasonSection.visibility = View.VISIBLE
                } else {
                    banReasonSection.visibility = View.GONE
                }
                
                // Hide the ban reason input section for banned users
                banReasonInputSection.visibility = View.GONE
            } else {
                // Update status display near avatar
                userStatusText.text = "Active"
                userStatusText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                userStatusText.background = requireContext().getDrawable(R.drawable.status_active_bg)
                
                banButton.visibility = View.VISIBLE
                unbanButton.visibility = View.GONE
                banReasonSection.visibility = View.GONE
                
                // Show the ban reason input section for active users
                banReasonInputSection.visibility = View.VISIBLE
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Set up button click listeners
        dialogView.findViewById<Button>(R.id.dialogCancelButton).setOnClickListener {
            dialog.dismiss()
        }
        
        // Only set up ban/unban listeners if not viewing own profile
        if (!isOwnProfile) {
            banButton.setOnClickListener {
                val reason = banReasonInput.text.toString().trim()
                if (reason.isEmpty()) {
                    banReasonInput.error = "Please enter a ban reason"
                    return@setOnClickListener
                }
                
                banUser(user.uid, reason) {
                    dialog.dismiss()
                    loadUsers() // Refresh the list
                }
            }
            
            unbanButton.setOnClickListener {
                unbanUser(user.uid) {
                    dialog.dismiss()
                    loadUsers() // Refresh the list
                }
            }
        }
        
        dialog.show()
    }

    private fun banUser(userId: String, reason: String, onComplete: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to ban users", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if current user is admin
        val isAdmin = currentUser.email?.endsWith("@admin.watufound.com") == true ||
                     currentUser.email == "admin@watufound.com"
        
        if (!isAdmin) {
            Toast.makeText(requireContext(), "Only admins can ban users", Toast.LENGTH_SHORT).show()
            return
        }
        
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        
        val banData = mapOf(
            "isBanned" to true,
            "banReason" to reason,
            "bannedAt" to System.currentTimeMillis(),
            "bannedBy" to currentUser.uid
        )
        
        userRef.update(banData)
            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "User banned successfully", Toast.LENGTH_SHORT).show()
                onComplete()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to ban user: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unbanUser(userId: String, onComplete: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to unban users", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if current user is admin
        val isAdmin = currentUser.email?.endsWith("@admin.watufound.com") == true ||
                     currentUser.email == "admin@watufound.com"
        
        if (!isAdmin) {
            Toast.makeText(requireContext(), "Only admins can unban users", Toast.LENGTH_SHORT).show()
            return
        }
        
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        
        val unbanData = mapOf(
            "isBanned" to false,
            "banReason" to "",
            "bannedAt" to 0,
            "bannedBy" to ""
        )
        
        userRef.update(unbanData)
            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "User unbanned successfully", Toast.LENGTH_SHORT).show()
                onComplete()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to unban user: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        try {
            binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            // Ignore binding errors
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        try {
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerViewUsers.visibility = if (isEmpty) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            // Ignore binding errors
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 