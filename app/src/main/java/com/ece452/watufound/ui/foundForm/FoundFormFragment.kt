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
import androidx.navigation.fragment.findNavController
import com.ece452.watufound.R
import com.ece452.watufound.databinding.FragmentFoundFormBinding
import com.ece452.watufound.models.FoundItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ece452.watufound.utils.BuildingLocationUtils

class FoundFormFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFoundFormBinding? = null
    private val binding get() = _binding!!
    val sdf = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())

    private val viewModel: FoundFormViewModel by viewModels()

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var lastPin: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoundFormBinding.inflate(inflater, container, false)
        val view = _binding?.root ?: FragmentFoundFormBinding.inflate(inflater, container, false).root
        mapView = view.findViewById(R.id.mapViewPreview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpinners()

        // Date & Time Picker
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
                                year, month + 1, dayOfMonth, hourOfDay, minute
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

        binding.submitButton.setOnClickListener {
            val itemTitle = binding.itemTitleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val category = binding.categorySpinner.selectedItem?.toString()?.trim() ?: ""
            val foundAt = binding.foundAtInput.text.toString().trim()
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

            if (foundAt == "Select Location") {
                Toast.makeText(requireContext(), "Please select a valid location.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val enteredDate: Date = sdf.parse(dateTimeFound) ?: throw IllegalArgumentException("Invalid date")
                val currentDate = Date()

                if (enteredDate.after(currentDate)) {
                    Toast.makeText(requireContext(), "Date & Time Found cannot be in the future.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Invalid date format. Use yyyy-MM-dd, HH:mm.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitFoundItem(
                FoundItem(
                    itemTitle = itemTitle,
                    description = description,
                    category = category,
                    foundAt = foundAt,
                    dateTimeFound = dateTimeFound,
                    dateCreated = sdf.format(Date()),
                    isAnonymous = isAnonymous
                )
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

        val locations = listOf(
            "Select Location", //placeholder
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

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        binding.foundAtInput.setAdapter(locationAdapter)
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
            android.util.Log.e("FoundFormFragment", "Map styling failed", e)
        }
        
        // If a location was already selected, show it
        val location = binding.foundAtInput.text.toString()
        updateMapForLocation(location)
    }

    private fun clearForm() {
        binding.itemTitleInput.text?.clear()
        binding.descriptionInput.text?.clear()
        binding.dateTimeInput.text?.clear()
        binding.categorySpinner.setSelection(0)
        binding.foundAtInput.text?.clear()
    }

    // MapView lifecycle
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onDestroyView() { super.onDestroyView(); mapView?.onDestroy(); _binding = null }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
}
