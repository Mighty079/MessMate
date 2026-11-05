package com.example.messmate.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataRepository(context: Context) {
  private val db = LocalDatabase(context)
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _menu = MutableStateFlow(Menu())
  val menu: StateFlow<Menu> = _menu.asStateFlow()

  private val _routines = MutableStateFlow(emptyList<Routine>())
  val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  init {
    scope.launch {
      val loadedMenu = cleanMenuTimings(db.getMenu())
      val loadedRoutines = db.getRoutines()
      val loadedSettings = db.getSettings()
      
      _menu.value = loadedMenu
      _routines.value = loadedRoutines
      _settings.value = loadedSettings
    }
  }

  fun refreshAll() {
    scope.launch {
      val loadedMenu = cleanMenuTimings(db.getMenu())
      val loadedRoutines = db.getRoutines()
      val loadedSettings = db.getSettings()
      
      _menu.value = loadedMenu
      _routines.value = loadedRoutines
      _settings.value = loadedSettings
    }
  }

  fun updateMenu(newMenu: Menu) {
    val cleanedMenu = cleanMenuTimings(newMenu)
    _menu.value = cleanedMenu
    scope.launch {
      db.saveMenu(cleanedMenu)
    }
  }

  fun clearMenu() {
    val emptyMenu = Menu()
    _menu.value = emptyMenu
    
    val currentSettings = _settings.value
    val newSettings = currentSettings.copy(menuUploadedAt = null)
    _settings.value = newSettings
    scope.launch {
      db.clearMenu()
      db.saveSettings(newSettings)
    }
  }

  fun updateRoutines(newRoutines: List<Routine>) {
    _routines.value = newRoutines
    scope.launch {
      db.saveRoutines(newRoutines)
    }
  }

  fun addRoutine(routine: Routine) {
    val newList = _routines.value + routine
    updateRoutines(newList)
  }

  fun editRoutine(updated: Routine) {
    val newList = _routines.value.map { if (it.id == updated.id) updated else it }
    updateRoutines(newList)
  }

  fun deleteRoutine(routineId: String) {
    val newList = _routines.value.filter { it.id != routineId }
    updateRoutines(newList)
  }

  fun updateSettings(newSettings: AppSettings) {
    _settings.value = newSettings
    scope.launch {
      db.saveSettings(newSettings)
    }
  }

  private fun cleanMenuTimings(rawMenu: Menu): Menu {
    val cleanedDays = rawMenu.days.mapValues { (_, meals) ->
      meals.map { meal ->
        val cleanTime = cleanTimeStr(meal.time)
        meal.copy(time = cleanTime)
      }
    }
    return Menu(cleanedDays)
  }

  private fun cleanTimeStr(timeStr: String): String {
    val clean = timeStr.trim()
    
    // Attempt regex matching first:
    val higherSemRegex = Regex("""(?i)HIGHER\s*(?:SEMESTER|SEM)\s*[-–]?\s*([\d.:]+\s*(?:to|and|-|–)\s*[\d.:]+\s*(?:AM|PM)?)""")
    val match = higherSemRegex.find(clean)
    if (match != null) {
      return match.groupValues[1].trim()
    }
    
    // Substring fallback:
    val lowerClean = clean.lowercase()
    if (lowerClean.contains("higher semester")) {
      val sub = clean.substring(lowerClean.indexOf("higher semester") + "higher semester".length)
      return sub.replace(Regex("""^[-\s–]+"""), "").trim()
    } else if (lowerClean.contains("higher sem")) {
      val sub = clean.substring(lowerClean.indexOf("higher sem") + "higher sem".length)
      return sub.replace(Regex("""^[-\s–]+"""), "").trim()
    }
    
    return clean.replace(Regex("(?i)First Year[-–]?"), "")
                .replace(Regex("(?i)Higher Semester[-–]?"), "")
                .replace(Regex("(?i)Higher Sem[-–]?"), "")
                .trim()
  }
}

