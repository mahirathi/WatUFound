package com.ece452.watufound.ui.foundForm

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
import com.ece452.watufound.databinding.FragmentFoundFormBinding
import com.ece452.watufound.models.FoundItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import java.util.Calendar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.ece452.watufound.utils.BuildingLocationUtils
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


class FoundFormFragment : Fragment() {

    private var _binding: FragmentFoundFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FoundFormViewModel by viewModels()
    val sdf = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
    private var mapView: MapView? = null
    private var lastPin: GeoPoint? = null
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
        _binding = FragmentFoundFormBinding.inflate(inflater, container, false)
        mapView = _binding?.root?.findViewById(R.id.mapViewPreview)
        
        // Configure osmdroid
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", 0))
        
        // Set up the map
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        
        // Set initial view to Waterloo campus area
        mapView?.controller?.setCenter(GeoPoint(43.4723, -80.5449)) // Waterloo campus coordinates
        mapView?.controller?.setZoom(15.0)
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpinners()
        checkUserBanStatus()

        // Date & Time Picker
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

        // Show dropdown on click
        binding.foundAtInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) (view as AutoCompleteTextView).showDropDown()
        }
        binding.foundAtInput.setOnClickListener {
            binding.foundAtInput.showDropDown()
        }

        binding.foundAtInput.setOnItemClickListener { _, _, position, _ ->
            val location = binding.foundAtInput.adapter.getItem(position) as? String
            updateMapForLocation(location)
        }

        binding.buttonSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }


        binding.submitButton.setOnClickListener {
            val itemTitle = binding.itemTitleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val category = binding.categorySpinner.selectedItem?.toString()?.trim() ?: ""
            val foundAt = binding.foundAtInput.text.toString().trim()
            val roomNumber = binding.roomNumberInput.text.toString().trim()
            val dateTimeFound = binding.dateTimeInput.text.toString().trim()
            val isAnonymous = binding.switchAnonymous.isChecked

            if (itemTitle.isBlank() || description.isBlank() || dateTimeFound.isBlank()) {
                Toast.makeText(requireContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category == "Select Category") {
                Toast.makeText(requireContext(), "Please select a valid category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (foundAt.isBlank()) {
                Toast.makeText(requireContext(), "Please select a valid location.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Use internal format stored in tag, fallback to text if tag is null
                val dateTimeForValidation = binding.dateTimeInput.tag as? String ?: binding.dateTimeInput.text.toString()
                val enteredDate: Date = sdf.parse(dateTimeForValidation) ?: throw IllegalArgumentException("Invalid date")
                val currentDate = Date()

                if (enteredDate.after(currentDate)) {
                    Toast.makeText(requireContext(), "Date & Time Found cannot be in the future.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Invalid date format. Use yyyy-MM-dd, HH:mm.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateTimeForSubmission = binding.dateTimeInput.tag as? String ?: binding.dateTimeInput.text.toString()
            // Block image upload for sensitive categories
            val restrictedCategories = listOf("WatCard")
            if (restrictedCategories.contains(category) && selectedImageUri != null) {
                Toast.makeText(
                    requireContext(),
                    "Images cannot be attached for $category due to privacy concerns.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.submitFoundItem(
                FoundItem(
                    itemTitle = itemTitle,
                    description = description,
                    category = category,
                    foundAt = foundAt,
                    dateTimeFound = dateTimeForSubmission,
                    roomNumber = roomNumber,
                    dateCreated = sdf.format(Date()),
                    isAnonymous = isAnonymous
                ),
                selectedImageUri,
                onBanned = {
                    // Handle banned user
//                    Toast.makeText(requireContext(), "You are banned from posting. Please contact an administrator.", Toast.LENGTH_LONG).show()
                }
            )

            Toast.makeText(requireContext(), "Posting your found item...", Toast.LENGTH_SHORT).show()
            clearForm()
            findNavController().navigate(R.id.navigation_feed)
        }
    }

    private fun setupSpinners() {
        val categories = listOf(
            "Select Category", //placeholder
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
            "Engineering 6 (E6)",
            "Engineering 5 (E5)",
            "Engineering 7 (E7)",
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

        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        binding.foundAtInput.setAdapter(locationAdapter)
    }

    private fun updateMapForLocation(location: String?) {
        if (location.isNullOrBlank()) {
            mapView?.overlays?.clear()
            mapView?.invalidate()
            binding.mapViewPreview.visibility = View.GONE
            return
        }
        
        if (location == "Other") {
            binding.mapViewPreview.visibility = View.VISIBLE
            
            // Clear any existing markers
            mapView?.overlays?.clear()
            
            // Center on general University of Waterloo area
            val uwCenter = GeoPoint(43.470341, -80.544251)
            mapView?.controller?.setCenter(uwCenter)
            mapView?.controller?.setZoom(14.0) // Zoom out to show general area
            mapView?.invalidate()
            return
        }
        
        val geoPoint = BuildingLocationUtils.getBuildingGeoPoint(requireContext(), location!!)
        if (geoPoint != null) {
            binding.mapViewPreview.visibility = View.VISIBLE
            
            mapView?.overlays?.clear()
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = location
            
            // Create a modern pin appearance
            marker.icon = requireContext().getDrawable(R.drawable.ic_red_map_pin)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            mapView?.overlays?.add(marker)
            mapView?.controller?.setCenter(geoPoint)
            mapView?.controller?.setZoom(17.0)
            mapView?.invalidate()
            lastPin = geoPoint
        } else {
            // Hide the map if coordinates not found
            binding.mapViewPreview.visibility = View.GONE
            mapView?.overlays?.clear()
            mapView?.invalidate()
        }
    }

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
                android.util.Log.e("FoundFormFragment", "Failed to check ban status", exception)
            }
    }

    private fun disableFormForBannedUser() {
        // Disable all form inputs
        binding.itemTitleInput.isEnabled = false
        binding.descriptionInput.isEnabled = false
        binding.categorySpinner.isEnabled = false
        binding.foundAtInput.isEnabled = false
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
        binding.foundAtInput.text?.clear()
        binding.roomNumberInput.text?.clear()
        selectedImageUri = null
        binding.imagePreview.setImageResource(R.drawable.ic_image_upload)
    }

    // Helper function to get month name
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
            else -> "Unknown Month"
        }
    }

    // MapView lifecycle
    override fun onResume() { 
        super.onResume()
        mapView?.onResume()
    }
    
    override fun onPause() { 
        super.onPause()
        mapView?.onPause()
    }
    
    override fun onDestroyView() { 
        super.onDestroyView()
        mapView?.onDetach()
        _binding = null
    }
    
    override fun onLowMemory() { 
        super.onLowMemory()
        mapView?.onDetach()
    }
}
