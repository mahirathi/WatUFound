package com.ece452.watufound.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R

class UpdateProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("UpdateProfileFragment", "onCreateView called")
        
        val profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_update_profile, container, false)

        // Find views
        val updateNameEditText = root.findViewById<EditText>(R.id.updateNameEditText)
        val updateEmailEditText = root.findViewById<EditText>(R.id.updateEmailEditText)
        val passwordEditText = root.findViewById<EditText>(R.id.passwordEditText)
        val updateProfileButton = root.findViewById<Button>(R.id.updateProfileButton)
        val loadingIndicator = root.findViewById<ProgressBar>(R.id.loadingIndicator)

        // Observe loading state
        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("UpdateProfileFragment", "Loading state: $isLoading")
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            updateProfileButton?.isEnabled = !isLoading
        }

        // Observe update status
        profileViewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNotEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_LONG).show()
                
                // If update was successful, go back to profile
                if (status.contains("successfully")) {
                    findNavController().navigateUp()
                }
            }
        }

        // Set up update profile button
        updateProfileButton?.setOnClickListener {
            val newName = updateNameEditText?.text?.toString()?.trim()
            val newEmail = updateEmailEditText?.text?.toString()?.trim()
            val password = passwordEditText?.text?.toString()

            // Validate input
            if (newName.isNullOrBlank() && newEmail.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please enter at least one field to update", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!newEmail.isNullOrBlank() && password.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Password is required to update email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the ViewModel to update profile
            profileViewModel.updateProfile(newName, newEmail, password)

            // Clear the input fields after update
            updateNameEditText?.text?.clear()
            updateEmailEditText?.text?.clear()
            passwordEditText?.text?.clear()
        }

        return root
    }
} 