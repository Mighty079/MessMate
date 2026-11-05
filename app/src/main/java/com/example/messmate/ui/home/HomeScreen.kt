package com.example.messmate.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.messmate.*
import com.example.messmate.data.DataRepository
import com.example.messmate.data.Meal
import com.example.messmate.data.Routine
import com.example.messmate.data.PERMANENT_GEMINI_API_KEY
import com.example.messmate.notifications.AlarmScheduler
import com.example.messmate.ui.settings.getMockExtractedMenu
import com.example.messmate.ui.settings.getTestMockMenu
import com.example.messmate.ui.settings.performGeminiOcr
import com.example.messmate.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  onNavigate: (NavKey) -> Unit,
  repository: DataRepository,
  modifier: Modifier = Modifier
) {
  val settingsState by repository.settings.collectAsState()
  val menuState by repository.menu.collectAsState()
  val routinesState by repository.routines.collectAsState()
  
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val alarmScheduler = remember { AlarmScheduler(context) }
  var isExtracting by remember { mutableStateOf(false) }

  val notifManager = remember { androidx.core.app.NotificationManagerCompat.from(context) }
  val notificationsEnabled = remember { notifManager.areNotificationsEnabled() }

  // 1. Dynamic Greeting based on time
  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when (hour) {
      in 5..11 -> "Good Morning"
      in 12..16 -> "Good Afternoon"
      in 17..20 -> "Good Evening"
      else -> "Good Night"
    }
  }

  // 2. Day & Date Formatting
  val todayDayOfWeek = remember {
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
  val formattedDate = remember {
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
  }

  // 3. Quick statistics
  val todayMeals = menuState.days[todayDayOfWeek] ?: emptyList()
  val activeRoutines = routinesState.filter { it.enabled }

  // 4. Calculate Next Meal
  val nextMeal = remember(todayMeals) {
    calculateNextMeal(todayMeals)
  }

  // 5. Image Picker for quick header upload
  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      scope.launch {
        isExtracting = true
        try {
          val extractedMenu = performGeminiOcr(context, it, PERMANENT_GEMINI_API_KEY)
          if (extractedMenu != null) {
            repository.updateMenu(extractedMenu)
            val todayStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
            repository.updateSettings(settingsState.copy(menuUploadedAt = "Uploaded $todayStr"))
            alarmScheduler.rescheduleAll(repository)
            Toast.makeText(context, "Menu extracted successfully!", Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          isExtracting = false
        }
      }
    }
  }

  Scaffold(
    containerColor = WarmBackground,
    bottomBar = {
      HomeBottomNavigation(onNavigate = onNavigate)
    }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        // Gradient Header Banner
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(listOf(OrangeStart, OrangeEnd)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
              )
              .statusBarsPadding()
              .padding(bottom = 24.dp, top = 16.dp, start = 16.dp, end = 16.dp)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = formattedDate,
                  fontSize = 13.sp,
                  color = Color.White.copy(alpha = 0.8f),
                  fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                  ) {
                    Icon(
                      imageVector = Icons.Default.PhotoCamera,
                      contentDescription = "Upload Menu",
                      tint = Color.White
                    )
                  }
                  IconButton(
                    onClick = { onNavigate(Settings) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                  ) {
                    Icon(
                      imageVector = Icons.Default.Settings,
                      contentDescription = "Settings",
                      tint = Color.White
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "$greeting, ${settingsState.userName}! 👋",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Here's your day at a glance",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
              )

              Spacer(modifier = Modifier.height(16.dp))

              // Next Meal Card
              NextMealCard(nextMeal = nextMeal, onCardClick = { onNavigate(TodayMenu) })
            }
          }
        }

        // Notification Permission Warning if blocked
        if (!notificationsEnabled) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable {
                  val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                  }
                  context.startActivity(intent)
                }
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
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
                    text = "Meal Notifications are Disabled",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                  )
                  Text(
                    text = "Tap here to turn on notifications so meal reminders can pop up on time.",
                    color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }

        // Quick Grid Actions
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              GridCard(
                icon = Icons.Default.RestaurantMenu,
                title = "Today's Menu",
                subtitle = if (todayMeals.isNotEmpty()) "${todayMeals.size} meals today" else "No meals loaded",
                backgroundColor = Color(0xFFFFF2E6),
                iconColor = OrangeStart,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(TodayMenu) }
              )
              GridCard(
                icon = Icons.Default.CalendarMonth,
                title = "Weekly Menu",
                subtitle = "Browse all days",
                backgroundColor = Color(0xFFEEF2FF),
                iconColor = BlueStart,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(WeeklyMenu) }
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              GridCard(
                icon = Icons.Default.UploadFile,
                title = "Upload Menu",
                subtitle = "Update monthly",
                backgroundColor = Color(0xFFFFFCE5),
                iconColor = LunchAccent,
                modifier = Modifier.weight(1f),
                onClick = { imagePickerLauncher.launch("image/*") }
              )
              GridCard(
                icon = Icons.Default.FormatListBulleted,
                title = "My Routine",
                subtitle = "${activeRoutines.size} active tasks",
                backgroundColor = Color(0xFFEAFBF0),
                iconColor = SnacksAccent,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(MyRoutine) }
              )
            }
          }
        }

        // Today's Menu Preview Section
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionHeader(title = "Today's Menu", actionText = "See all >", onActionClick = { onNavigate(TodayMenu) })
            Spacer(modifier = Modifier.height(8.dp))
            
            if (todayMeals.isEmpty()) {
              PreviewEmptyCard("No meals loaded for today.")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todayMeals.forEach { meal ->
                  PreviewMealRow(meal = meal)
                }
              }
            }
          }
        }

        // My Routine Preview Section
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionHeader(title = "My Routine", actionText = "See all >", onActionClick = { onNavigate(MyRoutine) })
            Spacer(modifier = Modifier.height(8.dp))
            
            val todayRoutines = getRoutinesForDay(routinesState, todayDayOfWeek)
            if (todayRoutines.isEmpty()) {
              PreviewEmptyCard("No routines scheduled for today.")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todayRoutines.forEach { routine ->
                  PreviewRoutineRow(routine = routine)
                }
              }
            }
          }
        }
      }

      // Loader Dialog
      if (isExtracting) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
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
              CircularProgressIndicator(color = OrangeStart)
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Extracting Menu via AI...",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NextMealCard(
  nextMeal: Meal?,
  onCardClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClick),
    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
    shape = RoundedCornerShape(16.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "Next Meal",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "NEXT MEAL",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
          )
          if (nextMeal != null) {
            Text(
              text = nextMeal.name,
              fontSize = 18.sp,
              color = Color.White,
              fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = nextMeal.time,
              fontSize = 13.sp,
              color = Color.White.copy(alpha = 0.85f),
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = nextMeal.items.joinToString(", "),
              fontSize = 13.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          } else {
            Text(
              text = "No upcoming meals",
              fontSize = 16.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      
      if (nextMeal != null) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = "SOON",
            color = OrangeStart,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}

@Composable
fun GridCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  backgroundColor: Color,
  iconColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .height(100.dp)
      .clickable(onClick = onClick)
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(backgroundColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconColor,
          modifier = Modifier.size(18.dp)
        )
      }
      Column {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary
        )
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  actionText: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      color = TextPrimary
    )
    Text(
      text = actionText,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = OrangeStart,
      modifier = Modifier.clickable(onClick = onActionClick)
    )
  }
}

