package com.ece452.watufound.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ece452.watufound.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DashboardFragment", "onCreateView called")
        
        val dashboardViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_profile, container, false)
        Log.d("DashboardFragment", "Layout inflated successfully")

        // Find views using findViewById
        val userNameText = root.findViewById<TextView>(R.id.userNameText)
        val userEmailText = root.findViewById<TextView>(R.id.userEmailText)
        val loadingIndicator = root.findViewById<ProgressBar>(R.id.loadingIndicator)

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

            // Observe loading state
            dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d("DashboardFragment", "Loading state: $isLoading")
                loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        } else {
            Log.e("DashboardFragment", "Failed to find views")
            Toast.makeText(requireContext(), "Failed to load profile view", Toast.LENGTH_LONG).show()
        }

        return root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("DashboardFragment", "onViewCreated called")
//    }
}