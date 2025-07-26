package com.ece452.watufound.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        adapter = PostAdapter(emptyList())
        binding.recyclerViewPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPosts.adapter = adapter

        // Set up SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.refreshPosts()
        }

        homeViewModel.filteredPosts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
            // Stop the refresh animation when data is loaded
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.buttonAll.setOnClickListener {
            homeViewModel.setFilter(HomeViewModel.Filter.ALL)
            highlightFilterButton(binding.buttonAll)
        }
        binding.buttonLost.setOnClickListener {
            homeViewModel.setFilter(HomeViewModel.Filter.LOST)
            highlightFilterButton(binding.buttonLost)
        }
        binding.buttonFound.setOnClickListener {
            homeViewModel.setFilter(HomeViewModel.Filter.FOUND)
            highlightFilterButton(binding.buttonFound)
        }
        
        // Refresh button
        binding.buttonRefresh.setOnClickListener {
            homeViewModel.refreshPosts()
        }
        
        // Default highlight
        highlightFilterButton(binding.buttonAll)

        return root
    }

    override fun onResume() {
        super.onResume()
        // Refresh the feed when returning to this fragment (e.g., after creating a post)
        homeViewModel.refreshPosts()
    }

    private fun highlightFilterButton(selected: TextView) {
        val buttons = listOf(binding.buttonAll, binding.buttonLost, binding.buttonFound)
        for (btn in buttons) {
            btn.isSelected = false
            btn.setBackgroundResource(R.drawable.tab_button_background)
            btn.setTextColor(resources.getColor(R.color.tab_text_unselected, null))
        }
        selected.isSelected = true
        selected.setBackgroundResource(R.drawable.tab_button_selected_background)
        selected.setTextColor(resources.getColor(R.color.tab_text_selected, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}