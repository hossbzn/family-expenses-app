package com.familyexpenses.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.familyexpenses.app.feature.addtransaction.AddTransactionRoute
import com.familyexpenses.app.feature.addtransaction.AddTransactionViewModel
import com.familyexpenses.app.feature.dashboard.DashboardRoute
import com.familyexpenses.app.feature.history.HistoryRoute

@Composable
fun FamilyExpensesNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
    ) {
        composable("dashboard") {
            DashboardRoute(
                onAddExpenseClick = {
                    navController.navigate("add-transaction?familyPaidFromPersonal=false")
                },
                onAddFamilyPaidWithPersonalClick = {
                    navController.navigate("add-transaction?familyPaidFromPersonal=true")
                },
                onHistoryClick = {
                    navController.navigate("history")
                },
            )
        }
        composable(
            route = "add-transaction?familyPaidFromPersonal={familyPaidFromPersonal}",
            arguments = listOf(
                navArgument(AddTransactionViewModel.FAMILY_PAID_FROM_PERSONAL_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            AddTransactionRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable("history") {
            HistoryRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
