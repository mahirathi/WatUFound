package com.ece452.watufound.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import com.ece452.watufound.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DashboardFragment", "onCreateView called")
        
        val dashboardViewModel =
            ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_profile, container, false)
        Log.d("DashboardFragment", "Layout inflated successfully")

        // Find views using findViewById
        val userNameText = root.findViewById<TextView>(R.id.userNameText)
        val userEmailText = root.findViewById<TextView>(R.id.userEmailText)
        val loadingIndicator = root.findViewById<ProgressBar>(R.id.loadingIndicator)
        val signOutButton = root.findViewById<Button>(R.id.signOutButton)
        val editProfileOption = root.findViewById<LinearLayout>(R.id.editProfileOption)

        val postsCountText = root.findViewById<TextView>(R.id.postsCountText)
        val itemsReturnedText = root.findViewById<TextView>(R.id.itemsReturnedText)

        if (userNameText != null && userEmailText != null) {
            Log.d("DashboardFragment", "Views found successfully")
            
            // Set initial text
            userNameText.text = "Initializing..."
            userEmailText.text = "Initializing..."
            
            // Observe user name
            dashboardViewModel.userName.observe(viewLifecycleOwner) { name ->
                Log.d("DashboardFragment", "UserName updated: $name")
                userNameText.text = name
            }

            // Observe user email
            dashboardViewModel.userEmail.observe(viewLifecycleOwner) { email ->
                Log.d("DashboardFragment", "UserEmail updated: $email")
                userEmailText.text = email
            }

            // Observe posts created count
            dashboardViewModel.postsCreated.observe(viewLifecycleOwner) { count ->
                Log.d("DashboardFragment", "Posts created updated: $count")
                postsCountText?.text = count.toString()
            }

            // Observe items returned count
            dashboardViewModel.itemsReturned.observe(viewLifecycleOwner) { count ->
                Log.d("DashboardFragment", "Items returned updated: $count")
                itemsReturnedText?.text = count.toString()
            }

            // Observe loading state
            dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d("DashboardFragment", "Loading state: $isLoading")
                loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        } else {
            Log.e("DashboardFragment", "Failed to find views")
            Toast.makeText(requireContext(), "Failed to load profile view", Toast.LENGTH_LONG).show()
        }

        // Set up edit profile option to navigate to update screen
        editProfileOption?.setOnClickListener {
            findNavController().navigate(R.id.navigation_update_profile)
        }

        // Set up sign out button
        signOutButton?.setOnClickListener {
            signOut()
        }

        // Check if user is admin and show admin access
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isAdmin = currentUser?.email?.endsWith("@admin.watufound.com") == true ||
                     currentUser?.email == "admin@watufound.com"
        
        if (isAdmin) {
            // Show the new prominent admin dashboard button
            val adminDashboardButton = root.findViewById<LinearLayout>(R.id.adminDashboardButton)
            adminDashboardButton?.visibility = View.VISIBLE
            adminDashboardButton?.setOnClickListener {
                findNavController().navigate(R.id.navigation_admin_dashboard)
            }
            
            // Hide the old admin access option
            val adminAccessOption = root.findViewById<LinearLayout>(R.id.adminAccessOption)
            adminAccessOption?.visibility = View.GONE
        }

        return root
    }

    private fun signOut() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        
        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate to login activity and clear the activity stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("DashboardFragment", "onViewCreated called")
//    }
}