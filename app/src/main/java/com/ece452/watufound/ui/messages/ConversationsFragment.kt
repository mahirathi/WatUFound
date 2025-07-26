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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var conversationsAdapter: ConversationsAdapter
    private var conversationsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversationsAdapter = ConversationsAdapter(
            onConversationClick = { conversation ->
                val bundle = Bundle().apply {
                    putString("conversationId", conversation.conversationId)
                    putString("otherUserName", conversation.posterName)
                    putString("postTitle", conversation.postTitle)
                }
                android.util.Log.d("ConversationsFragment", "Navigating to conversation: $bundle")
                findNavController().navigate(R.id.navigation_conversation, bundle)
            },
            showDeleteButton = false
        )
        binding.recyclerViewRecent.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRecent.adapter = conversationsAdapter

        // Fetch conversations from database
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseDatabase.getInstance().reference
            conversationsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Prevent crash if view is destroyed
                    val conversations = mutableListOf<ConversationListItem>()
                    for (child in snapshot.children) {
                        val conversationId = child.child("conversationId").getValue(String::class.java) ?: continue
                        val posterName = child.child("posterName").getValue(String::class.java) ?: ""
                        val postTitle = child.child("postTitle").getValue(String::class.java) ?: ""
                        conversations.add(ConversationListItem(conversationId, posterName, postTitle))
                    }
                    conversationsAdapter.submitList(conversations)
                    binding.textNoResults.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            db.child("userConversations").child(currentUser.uid)
                .addValueEventListener(conversationsListener!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && conversationsListener != null) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("userConversations").child(currentUser.uid).removeEventListener(conversationsListener!!)
        }
        _binding = null
    }
} 