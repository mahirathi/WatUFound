package com.ece452.watufound.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentPostContainerBinding
import com.ece452.watufound.ui.foundForm.FoundFormFragment
import com.ece452.watufound.ui.lostForm.LostFormFragment

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

        // Set up button click listeners
        binding.buttonLostItem.setOnClickListener {
            highlightTabButton(binding.buttonLostItem)
            childFragmentManager.commit {
                replace(binding.fragmentContainer.id, LostFormFragment())
            }
        }

        binding.buttonFoundItem.setOnClickListener {
            highlightTabButton(binding.buttonFoundItem)
            childFragmentManager.commit {
                replace(binding.fragmentContainer.id, FoundFormFragment())
            }
        }
        
        // Default highlight
        highlightTabButton(binding.buttonLostItem)
    }

    private fun highlightTabButton(selected: TextView) {
        val buttons = listOf(binding.buttonLostItem, binding.buttonFoundItem)
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