@Composable
fun PreviewEmptyCard(text: String) {
  Card(
    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(12.dp)
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(text = text, fontSize = 13.sp, color = TextSecondary)
    }
  }
}

@Composable
fun PreviewMealRow(meal: Meal) {
  val accentColor = when (meal.id) {
    "breakfast" -> BreakfastAccent
    "lunch" -> LunchAccent
    "snacks" -> SnacksAccent
    else -> DinnerAccent
  }

  Card(
    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(accentColor)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = meal.name,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = TextPrimary,
        modifier = Modifier.width(90.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = meal.items.joinToString(", "),
        fontSize = 13.sp,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
fun PreviewRoutineRow(routine: Routine) {
  Card(
    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
    colors = CardDefaults.cardColors(containerColor = AppWhite),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BlueStart)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = routine.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary
          )
          Text(
            text = "${routine.startTime} - ${routine.endTime ?: "No End Time"}",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }
      Text(
        text = routine.repeatDays.joinToString(", "),
        fontSize = 11.sp,
        color = BlueStart,
        fontWeight = FontWeight.Bold
      )
    }
  }
}



@Composable
fun HomeBottomNavigation(
  onNavigate: (NavKey) -> Unit
) {
  NavigationBar(
    containerColor = AppWhite,
    tonalElevation = 8.dp,
    modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
  ) {
    NavigationBarItem(
      selected = true,
      onClick = { },
      icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
      label = { Text("Home", fontSize = 11.sp) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = OrangeStart,
        selectedTextColor = OrangeStart,
        indicatorColor = OrangeStart.copy(alpha = 0.1f),
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )
    NavigationBarItem(
      selected = false,
      onClick = { onNavigate(TodayMenu) },
      icon = { Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = "Menu") },
      label = { Text("Menu", fontSize = 11.sp) },
      colors = NavigationBarItemDefaults.colors(
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )
    NavigationBarItem(
      selected = false,
      onClick = { onNavigate(WeeklyMenu) },
      icon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Weekly") },
      label = { Text("Weekly", fontSize = 11.sp) },
      colors = NavigationBarItemDefaults.colors(
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )
    NavigationBarItem(
      selected = false,
      onClick = { onNavigate(MyRoutine) },
      icon = { Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = "Routine") },
      label = { Text("Routine", fontSize = 11.sp) },
      colors = NavigationBarItemDefaults.colors(
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )
  }
}

// Calculate upcoming meal from current time
fun calculateNextMeal(meals: List<Meal>): Meal? {
  if (meals.isEmpty()) return null
  
  val now = Calendar.getInstance()
  val currentHour = now.get(Calendar.HOUR_OF_DAY)
  val currentMinute = now.get(Calendar.MINUTE)
  val currentTotalMinutes = currentHour * 60 + currentMinute

  val parsedMeals = meals.mapNotNull { meal ->
    val timePair = parseTimeStr(meal.time) ?: return@mapNotNull null
    val mealTotalMinutes = timePair.first * 60 + timePair.second
    Triple(meal, mealTotalMinutes, meal.enabled)
  }

  // Find the first meal today that is in the future
  val futureMeal = parsedMeals.filter { it.third && it.second > currentTotalMinutes }
    .minByOrNull { it.second }

  if (futureMeal != null) {
    return futureMeal.first
  }

  // If no future meal today, the next meal is the first meal of tomorrow (which has the lowest minutes)
  return parsedMeals.filter { it.third }.minByOrNull { it.second }?.first ?: parsedMeals.firstOrNull()?.first
}

fun parseTimeStr(timeStr: String): Pair<Int, Int>? {
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
    
    return Pair(hour, minute)
  } catch (e: Exception) {
    return null
  }
}

// Filter routines scheduled for today's day of week
fun getRoutinesForDay(routines: List<Routine>, dayOfWeek: String): List<Routine> {
  val abbr = when (dayOfWeek) {
    "Monday" -> "Mo"
    "Tuesday" -> "Tu"
    "Wednesday" -> "We"
    "Thursday" -> "Th"
    "Friday" -> "Fr"
    "Saturday" -> "Sa"
    "Sunday" -> "Su"
    else -> ""
  }
  return routines.filter { it.repeatDays.contains(abbr) }
}
