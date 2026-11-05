package com.example.messmate.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messmate.data.DataRepository
import com.example.messmate.data.Meal
import com.example.messmate.notifications.AlarmScheduler
import com.example.messmate.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayMenuScreen(
  onBack: () -> Unit,
  repository: DataRepository,
  modifier: Modifier = Modifier
) {
  val menuState by repository.menu.collectAsState()
  val settingsState by repository.settings.collectAsState()
  val scope = rememberCoroutineScope()
  val context = androidx.compose.ui.platform.LocalContext.current
  val alarmScheduler = remember { AlarmScheduler(context) }

  // Get current day of the week
  val currentDay = remember {
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    when (dayOfWeek) {
      Calendar.MONDAY -> "Monday"
      Calendar.TUESDAY -> "Tuesday"
      Calendar.WEDNESDAY -> "Wednesday"
      Calendar.THURSDAY -> "Thursday"
      Calendar.FRIDAY -> "Friday"
      Calendar.SATURDAY -> "Saturday"
      Calendar.SUNDAY -> "Sunday"
      else -> "Monday"
    }
  }

  // Get today's meals
  val todayMeals = menuState.days[currentDay] ?: emptyList()

  // Format today's date
  val formattedDate = remember {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    sdf.format(Date())
  }

  Scaffold(
    containerColor = WarmBackground,
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(OrangeStart, OrangeEnd)))
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
              text = "Today's Menu",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = formattedDate,
              fontSize = 14.sp,
              color = Color.White.copy(alpha = 0.8f)
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
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (settingsState.menuUploadedAt == null || todayMeals.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppWhite),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorder)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No Menu Uploaded",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Please go to Settings to upload your hostel mess menu image.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        items(todayMeals) { meal ->
          MealCard(
            meal = meal,
            onToggle = { isEnabled ->
              val updatedMeals = todayMeals.map {
                if (it.id == meal.id) it.copy(enabled = isEnabled) else it
              }
              val updatedDays = menuState.days.toMutableMap()
              updatedDays[currentDay] = updatedMeals
              repository.updateMenu(menuState.copy(days = updatedDays))
              
              // Reschedule notifications
              alarmScheduler.rescheduleAll(repository)
            }
          )
        }

        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2E6)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFFD1A9))
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Reminders Active",
                tint = BreakfastAccent,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Meal Reminders Active",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = BreakfastAccent
                )
                Text(
                  text = "You'll be notified before each enabled meal. Toggle individual meals above to customize.",
                  fontSize = 13.sp,
                  color = Color(0xFF8A542C),
                  lineHeight = 18.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealCard(
  meal: Meal,
  onToggle: (Boolean) -> Unit
) {
  val (cardBg, accentColor) = when (meal.id) {
    "breakfast" -> Pair(BreakfastBg, BreakfastAccent)
    "lunch" -> Pair(LunchBg, LunchAccent)
    "snacks" -> Pair(SnacksBg, SnacksAccent)
    "dinner" -> Pair(DinnerBg, DinnerAccent)
    else -> Pair(BreakfastBg, BreakfastAccent)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column {
      // Header strip matching meal type
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(cardBg)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(accentColor)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = meal.name,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accentColor
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = meal.time,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }
        Switch(
          checked = meal.enabled,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accentColor,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.LightGray
          )
        )
      }

      // Meal items capsule list
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          meal.items.forEach { item ->
            Box(
              modifier = Modifier
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Text(
                text = item,
                fontSize = 14.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        if (!meal.enabled) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.NotificationsOff,
              contentDescription = "Disabled",
              tint = Color(0xFFC25C5C),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Notification disabled for this meal",
              fontSize = 12.sp,
              color = Color(0xFFC25C5C),
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

