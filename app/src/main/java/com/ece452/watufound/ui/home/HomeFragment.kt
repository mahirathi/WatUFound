package com.ece452.watufound.ui.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentHomeBinding
import com.ece452.watufound.utils.CategoryUtils
import java.text.SimpleDateFormat
import java.util.*

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

        // Set up search functionality
        setupSearch()

        // Set up search button
        binding.buttonSearch.setOnClickListener {
            val query = binding.searchEditText.text.toString()
            homeViewModel.setSearchQuery(query)
        }

        // Set up filter button
        binding.buttonFilter.setOnClickListener {
            showFilterDialog()
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
        
        // Refresh functionality is handled by SwipeRefreshLayout
        // No visible refresh button needed
        // Refresh button
        // binding.buttonRefresh.setOnClickListener {
            // homeViewModel.refreshPosts()
        // }
        
        // Default highlight
        highlightFilterButton(binding.buttonAll)

        return root
    }

    private fun setupSearch() {
        // Set up search on Enter key press
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString()
                homeViewModel.setSearchQuery(query)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter, null)
        
        // Get current filter data
        val currentFilterData = homeViewModel.getFilterData()
        
        // Set up date and time pickers
        val editTextStartDateTime = dialogView.findViewById<EditText>(R.id.editTextStartDateTime)
        val editTextEndDateTime = dialogView.findViewById<EditText>(R.id.editTextEndDateTime)
        
        // Set current date/time values
        if (currentFilterData.startDateTime != null) {
            editTextStartDateTime.setText(formatDateTimeForDisplay(currentFilterData.startDateTime))
            editTextStartDateTime.tag = formatDateTimeForInternal(currentFilterData.startDateTime)
        }
        if (currentFilterData.endDateTime != null) {
            editTextEndDateTime.setText(formatDateTimeForDisplay(currentFilterData.endDateTime))
            editTextEndDateTime.tag = formatDateTimeForInternal(currentFilterData.endDateTime)
        }
        
        // Set up date/time picker dialogs
        editTextStartDateTime.setOnClickListener {
            showDateTimePicker(editTextStartDateTime, currentFilterData.startDateTime) { dateTime ->
                editTextStartDateTime.setText(formatDateTimeForDisplay(dateTime))
                editTextStartDateTime.tag = formatDateTimeForInternal(dateTime)
                
                // Check if end date is now before start date
                val endDateTimeText = editTextEndDateTime.text.toString()
                if (endDateTimeText.isNotEmpty()) {
                    val endDateTime = try {
                        val internalFormat = editTextEndDateTime.tag as? String ?: endDateTimeText
                        SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault()).parse(internalFormat)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (endDateTime != null && endDateTime.before(dateTime)) {
                        Toast.makeText(requireContext(), "End date/time is now before start date/time", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        editTextEndDateTime.setOnClickListener {
            showDateTimePicker(editTextEndDateTime, currentFilterData.endDateTime) { dateTime ->
                editTextEndDateTime.setText(formatDateTimeForDisplay(dateTime))
                editTextEndDateTime.tag = formatDateTimeForInternal(dateTime)
                
                // Check if end date is before start date
                val startDateTimeText = editTextStartDateTime.text.toString()
                if (startDateTimeText.isNotEmpty()) {
                    val startDateTime = try {
                        val internalFormat = editTextStartDateTime.tag as? String ?: startDateTimeText
                        SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault()).parse(internalFormat)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (startDateTime != null && dateTime.before(startDateTime)) {
                        Toast.makeText(requireContext(), "End date/time cannot be before start date/time", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Set up dynamic category checkboxes
        val categoryContainer = dialogView.findViewById<LinearLayout>(R.id.categoryCheckboxesContainer)
        val categoryCheckboxes = mutableMapOf<String, CheckBox>()
        
        CategoryUtils.getSelectableCategories().forEach { category ->
            val checkBox = CheckBox(requireContext()).apply {
                text = category
                textSize = 16f
                setPadding(32, 16, 32, 16)
                setTextColor(resources.getColor(R.color.black, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
                buttonDrawable = resources.getDrawable(R.drawable.checkbox_purple_selector, null)
                isChecked = currentFilterData.selectedCategories.contains(category)
                
                // Add listener to update Select All button text when checkbox state changes
                setOnCheckedChangeListener { _, _ ->
                    val allChecked = categoryCheckboxes.values.all { it.isChecked }
                    val buttonSelectAll = dialogView.findViewById<Button>(R.id.buttonSelectAllCategories)
                    buttonSelectAll.text = if (allChecked) "Deselect All Categories" else "Select All Categories"
                }
            }
            categoryCheckboxes[category] = checkBox
            categoryContainer.addView(checkBox)
        }
        
        // Set up Select All button
        val buttonSelectAll = dialogView.findViewById<Button>(R.id.buttonSelectAllCategories)
        buttonSelectAll.setOnClickListener {
            val allChecked = categoryCheckboxes.values.all { it.isChecked }
            categoryCheckboxes.values.forEach { checkBox ->
                checkBox.isChecked = !allChecked
            }
            buttonSelectAll.text = if (allChecked) "Select All Categories" else "Deselect All Categories"
        }
        
        // Update Select All button text based on current state
        val allChecked = categoryCheckboxes.values.all { it.isChecked }
        buttonSelectAll.text = if (allChecked) "Deselect All Categories" else "Select All Categories"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set up close button
        dialogView.findViewById<ImageButton>(R.id.buttonClose).setOnClickListener {
            dialog.dismiss()
        }

        // Set up button listeners
        dialogView.findViewById<Button>(R.id.buttonClearFilters).setOnClickListener {
            // Clear date/time fields
            editTextStartDateTime.setText("")
            editTextStartDateTime.tag = null
            editTextEndDateTime.setText("")
            editTextEndDateTime.tag = null
            
            // Uncheck all categories
            categoryCheckboxes.values.forEach { checkBox ->
                checkBox.isChecked = false
            }
            
            // Update Select All button
            buttonSelectAll.text = "Select All Categories"
            
            // Actually clear the filters in the ViewModel
            homeViewModel.clearAllFilters()
            
           
        }

        dialogView.findViewById<Button>(R.id.buttonApplyFilters).setOnClickListener {
            // Parse date/time range
            val startDateTime = try {
                if (editTextStartDateTime.text.isNotEmpty()) {
                    val internalFormat = editTextStartDateTime.tag as? String ?: editTextStartDateTime.text.toString()
                    SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault()).parse(internalFormat)
                } else null
            } catch (e: Exception) {
                null
            }
            
            val endDateTime = try {
                if (editTextEndDateTime.text.isNotEmpty()) {
                    val internalFormat = editTextEndDateTime.tag as? String ?: editTextEndDateTime.text.toString()
                    SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault()).parse(internalFormat)
                } else null
            } catch (e: Exception) {
                null
            }
            
            // Validate date/time range
            if (startDateTime != null && endDateTime != null && endDateTime.before(startDateTime)) {
                Toast.makeText(requireContext(), "End date/time must be after start date/time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Get selected categories
            val selectedCategories = categoryCheckboxes.entries
                .filter { it.value.isChecked }
                .map { it.key }
                .toSet()
            
            // Apply filters
            homeViewModel.setDateTimeRange(startDateTime, endDateTime)
            homeViewModel.setSelectedCategories(selectedCategories)
            
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun showDateTimePicker(editText: EditText, currentDate: Date?, onDateTimeSelected: (Date) -> Unit) {
        val context = requireContext()
        val calendar = Calendar.getInstance()
        currentDate?.let { calendar.time = it }

        val datePicker = DatePickerDialog(
            context,
            R.style.PurpleDatePickerDialog,
            { _, year, month, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    context,
                    R.style.PurpleTimePickerDialog,
                    { _, hourOfDay, minute ->
                        // Convert to 12-hour format with AM/PM
                        val amPm = if (hourOfDay < 12) "AM" else "PM"
                        val hour12 = when (hourOfDay) {
                            0 -> 12
                            in 13..23 -> hourOfDay - 12
                            else -> hourOfDay
                        }
                        
                        // Format for display (readable format)
                        val displayFormat = String.format(
                            "%s %d, %d at %d:%02d %s",
                            getMonthName(month + 1),
                            dayOfMonth,
                            year,
                            hour12,
                            minute,
                            amPm
                        )
                        
                        // Store in internal format for database
                        val internalFormat = String.format(
                            "%04d-%02d-%02d, %02d:%02d",
                            year,
                            month + 1,
                            dayOfMonth,
                            hourOfDay,
                            minute
                        )
                        
                        // Display readable format, store internal format in tag
                        editText.setText(displayFormat)
                        editText.tag = internalFormat
                        
                        // Create Date object for callback
                        val selectedDateTime = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault()).parse(internalFormat)
                        selectedDateTime?.let { onDateTimeSelected(it) }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // Use 12-hour format with AM/PM
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    
    private fun formatDateTimeForDisplay(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when (hour) {
            0 -> 12
            in 13..23 -> hour - 12
            else -> hour
        }
        
        return String.format(
            "%s %d, %d at %d:%02d %s",
            getMonthName(month),
            day,
            year,
            hour12,
            minute,
            amPm
        )
    }
    
    private fun formatDateTimeForInternal(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        return String.format(
            "%04d-%02d-%02d, %02d:%02d",
            year,
            month,
            day,
            hour,
            minute
        )
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the feed when returning to this fragment (e.g., after creating a post)
        homeViewModel.refreshPosts()
    }

    private fun highlightFilterButton(selected: android.widget.TextView) {
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