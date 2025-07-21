package com.ece452.watufound.ui.messages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentConversationBinding

class ConversationFragment : Fragment() {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding!!
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var messagesAdapter: MessagesAdapter

    companion object {
        private const val ARG_OTHER_USER_ID = "otherUserId"
        private const val ARG_OTHER_USER_NAME = "otherUserName"

        fun newInstance(otherUserId: String, otherUserName: String): ConversationFragment {
            Log.d("ConversationFragment", "Creating new instance with user: $otherUserName ($otherUserId)")
            val fragment = ConversationFragment()
            val args = Bundle()
            args.putString(ARG_OTHER_USER_ID, otherUserId)
            args.putString(ARG_OTHER_USER_NAME, otherUserName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ConversationFragment", "onCreateView called")
        try {
            conversationViewModel = ViewModelProvider(this)[ConversationViewModel::class.java]
            _binding = FragmentConversationBinding.inflate(inflater, container, false)
            Log.d("ConversationFragment", "Binding successful")
            
            return binding.root
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error in onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ConversationFragment", "onViewCreated called")
        
        try {
            val otherUserId = arguments?.getString(ARG_OTHER_USER_ID) ?: ""
            val otherUserName = arguments?.getString(ARG_OTHER_USER_NAME) ?: "Unknown"
            
            Log.d("ConversationFragment", "Conversation with: $otherUserName ($otherUserId)")
            
            // Set the conversation title
            binding.textConversationTitle.text = "Chat with $otherUserName"
            
            // Hide action bar and bottom navigation
            hideSystemUI()
            
            // Setup back button
            setupBackButton()
            
            // Show a test message in the RecyclerView
            setupRecyclerView()
            setupSendButton()
            observeMessages()
            
            // Initialize the conversation
            conversationViewModel.initializeConversation(otherUserId)
            
            Log.d("ConversationFragment", "Setup completed successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error in onViewCreated", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        Log.d("ConversationFragment", "Setting up RecyclerView")
        try {
            messagesAdapter = MessagesAdapter()
            binding.recyclerViewMessages.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                adapter = messagesAdapter
            }
            Log.d("ConversationFragment", "RecyclerView setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun hideSystemUI() {
        Log.d("ConversationFragment", "Hiding system UI")
        try {
            // Hide action bar
            (activity as? AppCompatActivity)?.supportActionBar?.hide()
            
            // Hide bottom navigation
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
            
            Log.d("ConversationFragment", "System UI hidden successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error hiding system UI", e)
        }
    }

    private fun setupBackButton() {
        Log.d("ConversationFragment", "Setting up back button")
        try {
            binding.buttonBack.setOnClickListener {
                Log.d("ConversationFragment", "Back button clicked")
                // Show system UI again before going back
                showSystemUI()
                // Use Navigation Component to go back
                findNavController().navigateUp()
            }
            Log.d("ConversationFragment", "Back button setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up back button", e)
            throw e
        }
    }

    private fun setupSendButton() {
        Log.d("ConversationFragment", "Setting up send button")
        try {
            // Make sure the input area is visible
            binding.editTextMessage.hint = "Type a message..."
            binding.editTextMessage.isEnabled = true
            binding.editTextMessage.isFocusable = true
            
            binding.buttonSend.setOnClickListener {
                val messageText = binding.editTextMessage.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    Log.d("ConversationFragment", "Sending message: $messageText")
                    conversationViewModel.sendMessage(messageText)
                    binding.editTextMessage.text.clear()
                }
            }
            
            Log.d("ConversationFragment", "Send button setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up send button", e)
            throw e
        }
    }

    private fun observeMessages() {
        Log.d("ConversationFragment", "Setting up message observer")
        try {
            conversationViewModel.messages.observe(viewLifecycleOwner) { messages ->
                Log.d("ConversationFragment", "Messages updated: ${messages.size} messages")
                messagesAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
            Log.d("ConversationFragment", "Message observer setup successful")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error setting up message observer", e)
            throw e
        }
    }

    private fun showSystemUI() {
        Log.d("ConversationFragment", "Showing system UI")
        try {
            // Show action bar
            (activity as? AppCompatActivity)?.supportActionBar?.show()
            
            // Show bottom navigation
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
            
            Log.d("ConversationFragment", "System UI shown successfully")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error showing system UI", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ConversationFragment", "onDestroyView called")
        // Show system UI when leaving conversation
        showSystemUI()
        _binding = null
    }
} 