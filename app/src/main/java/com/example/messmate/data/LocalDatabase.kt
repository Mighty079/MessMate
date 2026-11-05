package com.example.messmate.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalDatabase(private val context: Context) {

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  private val menuFile = File(context.filesDir, "menu.json")
  private val routinesFile = File(context.filesDir, "routines.json")
  private val settingsFile = File(context.filesDir, "settings.json")

  fun getMenu(): Menu {
    return try {
      if (menuFile.exists()) {
        json.decodeFromString<Menu>(menuFile.readText())
      } else {
        Menu()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Menu()
    }
  }

  fun saveMenu(menu: Menu) {
    try {
      menuFile.writeText(json.encodeToString(menu))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getRoutines(): List<Routine> {
    return try {
      if (routinesFile.exists()) {
        json.decodeFromString<List<Routine>>(routinesFile.readText())
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  fun saveRoutines(routines: List<Routine>) {
    try {
      routinesFile.writeText(json.encodeToString(routines))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getSettings(): AppSettings {
    return try {
      if (settingsFile.exists()) {
        json.decodeFromString<AppSettings>(settingsFile.readText())
      } else {
        AppSettings()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      AppSettings()
    }
  }

  fun saveSettings(settings: AppSettings) {
    try {
      settingsFile.writeText(json.encodeToString(settings))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun clearMenu() {
    try {
      if (menuFile.exists()) {
        menuFile.delete()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
