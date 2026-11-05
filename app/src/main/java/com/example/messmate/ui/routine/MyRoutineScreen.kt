package com.example.messmate.ui.routine

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messmate.data.DataRepository
import com.example.messmate.data.Routine
import com.example.messmate.notifications.AlarmScheduler
import com.example.messmate.theme.*
import java.util.*

@Composable
fun MyRoutineScreen(
  onBack: () -> Unit,
  repository: DataRepository,
  modifier: Modifier = Modifier
) {
  val routinesState by repository.routines.collectAsState()
  val context = LocalContext.current
  val alarmScheduler = remember { AlarmScheduler(context) }

  var showDialog by remember { mutableStateOf(false) }
  var editingRoutine by remember { mutableStateOf<Routine?>(null) }

  val activeCount = routinesState.count { it.enabled }
  val totalCount = routinesState.size

  Scaffold(
    containerColor = WarmBackground,
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(BlueStart, BlueEnd)))
          .statusBarsPadding()
          .padding(vertical = 20.dp, horizontal = 16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
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
                text = "My Routine",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "$activeCount active • $totalCount total",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
              )
            }
          }
          
          Button(
            onClick = {
              editingRoutine = null
              showDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add",
              tint = BlueStart,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Add",
              color = BlueStart,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = modifier
        .padding(paddingValues)
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
      if (routinesState.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppWhite),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder)
          ) {
            Column(
              modifier = Modifier.padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No Routines Scheduled",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Add routine items like study, exercises, or lectures to receive timely offline reminders.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        itemsIndexed(routinesState) { index, routine ->
          RoutineCard(
            routine = routine,
            index = index,
            onToggle = { isEnabled ->
              val updated = routine.copy(enabled = isEnabled)
              repository.editRoutine(updated)
              alarmScheduler.rescheduleAll(repository)
            },
            onEdit = {
              editingRoutine = routine
              showDialog = true
            },
            onDelete = {
              repository.deleteRoutine(routine.id)
              alarmScheduler.rescheduleAll(repository)
              Toast.makeText(context, "Routine deleted", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }

    if (showDialog) {
      RoutineDialog(
        routine = editingRoutine,
        onDismiss = { showDialog = false },
        onSave = { title, start, end, days, reminder, notes ->
          if (editingRoutine == null) {
            // New routine
            val newRoutine = Routine(
              id = UUID.randomUUID().toString(),
              title = title,
              startTime = start,
              endTime = if (end.isEmpty()) null else end,
              repeatDays = days,
              reminderTime = reminder,
              notes = if (notes.isEmpty()) null else notes,
              enabled = true
            )
            repository.addRoutine(newRoutine)
          } else {
            // Edit existing
            val updated = editingRoutine!!.copy(
              title = title,
              startTime = start,
              endTime = if (end.isEmpty()) null else end,
              repeatDays = days,
              reminderTime = reminder,
              notes = if (notes.isEmpty()) null else notes
            )
            repository.editRoutine(updated)
          }
          alarmScheduler.rescheduleAll(repository)
          showDialog = false
        }
      )
    }
  }
}

@Composable
fun RoutineCard(
  routine: Routine,
  index: Int,
  onToggle: (Boolean) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  // Loop through different capsule border/text colors based on index for variety
  val activeColor = when (index % 4) {
    0 -> BlueStart
    1 -> OrangeStart
    2 -> SnacksAccent
    else -> Color(0xFFE91E63) // Pink
  }
  
  val inactiveBg = CapsuleInactiveBg
  val inactiveText = CapsuleInactiveText

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(activeColor)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = routine.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary
          )
        }
        Switch(
          checked = routine.enabled,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = activeColor,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.LightGray
          )
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      
      // Timings
      Text(
        text = "${routine.startTime} – ${routine.endTime ?: "No End Time"}",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 22.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Repeat Days Caps List
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val allWeekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        allWeekdays.forEach { dayAbbr ->
          val isActive = routine.repeatDays.contains(dayAbbr)
          val capsuleBg = if (isActive) activeColor.copy(alpha = 0.15f) else inactiveBg
          val capsuleText = if (isActive) activeColor else inactiveText
          
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(capsuleBg)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = dayAbbr,
              fontSize = 12.sp,
              color = capsuleText,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Notes
      routine.notes?.let { note ->
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 22.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Notes",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = note,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = CardBorder)
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Reminder Info
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Reminder",
            tint = OrangeStart,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Reminder at ${routine.reminderTime}",
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = CapsuleInactiveBg),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Edit", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEF)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Delete", color = Color(0xFFD6384B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun RoutineDialog(
  routine: Routine?,
  onDismiss: () -> Unit,
  onSave: (title: String, start: String, end: String, days: List<String>, reminder: String, notes: String) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(routine?.title ?: "") }
  var startTime by remember { mutableStateOf(routine?.startTime ?: "8:00 AM") }
  var endTime by remember { mutableStateOf(routine?.endTime ?: "10:00 AM") }
  var repeatDays by remember { mutableStateOf(routine?.repeatDays ?: listOf("Mo", "Tu", "We", "Th", "Fr")) }
  var reminderOffsetMinutes by remember { mutableStateOf(5) } // Default 5 mins before
  var notes by remember { mutableStateOf(routine?.notes ?: "") }

  // Auto-calculate reminder time based on start time and offset
  val calculatedReminderTime = remember(startTime, reminderOffsetMinutes) {
    calculateReminder(startTime, reminderOffsetMinutes)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (routine == null) "Add Routine" else "Edit Routine",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title (e.g. Study DSA)") },
          placeholder = { Text("Enter routine title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Time pickers
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .clickable {
                showTimePickerDialog(context, startTime) { selectedTime ->
                  startTime = selectedTime
                }
              }
              .padding(14.dp)
          ) {
            Column {
              Text("Start Time", fontSize = 11.sp, color = TextSecondary)
              Text(startTime, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .clickable {
                showTimePickerDialog(context, endTime) { selectedTime ->
                  endTime = selectedTime
                }
              }
              .padding(14.dp)
          ) {
            Column {
              Text("End Time", fontSize = 11.sp, color = TextSecondary)
              Text(endTime, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Repeat Days Selector
        Text("Repeat Days", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val allWeekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
          allWeekdays.forEach { day ->
            val isSelected = repeatDays.contains(day)
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) BlueStart else CapsuleInactiveBg)
                .clickable {
                  repeatDays = if (isSelected) {
                    repeatDays - day
                  } else {
                    repeatDays + day
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = day,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextPrimary
              )
            }
          }
        }

        // Notification timing before start
        Text("Reminder Offset", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val offsets = listOf(
            Pair("At start", 0),
            Pair("5m before", 5),
            Pair("10m before", 10),
            Pair("15m before", 15)
          )
          offsets.forEach { (label, minutes) ->
            val isSelected = reminderOffsetMinutes == minutes
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) OrangeStart.copy(alpha = 0.2f) else CapsuleInactiveBg)
                .border(
                  1.dp,
                  if (isSelected) OrangeStart else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
                .clickable { reminderOffsetMinutes = minutes }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) OrangeStart else TextPrimary
              )
            }
          }
        }

        // Calculated reminder show
        Text(
          text = "Notification will trigger at $calculatedReminderTime",
          fontSize = 12.sp,
          color = OrangeStart,
          fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (optional)") },
          placeholder = { Text("e.g. Bring notes from yesterday") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.trim().isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
          } else if (repeatDays.isEmpty()) {
            Toast.makeText(context, "Please select at least one repeat day", Toast.LENGTH_SHORT).show()
          } else {
            onSave(title, startTime, endTime, repeatDays, calculatedReminderTime, notes)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

// TimePicker dialog triggering helper
fun showTimePickerDialog(context: android.content.Context, currentTime: String, onTimeSelected: (String) -> Unit) {
  val clean = currentTime.trim().uppercase()
  val regex = Regex("""(\d+):(\d+)\s*(AM|PM)""")
  val match = regex.find(clean)
  var hour = 8
  var minute = 0
  if (match != null) {
    hour = match.groupValues[1].toInt()
    minute = match.groupValues[2].toInt()
    val ampm = match.groupValues[3]
    if (ampm == "PM" && hour < 12) hour += 12
    if (ampm == "AM" && hour == 12) hour = 0
  }
  
  TimePickerDialog(
    context,
    { _, h, m ->
      val ampm = if (h >= 12) "PM" else "AM"
      val dispHour = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
      }
      val minStr = if (m < 10) "0$m" else "$m"
      onTimeSelected("$dispHour:$minStr $ampm")
    },
    hour,
    minute,
    false
  ).show()
}

// Reminder time calculation offset helper
fun calculateReminder(startTime: String, offsetMinutes: Int): String {
  val clean = startTime.trim().uppercase()
  val regex = Regex("""(\d+):(\d+)\s*(AM|PM)""")
  val match = regex.find(clean) ?: return startTime
  var hour = match.groupValues[1].toInt()
  val minute = match.groupValues[2].toInt()
  val ampm = match.groupValues[3]
  if (ampm == "PM" && hour < 12) hour += 12
  if (ampm == "AM" && hour == 12) hour = 0

  val calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    add(Calendar.MINUTE, -offsetMinutes)
  }

  val h = calendar.get(Calendar.HOUR_OF_DAY)
  val m = calendar.get(Calendar.MINUTE)
  val dispAmpm = if (h >= 12) "PM" else "AM"
  val dispHour = when {
    h == 0 -> 12
    h > 12 -> h - 12
    else -> h
  }
  val minStr = if (m < 10) "0$m" else "$m"
  return "$dispHour:$minStr $dispAmpm"
}
