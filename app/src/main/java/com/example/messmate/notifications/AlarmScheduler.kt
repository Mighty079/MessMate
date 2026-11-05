package com.example.messmate.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.messmate.MainActivity
import com.example.messmate.data.DataRepository
import com.example.messmate.data.Meal
import com.example.messmate.data.Routine
import java.util.Calendar
import java.util.Date

class AlarmScheduler(private val context: Context) {

  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  fun rescheduleAll(repository: DataRepository) {
    val settings = repository.settings.value
    val menu = repository.menu.value
    val routines = repository.routines.value

    Log.d("AlarmScheduler", "rescheduleAll called. Menu days count: ${menu.days.size}, Routines count: ${routines.size}")

    // Cancel all previously scheduled meal alarms (1..7 days for all 4 meals)
    listOf("breakfast", "lunch", "snacks", "dinner").forEach { cancelMealAlarms(it) }
    // Cancel all active routines
    routines.forEach { cancelRoutineAlarms(it) }

    // Schedule Meals
    if (menu.days.isNotEmpty()) {
      menu.days.forEach { (dayName, meals) ->
        val calendarDay = mapDayToCalendar(dayName) ?: return@forEach
        meals.forEach { meal ->
          if (meal.enabled) {
            scheduleMealAlarm(meal, calendarDay, settings.reminderTimingMinutes)
          }
        }
      }
    }

    // Schedule Routines
    routines.forEach { routine ->
      if (routine.enabled) {
        routine.repeatDays.forEach { repeatDay ->
          val calendarDay = mapAbbreviationToCalendar(repeatDay) ?: return@forEach
          scheduleRoutineAlarm(routine, calendarDay)
        }
      }
    }
  }

