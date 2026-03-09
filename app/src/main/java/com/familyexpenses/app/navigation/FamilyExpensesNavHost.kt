package com.familyexpenses.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familyexpenses.app.feature.home.HomeRoute

@Composable
fun FamilyExpensesNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeRoute()
        }
    }
}
