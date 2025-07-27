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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ece452.watufound.utils.BuildingLocationUtils
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


class LostFormFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLostFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LostFormViewModel by viewModels()
    val sdf = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var lastPin: LatLng? = null
    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                _binding?.imagePreview?.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostFormBinding.inflate(inflater, container, false)
        mapView = _binding?.root?.findViewById(R.id.mapViewPreview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpinners()
        checkUserBanStatus()

        // Date & Time Picker implementation
        binding.dateTimeInput.setOnClickListener {
            val context = requireContext()
            val calendar = Calendar.getInstance()

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
                            binding.dateTimeInput.setText(displayFormat)
                            binding.dateTimeInput.tag = internalFormat
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

        binding.buttonSelectImage.setOnClickListener {
            val selectedCategory = binding.categorySpinner.selectedItem.toString()
            if (listOf("WatCard").contains(selectedCategory)) {
                Toast.makeText(requireContext(), "Image upload is disabled for $selectedCategory for security reasons..", Toast.LENGTH_SHORT).show()
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }

        binding.submitButton.setOnClickListener {
            val itemTitle = binding.itemTitleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val category = binding.categorySpinner.selectedItem?.toString()?.trim() ?: ""
            val lastSeenAt = binding.lastSeenAtInput.text.toString().trim()
            val roomNumber = binding.roomNumberInput.text.toString().trim()
            val dateTimeLost = binding.dateTimeInput.text.toString().trim()
            val isAnonymous = binding.switchAnonymous.isChecked

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
                // Use internal format stored in tag, fallback to text if tag is null
                val dateTimeForValidation = binding.dateTimeInput.tag as? String ?: binding.dateTimeInput.text.toString()
                val enteredDate: Date = sdf.parse(dateTimeForValidation) ?: throw IllegalArgumentException("Invalid date")
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
            val dateTimeForSubmission = binding.dateTimeInput.tag as? String ?: binding.dateTimeInput.text.toString()
            viewModel.submitLostItem(
                LostItem(
                    itemTitle = itemTitle,
                    description = description,
                    category = category,
                    lastSeenAt = lastSeenAt,
                    roomNumber = roomNumber,
                    dateTimeLost = dateTimeForSubmission,
                    dateCreated = sdf.format(Date()),
                    isAnonymous = isAnonymous
                ),
                selectedImageUri, // Pass the image URI
                onBanned = {
                    // Handle banned user
//                    Toast.makeText(requireContext(), "You are banned from posting. Please contact an administrator.", Toast.LENGTH_LONG).show()
                }
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

        binding.lastSeenAtInput.setOnItemClickListener { _, _, position, _ ->
            val location = binding.lastSeenAtInput.adapter.getItem(position) as? String
            updateMapForLocation(location)
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
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        val restrictedCategories = listOf("WatCard")

        binding.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                if (restrictedCategories.contains(selectedCategory)) {
                    binding.buttonSelectImage.isEnabled = false
                    selectedImageUri = null
                    binding.imagePreview.setImageResource(R.drawable.ic_image_upload)
                    Toast.makeText(requireContext(), "Image upload disabled for $selectedCategory due to security reasons.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.buttonSelectImage.isEnabled = true
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

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
            "Engineering 2 (E2)",
            "Engineering 3 (E3)",
            "Engineering 5 (E5)",
            "Engineering 6 (E6)",
            "Engineering 7 (E7)",
            "Energy Research Centre (ERC)",
            "Environment 1 (EV1)",
            "Environment 2 (EV2)",
            "Environment 3 (EV3)",
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

        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        binding.lastSeenAtInput.setAdapter(locationAdapter)
    }


    private fun updateMapForLocation(location: String?) {
        if (location.isNullOrBlank() || location == "Select Location" || location == "Other") {
            googleMap?.clear()
            binding.mapViewPreview.visibility = View.GONE
            return
        }
        
        val latLng = BuildingLocationUtils.getBuildingLatLng(requireContext(), location)
        if (latLng != null) {
            // Show the map
            binding.mapViewPreview.visibility = View.VISIBLE
            
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title(location))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            lastPin = latLng
        } else {
            // Hide the map if coordinates not found
            binding.mapViewPreview.visibility = View.GONE
            googleMap?.clear()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Enable zoom controls
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        // Apply purple theme styling
        val purpleStyle = """
            [
                {
                    "featureType": "all",
                    "elementType": "geometry.fill",
                    "stylers": [
                        {
                            "color": "#f3e5f5"
                        }
                    ]
                },
                {
                    "featureType": "water",
                    "elementType": "geometry",
                    "stylers": [
                        {
                            "color": "#e1bee7"
                        }
                    ]
                },
                {
                    "featureType": "road",
                    "elementType": "geometry",
                    "stylers": [
                        {
                            "color": "#f8f0ff"
                        }
                    ]
                }
            ]
        """.trimIndent()
        
        try {
            val mapStyleOptions = com.google.android.gms.maps.model.MapStyleOptions(purpleStyle)
            googleMap?.setMapStyle(mapStyleOptions)
        } catch (e: Exception) {
            android.util.Log.e("LostFormFragment", "Map styling failed", e)
        }
        
        // If a location was already selected, show it
        val location = binding.lastSeenAtInput.text.toString()
        updateMapForLocation(location)
    }

    // MapView lifecycle
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onDestroyView() { super.onDestroyView(); mapView?.onDestroy(); _binding = null }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }

    private fun checkUserBanStatus() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isBanned = document.getBoolean("isBanned") ?: false
                    if (isBanned) {
                        // User is banned - disable form and show ban message
                        disableFormForBannedUser()
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("LostFormFragment", "Failed to check ban status", exception)
            }
    }

    private fun disableFormForBannedUser() {
        // Disable all form inputs
        binding.itemTitleInput.isEnabled = false
        binding.descriptionInput.isEnabled = false
        binding.categorySpinner.isEnabled = false
        binding.lastSeenAtInput.isEnabled = false
        binding.roomNumberInput.text?.clear()
        binding.roomNumberInput.isEnabled = false
        binding.dateTimeInput.isEnabled = false
        binding.buttonSelectImage.isEnabled = false
        binding.switchAnonymous.isEnabled = false
        
        // Disable submit button and change its appearance
        binding.submitButton.isEnabled = false
        binding.submitButton.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
        binding.submitButton.text = "POSTING DISABLED - PLEASE CONTACT ADMINISTRATOR"
        
        // Show ban message
//        val banMessage = "You are banned from posting. Please contact an administrator for assistance."
//        android.widget.Toast.makeText(requireContext(), banMessage, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun clearForm() {
        binding.itemTitleInput.text?.clear()
        binding.descriptionInput.text?.clear()
        binding.dateTimeInput.text?.clear()
        binding.categorySpinner.setSelection(0)
        binding.lastSeenAtInput.text?.clear()
        binding.roomNumberInput.text?.clear()
        selectedImageUri = null
        binding.imagePreview.setImageResource(R.drawable.ic_image_upload)
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
            else -> "Invalid Month"
        }
    }
}
