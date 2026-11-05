package com.example.messmate.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.messmate.MainActivity
import com.example.messmate.R
import com.example.messmate.data.DataRepository
import com.example.messmate.data.LocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val type = intent.getStringExtra("type") ?: "meal"
    val title = intent.getStringExtra("title") ?: "MessMate Reminder"
    val content = intent.getStringExtra("content") ?: ""

    Log.d("AlarmReceiver", "Received alarm trigger! Type: $type, Title: $title, Content: $content")

    val db = LocalDatabase(context)
    val settings = db.getSettings()

    val hasSound = settings.notificationSound
    val hasVibration = settings.vibration

    val channelId = when {
      hasSound && hasVibration -> "messmate_channel_sv_v2"
      hasSound && !hasVibration -> "messmate_channel_s_v2"
      !hasSound && hasVibration -> "messmate_channel_v_v2"
      else -> "messmate_channel_none_v2"
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val existingChannel = notificationManager.getNotificationChannel(channelId)
      if (existingChannel == null) {
        val channelName = "MessMate Reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance).apply {
          description = "MessMate meal reminders and daily routines"
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
          enableLights(true)

          if (hasSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(
              soundUri,
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
          } else {
            setSound(null, null)
          }

          if (hasVibration) {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
          } else {
            enableVibration(false)
            vibrationPattern = null
          }
        }
        notificationManager.createNotificationChannel(channel)
      }
    }

    // Check runtime notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        Log.e("AlarmReceiver", "Cannot post notification: POST_NOTIFICATIONS permission is not granted!")
      }
    }

    val clickIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      (System.currentTimeMillis() and 0xffff).toInt(),
      clickIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    if (hasSound) {
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      notificationBuilder.setSound(soundUri)
    }

    if (hasVibration) {
      notificationBuilder.setVibrate(longArrayOf(0, 300, 200, 300))
    } else {
      notificationBuilder.setVibrate(longArrayOf(0))
    }

    try {
      val notificationId = when (type) {
        "meal" -> 1001 + (intent.getStringExtra("meal_id")?.hashCode() ?: 0) % 100
        "routine" -> 2001 + (intent.getStringExtra("routine_id")?.hashCode() ?: 0) % 100
        else -> 9999
      }
      notificationManager.notify(notificationId, notificationBuilder.build())
      Log.d("AlarmReceiver", "Notification posted successfully with ID $notificationId")
    } catch (e: Exception) {
      Log.e("AlarmReceiver", "Failed to post notification", e)
    }

    // Reschedule so the repeating cycle never breaks!
    // Non-test alarms trigger a background refresh of the full schedule.
    if (type != "test") {
      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val repository = DataRepository(context.applicationContext)
          val scheduler = AlarmScheduler(context.applicationContext)
          scheduler.rescheduleAll(repository)
          Log.d("AlarmReceiver", "Successfully renewed all upcoming alarms after trigger.")
        } catch (e: Exception) {
          Log.e("AlarmReceiver", "Failed to renew alarms after trigger", e)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }
}
