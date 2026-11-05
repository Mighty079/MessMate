package com.example.messmate.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.messmate.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyMenuScreen(
  onBack: () -> Unit,
  repository: DataRepository,
  modifier: Modifier = Modifier
) {
  val menuState by repository.menu.collectAsState()
  val settingsState by repository.settings.collectAsState()
  
  val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  var selectedDay by remember { mutableStateOf(daysOfWeek[0]) }

  val mealsForDay = menuState.days[selectedDay] ?: emptyList()

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
              text = "Weekly Menu",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Browse meals for the entire week",
              fontSize = 14.sp,
              color = Color.White.copy(alpha = 0.8f)
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .padding(paddingValues)
        .fillMaxSize()
    ) {
      // Day Selector Row
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(daysOfWeek) { day ->
          val isSelected = day == selectedDay
          val chipBg = if (isSelected) PurpleStart else CapsuleInactiveBg
          val chipText = if (isSelected) CapsuleActiveText else TextPrimary
          
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(chipBg)
              .clickable { selectedDay = day }
              .padding(horizontal = 18.dp, vertical = 10.dp)
          ) {
            Text(
              text = day,
              color = chipText,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 14.sp
            )
          }
        }
      }

      // Meal List for Selected Day
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        if (settingsState.menuUploadedAt == null || mealsForDay.isEmpty()) {
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
          items(mealsForDay) { meal ->
            WeeklyMealRow(meal = meal)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyMealRow(meal: Meal) {
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
      // Header strip (no toggle switches on browse weekly menu screen)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(cardBg)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
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
          fontSize = 17.sp,
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
      }
    }
  }
}
