package com.ece452.watufound.ui.customForm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ece452.watufound.databinding.FragmentCustomFormBinding

class CustomFormFragment : Fragment() {

    private var _binding: FragmentCustomFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomFormViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.submitButton.setOnClickListener {
            val first = binding.firstNameInput.text.toString()
            val last = binding.lastNameInput.text.toString()
            val born = binding.birthYearInput.text.toString().toIntOrNull()

            if (first.isBlank() || last.isBlank() || born == null) {
                Toast.makeText(requireContext(), "Fill all fields properly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitPerson(first, last, born,
                onSuccess = {
                    Toast.makeText(requireContext(), "Added!", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}