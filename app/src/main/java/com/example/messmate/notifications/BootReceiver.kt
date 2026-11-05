package com.example.messmate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.messmate.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    Log.d("BootReceiver", "Received broadcast action: $action. Rescheduling all MessMate alarms...")
    
    val pendingResult = goAsync()
    val repository = DataRepository(context.applicationContext)
    val scheduler = AlarmScheduler(context.applicationContext)
    
    CoroutineScope(Dispatchers.IO).launch {
      try {
        scheduler.rescheduleAll(repository)
        Log.d("BootReceiver", "Rescheduled all alarms successfully for action: $action.")
      } catch (e: Exception) {
        Log.e("BootReceiver", "Error rescheduling in BootReceiver", e)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
