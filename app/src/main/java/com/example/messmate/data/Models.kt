package com.example.messmate.data

import kotlinx.serialization.Serializable

@Serializable
data class Meal(
  val id: String, // "breakfast", "lunch", "snacks", "dinner"
  val name: String, // "Breakfast", "Lunch", "Snacks", "Dinner"
  val time: String, // "8:00 AM", "1:00 PM", "5:00 PM", "8:00 PM"
  val items: List<String>,
  val enabled: Boolean = true
)

@Serializable
data class Menu(
  val days: Map<String, List<Meal>> = emptyMap()
)

@Serializable
data class Routine(
  val id: String,
  val title: String,
  val startTime: String, // "8:00 AM"
  val endTime: String? = null, // "10:00 AM"
  val repeatDays: List<String>, // listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
  val reminderTime: String, // "7:55 AM"
  val enabled: Boolean = true,
  val notes: String? = null
)

val PERMANENT_GEMINI_API_KEY: String = run {
  try {
    String(android.util.Base64.decode("QVEuQWI4Uk42S3JPX0tMNnFUSHF1TnJqbmJQX012bzdxeWFIc2MzQms3TEp0T3FLZVlMZw==", android.util.Base64.DEFAULT)).trim()
  } catch (e: Exception) {
    ""
  }
}

@Serializable
data class AppSettings(
  val notificationSound: Boolean = true,
  val vibration: Boolean = true,
  val reminderTimingMinutes: Int = 10, // 0, 10, 15
  val darkMode: Boolean = false,
  val userName: String = "Arjun",
  val geminiApiKey: String = PERMANENT_GEMINI_API_KEY,
  val menuUploadedAt: String? = null
)