  fun calculateNextOccurrence(targetDayOfWeek: Int, hour: Int, minute: Int, reminderMinutesBefore: Int = 0): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.MINUTE, -reminderMinutesBefore)
    }

    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    var daysUntil = (targetDayOfWeek - currentDayOfWeek + 7) % 7

    // If it's today, but the reminder time has already passed (or is within 15 seconds), schedule for next week (+7 days)
    if (daysUntil == 0 && target.timeInMillis <= now.timeInMillis + 15_000) {
      daysUntil = 7
    }

    target.add(Calendar.DAY_OF_YEAR, daysUntil)
    return target.timeInMillis
  }

  private fun scheduleMealAlarm(meal: Meal, calendarDay: Int, reminderTimingMinutes: Int) {
    val timePair = parseTime(meal.time) ?: return
    val triggerMillis = calculateNextOccurrence(calendarDay, timePair.first, timePair.second, reminderTimingMinutes)

    val timingLabel = if (reminderTimingMinutes > 0) "in $reminderTimingMinutes mins" else "now"
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = "com.example.messmate.MEAL_ALARM_${meal.id.lowercase().trim()}_$calendarDay"
      putExtra("type", "meal")
      putExtra("title", "${meal.name} $timingLabel")
      putExtra("content", meal.items.joinToString(", "))
      putExtra("meal_id", meal.id)
      putExtra("calendar_day", calendarDay)
      putExtra("reminder_minutes", reminderTimingMinutes)
    }

    val requestCode = getMealRequestCode(meal.id, calendarDay)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    scheduleExactAlarm(triggerMillis, pendingIntent, "Meal: ${meal.name} (Day: $calendarDay)")
  }

  private fun scheduleRoutineAlarm(routine: Routine, calendarDay: Int) {
    val timePair = parseTime(routine.reminderTime) ?: return
    val triggerMillis = calculateNextOccurrence(calendarDay, timePair.first, timePair.second, 0)

    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = "com.example.messmate.ROUTINE_ALARM_${routine.id}_$calendarDay"
      putExtra("type", "routine")
      putExtra("title", "Time for ${routine.title}")
      val subtitle = routine.notes?.takeIf { it.isNotBlank() }
        ?: "Routine: ${routine.startTime}${if (!routine.endTime.isNullOrEmpty()) " - ${routine.endTime}" else ""}"
      putExtra("content", subtitle)
      putExtra("routine_id", routine.id)
      putExtra("calendar_day", calendarDay)
    }

    val requestCode = getRoutineRequestCode(routine.id, calendarDay)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    scheduleExactAlarm(triggerMillis, pendingIntent, "Routine: ${routine.title} (Day: $calendarDay)")
  }

  private fun scheduleExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent, description: String) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
          Log.d("AlarmScheduler", "Scheduled exact alarm for $description at ${Date(triggerAtMillis)}")
          return
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
        Log.d("AlarmScheduler", "Scheduled exact alarm for $description at ${Date(triggerAtMillis)}")
        return
      }
    } catch (e: SecurityException) {
      Log.w("AlarmScheduler", "Exact alarm security restriction encountered for $description. Falling back to setAlarmClock.", e)
    }

    // High-reliability Fallback 1: AlarmClockInfo (exempt from SCHEDULE_EXACT_ALARM on Android 12+, wakes from Doze)
    try {
      val showIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val showPendingIntent = PendingIntent.getActivity(
        context,
        0,
        showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
      alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
      Log.d("AlarmScheduler", "Scheduled alarm clock for $description at ${Date(triggerAtMillis)}")
      return
    } catch (e: Exception) {
      Log.w("AlarmScheduler", "setAlarmClock failed for $description. Falling back to setAndAllowWhileIdle.", e)
    }

    // Fallback 2: setAndAllowWhileIdle
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
      }
      Log.d("AlarmScheduler", "Scheduled allow-while-idle alarm for $description at ${Date(triggerAtMillis)}")
    } catch (e: Exception) {
      Log.e("AlarmScheduler", "All alarm scheduling methods failed for $description", e)
    }
  }

  fun cancelRoutineAlarms(routine: Routine) {
    val days = 1..7
    days.forEach { calendarDay ->
      val requestCode = getRoutineRequestCode(routine.id, calendarDay)
      val intent = Intent(context, AlarmReceiver::class.java)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
      }
    }
  }

  fun cancelMealAlarms(mealId: String) {
    val days = 1..7
    days.forEach { calendarDay ->
      val requestCode = getMealRequestCode(mealId, calendarDay)
      val intent = Intent(context, AlarmReceiver::class.java)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
      }
    }
  }

  fun sendTestNotification() {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = "com.example.messmate.TEST_NOTIFICATION"
      putExtra("type", "test")
      putExtra("title", "MessMate Notifications Working!")
      putExtra("content", "Meal reminders and routine notifications are configured successfully.")
    }
    context.sendBroadcast(intent)
  }

  private fun getMealRequestCode(mealId: String, calendarDay: Int): Int {
    return (mealId.lowercase().trim().hashCode() * 31 + calendarDay) and 0xfffffff
  }

  private fun getRoutineRequestCode(routineId: String, calendarDay: Int): Int {
    return (routineId.trim().hashCode() * 31 + calendarDay + 1000) and 0xfffffff
  }

  fun parseTime(timeStr: String): Pair<Int, Int>? {
    try {
      val clean = timeStr.trim().uppercase()
      val timeRegex = Regex("""(\d+)[.:](\d+)""")
      val match = timeRegex.find(clean)

      var hour = 0
      var minute = 0
      var searchStartIndex = 0

      if (match != null) {
        hour = match.groupValues[1].toInt()
        minute = match.groupValues[2].toInt()
        searchStartIndex = match.range.last + 1
      } else {
        val singleNumberRegex = Regex("""(\d+)""")
        val singleMatch = singleNumberRegex.find(clean) ?: return null
        hour = singleMatch.groupValues[1].toInt()
        minute = 0
        searchStartIndex = singleMatch.range.last + 1
      }

      val remainder = clean.substring(searchStartIndex)
      var isPm = false
      val amIndex = remainder.indexOf("AM")
      val pmIndex = remainder.indexOf("PM")

      if (amIndex != -1 && pmIndex != -1) {
        isPm = pmIndex < amIndex
      } else if (pmIndex != -1) {
        isPm = true
      } else if (amIndex != -1) {
        isPm = false
      } else {
        isPm = clean.contains("PM")
      }

      if (isPm && hour < 12) hour += 12
      if (!isPm && hour == 12) hour = 0

      if (hour !in 0..23 || minute !in 0..59) return null

      return Pair(hour, minute)
    } catch (e: Exception) {
      return null
    }
  }

  fun mapDayToCalendar(day: String): Int? {
    val clean = day.trim().lowercase()
    return when {
      clean.startsWith("sun") || clean == "su" -> Calendar.SUNDAY
      clean.startsWith("mon") || clean == "mo" -> Calendar.MONDAY
      clean.startsWith("tue") || clean == "tu" -> Calendar.TUESDAY
      clean.startsWith("wed") || clean == "we" -> Calendar.WEDNESDAY
      clean.startsWith("thu") || clean == "th" -> Calendar.THURSDAY
      clean.startsWith("fri") || clean == "fr" -> Calendar.FRIDAY
      clean.startsWith("sat") || clean == "sa" -> Calendar.SATURDAY
      else -> null
    }
  }

  fun mapAbbreviationToCalendar(abbr: String): Int? {
    val clean = abbr.trim().lowercase()
    return when {
      clean.startsWith("su") -> Calendar.SUNDAY
      clean.startsWith("mo") -> Calendar.MONDAY
      clean.startsWith("tu") -> Calendar.TUESDAY
      clean.startsWith("we") -> Calendar.WEDNESDAY
      clean.startsWith("th") -> Calendar.THURSDAY
      clean.startsWith("fr") -> Calendar.FRIDAY
      clean.startsWith("sa") -> Calendar.SATURDAY
      else -> null
    }
  }
}
