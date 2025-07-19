package com.ece452.watufound.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        homeViewModel.filteredPosts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
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
        // Default highlight
        highlightFilterButton(binding.buttonAll)

        return root
    }

    private fun highlightFilterButton(selected: Button) {
        val buttons = listOf(binding.buttonAll, binding.buttonLost, binding.buttonFound)
        for (btn in buttons) {
            btn.setBackgroundColor(resources.getColor(android.R.color.white, null))
            btn.setTextColor(resources.getColor(R.color.purple_700, null))
        }
        selected.setBackgroundColor(resources.getColor(R.color.purple_200, null))
        selected.setTextColor(resources.getColor(R.color.purple_700, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}