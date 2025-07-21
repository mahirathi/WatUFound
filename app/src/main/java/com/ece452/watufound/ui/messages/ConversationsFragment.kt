package com.ece452.watufound.ui.messages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentConversationsBinding
import com.ece452.watufound.models.User

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var conversationsViewModel: ConversationsViewModel
    private lateinit var conversationsAdapter: ConversationsAdapter
    private lateinit var recentConversationsAdapter: ConversationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ConversationsFragment", "onCreateView called")
        conversationsViewModel = ViewModelProvider(this)[ConversationsViewModel::class.java]
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ConversationsFragment", "onViewCreated called")
        
        setupRecyclerView()
        setupRecentConversationsRecyclerView()
        setupSearchFunctionality()
        observeUsers()
        observeRecentConversations()
    }

    private fun setupRecyclerView() {
        Log.d("ConversationsFragment", "Setting up RecyclerView")
        conversationsAdapter = ConversationsAdapter(
            onUserClick = { user ->
                try {
                    Log.d("ConversationsFragment", "User clicked: ${user.displayName}")
                    
                    // Validate user data before navigation
                    if (user.uid.isNotEmpty() && user.displayName.isNotEmpty()) {
                        // For now, let's just show a simple dialog instead of navigation
                        // This will help us test if the issue is with navigation or the fragment itself
                        showConversationDialog(user)
                        
                    } else {
                        Log.e("ConversationsFragment", "Invalid user data: uid=${user.uid}, name=${user.displayName}")
                    }
                } catch (e: Exception) {
                    Log.e("ConversationsFragment", "Error handling user click", e)
                }
            },
            onDeleteClick = null,
            showDeleteButton = false
        )
        
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = conversationsAdapter
        }
    }

    private fun setupRecentConversationsRecyclerView() {
        Log.d("ConversationsFragment", "Setting up Recent Conversations RecyclerView")
        recentConversationsAdapter = ConversationsAdapter(
            onUserClick = { user ->
                try {
                    Log.d("ConversationsFragment", "Recent conversation clicked: ${user.displayName}")
                    
                    // Validate user data before navigation
                    if (user.uid.isNotEmpty() && user.displayName.isNotEmpty()) {
                        // Navigate to conversation
                        showConversationDialog(user)
                        
                    } else {
                        Log.e("ConversationsFragment", "Invalid user data: uid=${user.uid}, name=${user.displayName}")
                    }
                } catch (e: Exception) {
                    Log.e("ConversationsFragment", "Error handling recent conversation click", e)
                }
            },
            onDeleteClick = { user ->
                showDeleteConfirmationDialog(user)
            },
            showDeleteButton = true
        )
        
        binding.recyclerViewRecent.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentConversationsAdapter
        }
    }

    private fun showConversationDialog(user: User) {
        // Use Navigation Component to navigate to conversation
        try {
            Log.d("ConversationsFragment", "Navigating to conversation with ${user.displayName} (${user.uid})")
            
            // Clear search bar
            binding.editTextSearch.text.clear()
            
            // Navigate using Navigation Component with bundle arguments
            val bundle = Bundle().apply {
                putString("otherUserId", user.uid)
                putString("otherUserName", user.displayName)
            }
            findNavController().navigate(R.id.navigation_conversation, bundle)
            
            Log.d("ConversationsFragment", "Navigation action completed")
            
        } catch (e: Exception) {
            Log.e("ConversationsFragment", "Error navigating to conversation", e)
        }
    }

    private fun setupSearchFunctionality() {
        Log.d("ConversationsFragment", "Setting up search functionality")
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                Log.d("ConversationsFragment", "Search text changed: '$query'")
                conversationsViewModel.searchUsers(query)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.buttonSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString()
            Log.d("ConversationsFragment", "Search button clicked: '$query'")
            conversationsViewModel.searchUsers(query)
        }
        
        // Clear search when user clicks outside
        binding.root.setOnClickListener {
            binding.editTextSearch.clearFocus()
        }
    }

    private fun observeUsers() {
        Log.d("ConversationsFragment", "Setting up user observer")
        conversationsViewModel.filteredUsers.observe(viewLifecycleOwner) { users ->
            Log.d("ConversationsFragment", "Users updated: ${users.size} users")
            conversationsAdapter.submitList(users)
            
            // Show/hide RecyclerView and no results text based on search results
            if (users.isEmpty()) {
                binding.recyclerViewUsers.visibility = View.GONE
                binding.textNoResults.visibility = View.VISIBLE
                // Show recent conversations when no search results
                binding.textRecentTitle.visibility = View.VISIBLE
                binding.recyclerViewRecent.visibility = View.VISIBLE
            } else {
                binding.recyclerViewUsers.visibility = View.VISIBLE
                binding.textNoResults.visibility = View.GONE
                // Hide recent conversations when showing search results
                binding.textRecentTitle.visibility = View.GONE
                binding.recyclerViewRecent.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(user: User) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete the conversation with ${user.displayName}? This action cannot be undone and will delete all messages between you and ${user.displayName}.")
            .setPositiveButton("Delete") { _, _ ->
                Log.d("ConversationsFragment", "User confirmed deletion of conversation with ${user.displayName}")
                conversationsViewModel.deleteConversation(user.uid)
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }

    private fun observeRecentConversations() {
        Log.d("ConversationsFragment", "Setting up recent conversations observer")
        conversationsViewModel.recentConversations.observe(viewLifecycleOwner) { users ->
            Log.d("ConversationsFragment", "Recent conversations updated: ${users.size} users")
            recentConversationsAdapter.submitList(users)
            
            // Show/hide recent conversations section based on whether there are any
            if (users.isEmpty()) {
                binding.textRecentTitle.visibility = View.GONE
                binding.recyclerViewRecent.visibility = View.GONE
            } else {
                binding.textRecentTitle.visibility = View.VISIBLE
                binding.recyclerViewRecent.visibility = View.VISIBLE
            }
        }
        
        // Observe loading state
        conversationsViewModel.isLoadingRecentConversations.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Show loading state if needed
                Log.d("ConversationsFragment", "Loading recent conversations...")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear search bar when returning to this fragment
        binding.editTextSearch.text.clear()
        // Clear search results to show recent conversations
        conversationsViewModel.searchUsers("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 