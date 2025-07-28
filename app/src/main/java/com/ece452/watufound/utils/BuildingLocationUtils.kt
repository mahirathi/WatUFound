package com.ece452.watufound.utils

import android.content.Context
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader

object BuildingLocationUtils {
    private var buildingMap: Map<String, GeoPoint>? = null

    // Map dropdown names to CSV building names/codes
    private val dropdownToCsvName = mapOf(
        "Accelerator Centre Waterloo (ACW)" to "Accelerator Centre Waterloo",
        "Arts Lecture Hall (AL)" to "Arts Lecture Hall",
        "Brubacher House (BRH)" to "Brubacher House",
        "Centre for Environmental & Information Technology (EIT)" to "Centre for Environmental and Information Technology",
        "Central Services Building (CSB)" to "Central Services Building",
        "Dana Porter Library (DP)" to "Dana Porter Arts Library",
        "Davis Centre Library (DC)" to "William G. Davis Computer Research Centre",
        "Douglas Wright Engineering Building (DWE)" to "Douglas Wright Engineering Building",
        "East Campus Hall (ECH)" to "East Campus Hall",
        "Earth Sciences & Chemistry (ESC)" to "Earth Sciences & Chemistry",
        "Engineering 2 (E2)" to "Engineering 2",
        "Engineering 3 (E3)" to "Engineering 3",
        "Engineering 5 (E5)" to "Engineering 5",
        "Engineering 6 (E6)" to "Engineering 6",
        "Engineering 7 (E7)" to "Engineering 7",
        "Energy Research Centre (ERC)" to "Energy Research Centre",
        "Environment 1 (EV1)" to "Environment 1",
        "Environment 2 (EV2)" to "Environment 2",
        "Environment 3 (EV3)" to "Environment 3",
        "Federation Hall (FED)" to "Federation Hall",
        "General Services Complex (GSC)" to "General Services Complex",
        "Hagey Hall (HH)" to "J.G. Hagey Hall of the Humanities",
        "Health Expansion Building (EXP)" to "Expansion",
        "Health Services (HS)" to "Health Services",
        "Math & Computer Building (MC)" to "Mathematics & Computer Building",
        "Modern Languages (ML)" to "Modern Languages",
        "Physical Activities Complex (PAC)" to "Physical Activities Complex",
        "Quantum Nano Centre (QNC)" to "Mike & Ophelia Lazaridis Quantum-Nano Centre",
        "Research Advancement Centre (RAC)" to "Research Advancement Centre",
        "Research Advancement Centre 2 (RA2)" to "Research Advancement Centre 2",
        "School of Optometry (OPT)" to "Optometry",
        "South Campus Hall (SCH)" to "South Campus Hall",
        "Student Life Centre (SLC)" to "Student Life Centre",
        "Tatham Centre (TC)" to "William M. Tatham Centre for Co-operative Education & Career Action",
        "University Club (UC)" to "University Club"
    )

    fun getBuildingGeoPoint(context: Context, dropdownName: String): GeoPoint? {
        if (buildingMap == null) {
            buildingMap = loadBuildingMap(context)
        }
        val csvName = dropdownToCsvName[dropdownName] ?: dropdownName
        val result = buildingMap?.get(csvName)
        
        // Debug logging
        android.util.Log.d("BuildingLocationUtils", "Looking for: '$dropdownName'")
        android.util.Log.d("BuildingLocationUtils", "Mapped to CSV name: '$csvName'")
        android.util.Log.d("BuildingLocationUtils", "Found coordinates: $result")
        android.util.Log.d("BuildingLocationUtils", "Available buildings: ${buildingMap?.keys?.take(5)}")
        
        return result
    }

    private fun loadBuildingMap(context: Context): Map<String, GeoPoint> {
        val map = mutableMapOf<String, GeoPoint>()
        val assetManager = context.assets
        assetManager.open("Buildings.csv").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val header = reader.readLine() // skip header
                reader.lineSequence().forEach { line ->
                    val tokens = line.split(",")
                    if (tokens.size >= 7) {
                        val name = tokens[3].trim()
                        val lat = tokens[5].toDoubleOrNull()
                        val lng = tokens[6].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            map[name] = GeoPoint(lat, lng)
                        }
                    }
                }
            }
        }
        return map
    }
} 