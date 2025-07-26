package com.ece452.watufound.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adminViewModel: AdminViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adminViewModel = ViewModelProvider(this).get(AdminViewModel::class.java)
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupAdminStats()
        setupAdminActions()

        return root
    }

    private fun setupAdminStats() {
        // Observe admin statistics
        adminViewModel.activeUsers.observe(viewLifecycleOwner) { count ->
            binding.textActiveUsers.text = count.toString()
        }

        adminViewModel.bannedUsers.observe(viewLifecycleOwner) { count ->
            binding.textBannedUsers.text = count.toString()
        }

        adminViewModel.totalLostPosts.observe(viewLifecycleOwner) { count ->
            binding.textTotalLostPosts.text = count.toString()
        }

        adminViewModel.totalFoundPosts.observe(viewLifecycleOwner) { count ->
            binding.textTotalFoundPosts.text = count.toString()
        }
    }

    private fun setupAdminActions() {
        // User Management - Only implemented action
        binding.buttonUserManagement.setOnClickListener {
            findNavController().navigate(R.id.navigation_admin_users)
        }

        // Note: Post Management, Reports Management, and Analytics buttons are disabled
        // and don't have click listeners since they're not implemented yet
    }

    override fun onResume() {
        super.onResume()
        adminViewModel.refreshAdminStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 