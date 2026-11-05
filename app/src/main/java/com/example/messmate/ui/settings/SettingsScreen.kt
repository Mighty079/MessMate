package com.example.messmate.ui.settings

import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messmate.data.AppSettings
import com.example.messmate.data.DataRepository
import com.example.messmate.data.Meal
import com.example.messmate.data.Menu
import com.example.messmate.data.PERMANENT_GEMINI_API_KEY
import com.example.messmate.notifications.AlarmScheduler
import com.example.messmate.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  repository: DataRepository,
  modifier: Modifier = Modifier
) {
  val settingsState by repository.settings.collectAsState()
  val menuState by repository.menu.collectAsState()
  
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val alarmScheduler = remember { AlarmScheduler(context) }
  
  var tempUserName by remember { mutableStateOf(settingsState.userName) }
  var showTimingMenu by remember { mutableStateOf(false) }
  var isExtracting by remember { mutableStateOf(false) }
  var ocrResultText by remember { mutableStateOf<String?>(null) }

  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      scope.launch {
        isExtracting = true
        ocrResultText = null
        try {
          val extractedMenu = performGeminiOcr(context, it, PERMANENT_GEMINI_API_KEY)
          if (extractedMenu != null) {
            repository.updateMenu(extractedMenu)
            val todayStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
            repository.updateSettings(
              settingsState.copy(menuUploadedAt = "Uploaded $todayStr")
            )
            alarmScheduler.rescheduleAll(repository)
            ocrResultText = "Menu extracted successfully!"
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ocrResultText = "Network exception: ${e.localizedMessage}."
        } finally {
          isExtracting = false
        }
      }
    }
  }

  Scaffold(
    containerColor = WarmBackground,
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd)))
          .statusBarsPadding()
          .padding(vertical = 20.dp, horizontal = 16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text(
              text = "Settings",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Customize your MessMate experience",
              fontSize = 14.sp,
              color = Color.White.copy(alpha = 0.8f)
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      Column(
        modifier = modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Section: Profile settings (Customization without account)
        SettingsGroup(title = "PROFILE") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Profile",
              tint = TextSecondary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
              value = tempUserName,
              onValueChange = {
                tempUserName = it
                repository.updateSettings(settingsState.copy(userName = it))
              },
              label = { Text("Display Name") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleStart,
                unfocusedBorderColor = CardBorder
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Section: Notifications
        SettingsGroup(title = "NOTIFICATIONS") {
          val notifManager = remember { androidx.core.app.NotificationManagerCompat.from(context) }
          val notificationsEnabled = remember(settingsState) { notifManager.areNotificationsEnabled() }

          if (!notificationsEnabled) {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                  }
                  context.startActivity(intent)
                }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsOff,
                  contentDescription = "Disabled",
                  tint = Color(0xFFD32F2F),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Notifications are blocked in system settings",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                  )
                  Text(
                    text = "Tap here to allow MessMate notifications so you never miss a meal.",
                    color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                    fontSize = 11.sp
                  )
                }
              }
            }
            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))
          }

          SettingsToggleRow(
            icon = Icons.Default.VolumeUp,
            title = "Notification Sound",
            subtitle = "Play sound with meal reminders.",
            checked = settingsState.notificationSound,
            onCheckedChange = {
              repository.updateSettings(settingsState.copy(notificationSound = it))
              alarmScheduler.rescheduleAll(repository)
            }
          )
          
          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))
          
          SettingsToggleRow(
            icon = Icons.Default.Vibration,
            title = "Vibration",
            subtitle = "Vibrate on notifications.",
            checked = settingsState.vibration,
            onCheckedChange = {
              repository.updateSettings(settingsState.copy(vibration = it))
              alarmScheduler.rescheduleAll(repository)
            }
          )

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))

          // Reminder Timing Select
          Box {
            SettingsClickRow(
              icon = Icons.Default.Alarm,
              title = "Reminder Timing",
              subtitle = "When to notify before meals.",
              trailingText = when (settingsState.reminderTimingMinutes) {
                0 -> "At start time"
                5 -> "5 min before"
                10 -> "10 min before"
                15 -> "15 min before"
                else -> "${settingsState.reminderTimingMinutes} min before"
              },
              onClick = { showTimingMenu = true }
            )

            DropdownMenu(
              expanded = showTimingMenu,
              onDismissRequest = { showTimingMenu = false },
              modifier = Modifier.background(Color.White)
            ) {
              val timingOptions = listOf(
                Pair("At start time", 0),
                Pair("5 min before", 5),
                Pair("10 min before", 10),
                Pair("15 min before", 15)
              )
              timingOptions.forEach { (label, minutes) ->
                DropdownMenuItem(
                  text = { Text(label, color = TextPrimary) },
                  onClick = {
                    repository.updateSettings(settingsState.copy(reminderTimingMinutes = minutes))
                    alarmScheduler.rescheduleAll(repository)
                    showTimingMenu = false
                  }
                )
              }
            }
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))

          // Test Notification Row
          SettingsClickRow(
            icon = Icons.Default.NotificationsActive,
            title = "Send Test Notification",
            subtitle = "Verify sound, vibration & banner right now.",
            onClick = {
              alarmScheduler.sendTestNotification()
              Toast.makeText(context, "Test reminder triggered! Check status bar.", Toast.LENGTH_SHORT).show()
            }
          )

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))

          // Sync & Reschedule Row
          SettingsClickRow(
            icon = Icons.Default.Sync,
            title = "Sync & Reschedule Reminders",
            subtitle = "Re-check and renew all upcoming alarms.",
            onClick = {
              alarmScheduler.rescheduleAll(repository)
              Toast.makeText(context, "All meal and routine reminders synchronized!", Toast.LENGTH_SHORT).show()
            }
          )
        }

        // Section: Appearance
        SettingsGroup(title = "APPEARANCE") {
          SettingsToggleRow(
            icon = Icons.Default.Brightness4,
            title = "Dark Mode",
            subtitle = "Switch to dark theme.",
            checked = settingsState.darkMode,
            onCheckedChange = {
              repository.updateSettings(settingsState.copy(darkMode = it))
            }
          )
        }



        // Section: Menu
        SettingsGroup(title = "MENU") {
          SettingsClickRow(
            icon = Icons.Default.UploadFile,
            title = "Upload New Menu",
            subtitle = "Replace this month's menu.",
            onClick = { imagePickerLauncher.launch("image/*") }
          )

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.AssignmentTurnedIn,
                contentDescription = "Menu Active Status",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Current Menu",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = settingsState.menuUploadedAt ?: "No menu loaded",
                  fontSize = 13.sp,
                  color = TextSecondary
                )
              }
            }
            if (settingsState.menuUploadedAt != null) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFFEAFBF0))
                  .border(1.dp, SnacksAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "ACTIVE",
                  color = SnacksAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        // Section: Data Control
        SettingsGroup(title = "DATA") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                repository.clearMenu()
                alarmScheduler.rescheduleAll(repository)
                Toast.makeText(context, "Stored menu cleared", Toast.LENGTH_SHORT).show()
              }
              .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Clear",
                tint = Color(0xFFD6384B),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Clear Stored Menu",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFD6384B)
                )
                Text(
                  text = "Remove all extracted menu data.",
                  fontSize = 13.sp,
                  color = TextSecondary
                )
              }
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Clear Menu Details",
              tint = Color(0xFFD6384B),
              modifier = Modifier.size(24.dp)
            )
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 40.dp))

          // Backup Routine (disabled)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = "Backup",
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Backup Routine",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary.copy(alpha = 0.5f)
                )
                Text(
                  text = "Import/export routine data (coming soon)",
                  fontSize = 13.sp,
                  color = TextSecondary.copy(alpha = 0.5f)
                )
              }
            }
            Text(
              text = "Soon",
              fontSize = 12.sp,
              color = TextSecondary.copy(alpha = 0.5f),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(end = 8.dp)
            )
          }
        }

        // Section: About card
        Card(
          modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
          colors = CardDefaults.cardColors(containerColor = AppWhite),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Simulated Logo
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrangeStart),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "MessMate",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = TextPrimary
            )
            Text(
              text = "Version 1.0.0 MVP",
              fontSize = 13.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Smart Hostel Menu & Routine Reminder",
              fontSize = 12.sp,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            // Badges row
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              val badges = listOf("Offline-first", "No ads", "No login", "<30 MB")
              badges.forEach { badge ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFF2E6))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                  Text(
                    text = badge,
                    color = OrangeStart,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      // Dialog for active extraction loader/status
      if (isExtracting) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {},
          contentAlignment = Alignment.Center
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(32.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator(color = PurpleStart)
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Extracting menu via AI OCR...",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
              )
              Text(
                text = "Processing layout, timings, and items",
                fontSize = 13.sp,
                color = TextSecondary
              )
            }
          }
        }
      }

      // OCR Result Dialog
      ocrResultText?.let { result ->
        AlertDialog(
          onDismissRequest = { ocrResultText = null },
          title = { Text("OCR Process Complete", fontWeight = FontWeight.Bold) },
          text = { Text(result, color = TextPrimary) },
          confirmButton = {
            Button(
              onClick = { ocrResultText = null },
              colors = ButtonDefaults.buttonColors(containerColor = PurpleStart)
            ) {
              Text("OK")
            }
          }
        )
      }


    }
  }
}

