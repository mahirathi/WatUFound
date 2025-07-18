package com.ece452.watufound.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun getRelativeTimeAgo(timestamp: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.getDefault())
            val date = dateFormat.parse(timestamp)
            val now = Date()
            val diffInMillis = now.time - date.time
            
            val seconds = diffInMillis / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7
            val months = days / 30
            val years = days / 365
            
            when {
                years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
                months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
                weeks > 0 -> "$weeks week${if (weeks > 1) "s" else ""} ago"
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
            }
        } catch (e: Exception) {
            timestamp // Return original timestamp if parsing fails
        }
    }
} 