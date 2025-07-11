package com.ece452.watufound.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ece452.watufound.databinding.FragmentPostContainerBinding
import com.ece452.watufound.ui.foundForm.FoundFormFragment
import com.ece452.watufound.ui.lostForm.LostFormFragment
import com.google.android.material.tabs.TabLayout

class PostContainerFragment : Fragment() {

    private var _binding: FragmentPostContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initially show LostFormFragment
        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(binding.fragmentContainer.id, LostFormFragment())
            }
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Lost Item"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Found Item"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedFragment = when (tab?.position) {
                    0 -> LostFormFragment()
                    1 -> FoundFormFragment()
                    else -> LostFormFragment()
                }
                childFragmentManager.commit {
                    replace(binding.fragmentContainer.id, selectedFragment)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}