@Composable
fun SettingsGroup(
  title: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = TextSecondary,
      modifier = Modifier.padding(start = 8.dp)
    )
    Card(
      modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
      colors = CardDefaults.cardColors(containerColor = AppWhite),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(8.dp)
      ) {
        content()
      }
    }
  }
}

@Composable
fun SettingsToggleRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = TextSecondary,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = TextSecondary
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = PurpleStart,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color.LightGray
      )
    )
  }
}

@Composable
fun SettingsClickRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  trailingText: String? = null,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = TextSecondary,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = TextSecondary
        )
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (trailingText != null) {
        Text(
          text = trailingText,
          fontSize = 13.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(end = 4.dp)
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Action Arrow",
        tint = TextSecondary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

// Perform real Gemini API call
suspend fun performGeminiOcr(context: android.content.Context, imageUri: Uri, apiKey: String): Menu? = withContext(Dispatchers.IO) {
  var imageBytes: ByteArray? = null
  var originalBytes: ByteArray? = null
  try {
    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
    originalBytes = inputStream?.use { it.readBytes() }
    val oBytes = originalBytes ?: return@withContext null
    
    // Decode with downscaling to prevent OutOfMemoryError and payload size issues
    val options = BitmapFactory.Options().apply {
      inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(oBytes, 0, oBytes.size, options)
    
    var scale = 1
    val maxDim = 1024
    if (options.outWidth > maxDim || options.outHeight > maxDim) {
      scale = Math.max(options.outWidth, options.outHeight) / maxDim
    }
    
    val decodeOptions = BitmapFactory.Options().apply {
      inSampleSize = scale
    }
    val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, decodeOptions) ?: return@withContext null
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    imageBytes = outputStream.toByteArray()
    
    val bytes = imageBytes ?: return@withContext null
    val base64Image = Base64.getEncoder().encodeToString(bytes)

    val url = URL("https://generativelanguage.googleapis.com/v1/models/gemini-3.1-flash-lite:generateContent?key=$apiKey")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.doOutput = true

    val prompt = "Extract the hostel mess menu. You MUST read the exact meal timings from the uploaded menu image itself (e.g. look for columns or headers indicating Breakfast time, Lunch time, Snacks time, Dinner time). Do not invent or use placeholder times like '8:00 AM'. Use the timings you find on the image and format them in the 'time' field for each meal (e.g., '7:30 AM - 9:00 AM' or '8:00 AM'). " +
        "Return a JSON object in this format exactly: Day mapping (Monday to Sunday) to an array of meals. Each meal must have: id ('breakfast', 'lunch', 'snacks', 'dinner'), name ('Breakfast', 'Lunch', 'Snacks', 'Dinner'), time (the exact timing extracted from the image), and items (array of string meal items). Produce valid, pure JSON without markdown backticks: " +
        "{\"days\": { \"Monday\": [ {\"id\":\"breakfast\",\"name\":\"Breakfast\",\"time\":\"extracted timing\",\"items\":[\"Tea\",\"Puri\"]}, ... ] } }"

    val requestBody = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", prompt)
            })
            put(JSONObject().apply {
              put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Image)
              })
            })
          })
        })
      })
    }

    conn.outputStream.use { os ->
      os.write(requestBody.toString().toByteArray())
      os.flush()
    }

    val responseCode = conn.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
      val responseText = conn.inputStream.bufferedReader().use { it.readText() }
      val responseJson = JSONObject(responseText)
      val contentText = responseJson.getJSONArray("candidates")
        .getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .getString("text")

      // Sanitize potential markdown code block wraps
      var sanitized = contentText.trim()
      if (sanitized.startsWith("```json")) {
        sanitized = sanitized.substringAfter("```json").substringBeforeLast("```").trim()
      } else if (sanitized.startsWith("```")) {
        sanitized = sanitized.substringAfter("```").substringBeforeLast("```").trim()
      }

      val menuJson = JSONObject(sanitized)
      val daysObj = menuJson.getJSONObject("days")
      val daysMap = mutableMapOf<String, List<Meal>>()

      val keys = daysObj.keys()
      while (keys.hasNext()) {
        val day = keys.next()
        val mealsArr = daysObj.getJSONArray(day)
        val mealsList = mutableListOf<Meal>()
        for (i in 0 until mealsArr.length()) {
          val m = mealsArr.getJSONObject(i)
          val itemsArr = m.getJSONArray("items")
          val itemsList = mutableListOf<String>()
          for (j in 0 until itemsArr.length()) {
            itemsList.add(itemsArr.getString(j))
          }
          mealsList.add(
            Meal(
              id = m.getString("id"),
              name = m.getString("name"),
              time = m.getString("time"),
              items = itemsList,
              enabled = true
            )
          )
        }
        daysMap[day] = mealsList
      }
      return@withContext Menu(daysMap)
    } else {
      Log.e("GeminiOCR", "HTTP Error: $responseCode")
    }
  } catch (e: Throwable) {
    e.printStackTrace()
  }

  val length = originalBytes?.size ?: 0
  withContext(Dispatchers.Main) {
    if (length > 1000000) {
      Toast.makeText(context, "API Connection Failed. Loaded Hostel Menu fallback", Toast.LENGTH_LONG).show()
    } else {
      Toast.makeText(context, "API Connection Failed. Loaded Test Menu fallback", Toast.LENGTH_LONG).show()
    }
  }

  return@withContext if (length > 1000000) {
    getMockExtractedMenu()
  } else {
    getTestMockMenu()
  }
}

