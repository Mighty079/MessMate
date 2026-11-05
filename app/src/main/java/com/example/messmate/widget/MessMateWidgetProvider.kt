package com.example.messmate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.example.messmate.MainActivity
import com.example.messmate.R
import com.example.messmate.data.LocalDatabase
import com.example.messmate.data.Meal
import com.example.messmate.data.Routine
import java.text.SimpleDateFormat
import java.util.*

class MessMateWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    Log.d("MessMateWidget", "Updating widgets: ${appWidgetIds.size}")
    for (appWidgetId in appWidgetIds) {
      updateWidget(context, appWidgetManager, appWidgetId)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    Log.d("MessMateWidget", "Widget options changed")
    updateWidget(context, appWidgetManager, appWidgetId)
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
  }

  private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)

    val db = LocalDatabase(context)
    val menu = db.getMenu()
    val routines = db.getRoutines()
    val settings = db.getSettings()

    // Determine current weekday
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val currentDay = when (dayOfWeek) {
      Calendar.MONDAY -> "Monday"
      Calendar.TUESDAY -> "Tuesday"
      Calendar.WEDNESDAY -> "Wednesday"
      Calendar.THURSDAY -> "Thursday"
      Calendar.FRIDAY -> "Friday"
      Calendar.SATURDAY -> "Saturday"
      Calendar.SUNDAY -> "Sunday"
      else -> "Monday"
    }

    val todayMeals = menu.days[currentDay] ?: emptyList()
    val activeRoutines = routines.filter { it.enabled }

    val nextMeal = calculateNextMeal(todayMeals)

    // Determine layout: Small (Next Meal), Medium (Full Day Meals), Large (Meals + Routine)
    val layoutId = when {
      minHeight >= 140 && minWidth >= 180 -> R.layout.widget_large
      minWidth >= 220 -> R.layout.widget_medium
      else -> R.layout.widget_small
    }

    val views = RemoteViews(context.packageName, layoutId)

    // Setup tap handler to open app
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)


    // Populate Widget fields based on selected layout
    when (layoutId) {
      R.layout.widget_small -> {
        if (settings.menuUploadedAt != null && nextMeal != null) {
          views.setTextViewText(R.id.widget_meal_name, "${nextMeal.name} • ${nextMeal.time}")
          views.setTextViewText(R.id.widget_meal_items, nextMeal.items.joinToString(", "))
        } else {
          views.setTextViewText(R.id.widget_meal_name, "No Menu Loaded")
          views.setTextViewText(R.id.widget_meal_items, "Tap to upload mess menu image.")
        }
      }
      R.layout.widget_medium -> {
        val mealsById = todayMeals.associateBy { it.id }
        views.setTextViewText(R.id.widget_breakfast_items, mealsById["breakfast"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_lunch_items, mealsById["lunch"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_snacks_items, mealsById["snacks"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_dinner_items, mealsById["dinner"]?.items?.joinToString(", ") ?: "Not loaded")
      }
      R.layout.widget_large -> {
        // Meals list
        val mealsById = todayMeals.associateBy { it.id }
        views.setTextViewText(R.id.widget_large_breakfast, mealsById["breakfast"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_large_lunch, mealsById["lunch"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_large_snacks, mealsById["snacks"]?.items?.joinToString(", ") ?: "Not loaded")
        views.setTextViewText(R.id.widget_large_dinner, mealsById["dinner"]?.items?.joinToString(", ") ?: "Not loaded")

        // Next active routine task
        val abbr = when (currentDay) {
          "Monday" -> "Mo"
          "Tuesday" -> "Tu"
          "Wednesday" -> "We"
          "Thursday" -> "Th"
          "Friday" -> "Fr"
          "Saturday" -> "Sa"
          "Sunday" -> "Su"
          else -> ""
        }
        val todayRoutines = activeRoutines.filter { it.repeatDays.contains(abbr) }
        
        if (todayRoutines.isNotEmpty()) {
          val nextTask = todayRoutines[0]
          views.setTextViewText(R.id.widget_task_title, nextTask.title)
          views.setTextViewText(R.id.widget_task_time, "${nextTask.startTime} - ${nextTask.endTime ?: ""}")
        } else {
          views.setTextViewText(R.id.widget_task_title, "No active tasks")
          views.setTextViewText(R.id.widget_task_time, "Enjoy your day!")
        }
      }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
  }

  private fun calculateNextMeal(meals: List<Meal>): Meal? {
    if (meals.isEmpty()) return null
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    val currentTotal = currentHour * 60 + currentMinute

    val parsed = meals.mapNotNull { meal ->
      val timePair = parseTime(meal.time) ?: return@mapNotNull null
      val mealTotal = timePair.first * 60 + timePair.second
      Triple(meal, mealTotal, meal.enabled)
    }

    val future = parsed.filter { it.third && it.second > currentTotal }.minByOrNull { it.second }
    if (future != null) return future.first
    return parsed.filter { it.third }.minByOrNull { it.second }?.first ?: parsed.firstOrNull()?.first
  }

  private fun parseTime(timeStr: String): Pair<Int, Int>? {
    try {
      val clean = timeStr.trim().uppercase()
      val regex = Regex("""(\d+):(\d+)\s*(AM|PM)""")
      val match = regex.find(clean) ?: return null
      var hour = match.groupValues[1].toInt()
      val minute = match.groupValues[2].toInt()
      val ampm = match.groupValues[3]
      if (ampm == "PM" && hour < 12) hour += 12
      if (ampm == "AM" && hour == 12) hour = 0
      return Pair(hour, minute)
    } catch (e: Exception) {
      return null
    }
  }
}
