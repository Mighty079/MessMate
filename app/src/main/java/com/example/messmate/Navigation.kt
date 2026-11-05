package com.example.messmate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.messmate.data.DataRepository
import com.example.messmate.ui.home.HomeScreen
import com.example.messmate.ui.menu.TodayMenuScreen
import com.example.messmate.ui.menu.WeeklyMenuScreen
import com.example.messmate.ui.routine.MyRoutineScreen
import com.example.messmate.ui.settings.SettingsScreen

@Composable
fun MainNavigation(repository: DataRepository) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          HomeScreen(
            onNavigate = { key -> backStack.add(key) },
            repository = repository,
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<TodayMenu> {
          TodayMenuScreen(
            onBack = { backStack.removeLastOrNull() },
            repository = repository,
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<WeeklyMenu> {
          WeeklyMenuScreen(
            onBack = { backStack.removeLastOrNull() },
            repository = repository,
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<MyRoutine> {
          MyRoutineScreen(
            onBack = { backStack.removeLastOrNull() },
            repository = repository,
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            repository = repository,
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