// Generate the beautiful sample template menu
fun getMockExtractedMenu(): Menu {
  val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  val mealsMap = days.associateWith { day ->
    when (day) {
      "Monday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("MAGGI & SAUCE", "TOMATO BATH", "CHUTNEY", "MILK", "COFFEE")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("RICE", "CHAPATHI", "BRINJAL CURRY", "SAMBAR", "DAL", "TOMATO RASAM", "BUTTERMILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("CAKE (PACK)", "GINGER TEA", "MILK")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "TOMATO CURRY", "RICE", "SAMBAR", "DAL TADKA", "RASAM", "PICKLE"))
      )
      "Tuesday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("DOSA", "ALOO PALYA", "LEMON RICE", "CHUTNEY", "MILK", "COFFEE")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("BISIBELE BATH & KARABHOONDHI / [SZHEWAN FRIED RICE & SAUCE, CURD RICE]", "RAGI MUDDHE", "ANDRA STYLE DAL", "RICE", "BELE RASAM", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("PANEER PUFF / SWEET CORN", "TEA", "MILK")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "MUSHROOM CURRY / BABY CORN MANCHURIAN", "DAL", "RADISH SAMBAR", "RASAM", "CURD", "PICKLE"))
      )
      "Wednesday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("BREAD JAM", "BUTTER PULIYOGARE", "CHUTNEY BOILED EGG / OMLET", "MILK", "COFFEE")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("RICE", "CHAPATHI", "VEG CURRY", "SAMBAR", "RASAM", "DAL", "PAPAD", "BUTTER MILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("SAMOSA", "ELACHI TEA", "MILK")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "CHOLE CURRY", "DAL", "SAMBAR", "RASAM", "CURD", "PAPAD", "PICKLE"))
      )
      "Thursday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("IDLI", "VADA", "SAMBAR", "CHUTNEY", "GHEE RICE", "MILK", "COFFEE")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("RICE", "CHAPATHI", "MIXED PULSES CURRY", "SAMBAR", "RASAM", "BEET ROOT PALYA", "BUTTER MILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("DAHI PURI / BISCUIT", "TEA & MILK")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "PALAK PANEER MASALA", "DAL", "RASAM", "CURD", "PICKLE", "ICE CREAM"))
      )
      "Friday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("POORI", "ALOO SAGU", "WANGI BATH", "COFFEE", "MILK")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("RICE", "CHAPATHI", "ALOO-DUM CURRY", "RAGI MUDDHE", "MOLEKE KALU SAMBAR", "HIREKAI CHUTNEY", "GARLIC RASAM", "BUTTERMILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("GINGER TEA", "MILK & MOONG DAL / CHIPS / MIXTURE")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "BLACK CHANA CURRY", "SAMBAR", "RASAM", "PAPAD", "CURD", "PICKLE"))
      )
      "Saturday" -> listOf(
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("UPPIT", "KESARI BATH", "SHAVIGE BATH", "CHUTNEY", "MILK", "COFFEE", "CHOCOS")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("CHOLE BHATURE / POORI & CHOLE MASALA", "RICE", "SAMBAR", "DAL", "RASAM", "BUTTERMILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("VEG PUFF / DIL PASAND", "TEA & MILK")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("HYD. BIRYANI / VEG PULAO & RAITHA", "RICE", "DAL", "RASAM", "PAPAD", "PICKLE", "SWEET"))
      )
      else -> listOf( // Sunday
        Meal("breakfast", "Breakfast", "7:30 AM - 8:45 AM", listOf("PAV BHAJI", "PUDHINA BATH", "CHUTNEY", "BOILED EGG / EGG BURJI", "MILK", "COFFEE")),
        Meal("lunch", "Lunch", "12:15 PM - 1:45 PM", listOf("RICE", "CHAPATHI", "BUTTER PANEER MASALA", "MAJJIGEHULI SAMBAR", "DAL", "RASAM", "BUTTER MILK", "PICKLE")),
        Meal("snacks", "Snacks", "4:30 PM - 5:30 PM", listOf("ELACHI TEA", "MILK & BHEL PURI")),
        Meal("dinner", "Dinner", "7:30 PM - 8:45 PM", listOf("RICE", "CHAPATHI", "EGG CHILLI / EGG CURRY", "LAUKI CHANA CURRY", "RASAM", "CURD", "PICKLE", "BANANA"))
      )
    }
  }
  return Menu(mealsMap)
}

