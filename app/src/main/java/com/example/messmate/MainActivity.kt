package com.example.messmate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.messmate.data.DataRepository
import com.example.messmate.notifications.AlarmScheduler
import com.example.messmate.theme.MessMateTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      Log.d("MainActivity", "Notification permission granted by user!")
      val repository = DataRepository(applicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        AlarmScheduler(applicationContext).rescheduleAll(repository)
      }
    } else {
      Log.w("MainActivity", "Notification permission was denied by user.")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    // Refresh and ensure all alarms are scheduled on startup
    val repository = DataRepository(applicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      AlarmScheduler(applicationContext).rescheduleAll(repository)
    }

    enableEdgeToEdge()
    setContent {
      val repo = remember { repository }
      val settingsState by repo.settings.collectAsState()

      MessMateTheme(darkTheme = settingsState.darkMode, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(repo)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val repository = DataRepository(applicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      AlarmScheduler(applicationContext).rescheduleAll(repository)
    }
  }
}
