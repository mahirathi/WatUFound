package com.ece452.watufound.ui.lostForm

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ece452.watufound.databinding.FragmentLostFormBinding
import com.ece452.watufound.models.LostItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import java.util.Calendar


class LostFormFragment : Fragment() {

    private var _binding: FragmentLostFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LostFormViewModel by viewModels()
    val sdf = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpinners()

        // Date & Time Picker implementation
        binding.dateTimeInput.setOnClickListener {
            val context = requireContext()
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val timePicker = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val formattedDate = String.format(
                                "%04d-%02d-%02d, %02d:%02d",
                                year,
                                month + 1,
                                dayOfMonth,
                                hourOfDay,
                                minute
                            )
                            binding.dateTimeInput.setText(formattedDate)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    timePicker.show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        binding.submitButton.setOnClickListener {
            val itemTitle = binding.itemTitleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val category = binding.categorySpinner.selectedItem?.toString()?.trim() ?: ""
            val lastSeenAt = binding.lastSeenAtInput.text.toString().trim()
            val dateTimeLost = binding.dateTimeInput.text.toString().trim()

            // Check for empty fields
            if (itemTitle.isBlank() || description.isBlank() || dateTimeLost.isBlank()) {
                Toast.makeText(requireContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check that category and location are not placeholders
            if (category == "Select Category") {
                Toast.makeText(requireContext(), "Please select a valid category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lastSeenAt == "Select Location") {
                Toast.makeText(requireContext(), "Please select a valid location.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that the date is not in the future
            try {
                val enteredDate: Date = sdf.parse(dateTimeLost) ?: throw IllegalArgumentException("Invalid date")
                val currentDate = Date()

                if (enteredDate.after(currentDate)) {
                    Toast.makeText(requireContext(), "Date & Time Lost cannot be in the future.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Invalid date format. Please use yyyy-MM-dd, HH:mm.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If all checks pass, proceed to submit
            viewModel.submitLostItem(
                LostItem(
                    itemTitle = itemTitle,
                    description = description,
                    category = category,
                    lastSeenAt = lastSeenAt,
                    dateTimeLost = dateTimeLost,
                    dateCreated = sdf.format(Date())
                )
            )

            Toast.makeText(requireContext(), "Posting your lost item...", Toast.LENGTH_SHORT).show()
            clearForm()
            findNavController().navigate(R.id.navigation_feed)
        }

        binding.lastSeenAtInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                (view as AutoCompleteTextView).showDropDown()
            }
        }

        binding.lastSeenAtInput.setOnClickListener {
            binding.lastSeenAtInput.showDropDown()
        }


    }

    private fun setupSpinners() {
        val categories = listOf(
            "Select Category",  // Placeholder
            "Accessories",
            "Clothing",
            "Documents",
            "Electronics",
            "Other",
            "WatCard"
        )

        val locations = listOf(
            "Select Location",  // Placeholder
            "Accelerator Centre Waterloo (ACW)",
            "Arts Lecture Hall (AL)",
            "Brubacher House (BRH)",
            "Centre for Environmental & Information Technology (EIT)",
            "Central Services Building (CSB)",
            "Dana Porter Library (DP)",
            "Davis Centre Library (DC)",
            "Douglas Wright Engineering Building (DWE)",
            "East Campus Hall (ECH)",
            "Earth Sciences & Chemistry (ESC)",
            "Engineering Building 2 (E2)",
            "Engineering Building 3 (E3)",
            "Engineering Building 5 (E5)",
            "Engineering Building 6 (E6)",
            "Engineering Building 7 (E7)",
            "Energy Research Centre (ERC)",
            "Environment Building 1",
            "Environment Building 2",
            "Environment Building 3",
            "Federation Hall (FED)",
            "General Services Complex (GSC)",
            "Hagey Hall (HH)",
            "Health Expansion Building (EXP)",
            "Health Services (HS)",
            "Math & Computer Building (MC)",
            "Modern Languages (ML)",
            "Physical Activities Complex (PAC)",
            "Quantum Nano Centre (QNC)",
            "Research Advancement Centre (RAC)",
            "Research Advancement Centre 2 (RA2)",
            "School of Optometry (OPT)",
            "South Campus Hall (SCH)",
            "Student Life Centre (SLC)",
            "Tatham Centre (TC)",
            "University Club (UC)",
            "Other"
        )

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter


        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        binding.lastSeenAtInput.setAdapter(locationAdapter)
    }

    private fun clearForm() {
        binding.itemTitleInput.text?.clear()
        binding.descriptionInput.text?.clear()
        binding.dateTimeInput.text?.clear()
        binding.categorySpinner.setSelection(0)
        binding.lastSeenAtInput.text?.clear()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
