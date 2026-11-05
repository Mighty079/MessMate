package com.example.messmate

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object TodayMenu : NavKey
@Serializable data object WeeklyMenu : NavKey
@Serializable data object MyRoutine : NavKey
@Serializable data object Settings : NavKey
