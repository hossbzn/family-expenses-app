package com.familyexpenses.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.feature.addtransaction.AddTransactionRoute
import com.familyexpenses.app.feature.addtransaction.AddTransactionViewModel
import com.familyexpenses.app.feature.dashboard.DashboardRoute
import com.familyexpenses.app.feature.history.HistoryRoute
import com.familyexpenses.app.feature.recurrence.RecurrenceRoute
import com.familyexpenses.app.feature.settlement.PendingSettlementRoute

@Composable
fun FamilyExpensesNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
    ) {
        composable("dashboard") {
            DashboardRoute(
                onAddPersonalIncomeClick = {
                    navController.navigate(
                        buildAddTransactionRoute(
                            accountId = DatabaseSeeder.PERSONAL_ACCOUNT_ID,
                            type = TransactionType.INCOME,
                        ),
                    )
                },
                onAddPersonalExpenseClick = {
                    navController.navigate(
                        buildAddTransactionRoute(
                            accountId = DatabaseSeeder.PERSONAL_ACCOUNT_ID,
                            type = TransactionType.EXPENSE,
                        ),
                    )
                },
                onAddFamilyIncomeClick = {
                    navController.navigate(
                        buildAddTransactionRoute(
                            accountId = DatabaseSeeder.FAMILY_ACCOUNT_ID,
                            type = TransactionType.INCOME,
                        ),
                    )
                },
                onAddFamilyExpenseClick = {
                    navController.navigate(
                        buildAddTransactionRoute(
                            accountId = DatabaseSeeder.FAMILY_ACCOUNT_ID,
                            type = TransactionType.EXPENSE,
                        ),
                    )
                },
                onAddFamilyExpensePaidFromPersonalClick = {
                    navController.navigate(
                        buildAddTransactionRoute(
                            accountId = DatabaseSeeder.FAMILY_ACCOUNT_ID,
                            type = TransactionType.EXPENSE,
                            familyPaidFromPersonal = true,
                        ),
                    )
                },
                onPendingSettlementClick = {
                    navController.navigate("pending-settlement")
                },
                onRecurringClick = {
                    navController.navigate("recurrence")
                },
                onHistoryClick = {
                    navController.navigate("history")
                },
            )
        }
        composable(
            route = "add-transaction?familyPaidFromPersonal={familyPaidFromPersonal}&initialAccountId={initialAccountId}&initialType={initialType}",
            arguments = listOf(
                navArgument(AddTransactionViewModel.FAMILY_PAID_FROM_PERSONAL_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(AddTransactionViewModel.INITIAL_ACCOUNT_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = DatabaseSeeder.PERSONAL_ACCOUNT_ID
                },
                navArgument(AddTransactionViewModel.INITIAL_TYPE_ARG) {
                    type = NavType.StringType
                    defaultValue = TransactionType.EXPENSE.name
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
        composable("pending-settlement") {
            PendingSettlementRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable("recurrence") {
            RecurrenceRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun buildAddTransactionRoute(
    accountId: String,
    type: TransactionType,
    familyPaidFromPersonal: Boolean = false,
): String = "add-transaction?" +
    "${AddTransactionViewModel.FAMILY_PAID_FROM_PERSONAL_ARG}=$familyPaidFromPersonal&" +
    "${AddTransactionViewModel.INITIAL_ACCOUNT_ID_ARG}=$accountId&" +
    "${AddTransactionViewModel.INITIAL_TYPE_ARG}=${type.name}"