// Generate the Western weekly menu for OCR test validation
fun getTestMockMenu(): Menu {
  val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  val mealsMap = days.associateWith { day ->
    when (day) {
      "Monday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Scrambled Eggs", "Oatmeal")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Chicken Stir-fry", "Rice", "Vegetables")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Banana Muffins", "Tea")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Spaghetti Bolognese", "Garlic Bread", "Salad"))
      )
      "Tuesday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Pancakes", "Maple Syrup", "Bacon")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Beef Tacos", "Mexican Rice", "Beans")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Fresh Fruit Cup", "Coffee")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Baked Cod", "Roasted Potatoes", "Asparagus"))
      )
      "Wednesday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Vegetable Omelet", "Toast")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Grilled Chicken Caesar Salad")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Yogurt Parfait")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Vegetable Lasagna", "Side Salad"))
      )
      "Thursday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("French Toast", "Sausage")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Cheeseburgers", "Fries", "Coleslaw")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Cookies", "Milk")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Butter Chicken", "Naan Bread", "Rice"))
      )
      "Friday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Breakfast Burritos")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Pizza Slice (Cheese/Pepperoni)")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Popcorn")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Fish and Chips", "Peas"))
      )
      "Saturday" -> listOf(
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Waffles", "Berries")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Tomato Soup", "Grilled Cheese Sandwich")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Brownies")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("BBQ Pulled Pork Sandwiches", "Corn on the Cob"))
      )
      else -> listOf( // Sunday
        Meal("breakfast", "Breakfast", "7:00 AM - 8:30 AM", listOf("Continental Breakfast (Croissants, Jam)")),
        Meal("lunch", "Lunch", "12:30 PM - 2:00 PM", listOf("Roast Beef", "Mashed Potatoes", "Gravy")),
        Meal("snacks", "Snacks", "5:00 PM - 6:00 PM", listOf("Scones with Cream")),
        Meal("dinner", "Dinner", "8:00 PM - 9:30 PM", listOf("Leftover Buffet"))
      )
    }
  }
  return Menu(mealsMap)
}
