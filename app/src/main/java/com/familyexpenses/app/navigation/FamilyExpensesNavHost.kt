package com.familyexpenses.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.feature.account.AccountDetailRoute
import com.familyexpenses.app.feature.account.AccountDetailViewModel
import com.familyexpenses.app.feature.addtransaction.AddTransactionRoute
import com.familyexpenses.app.feature.addtransaction.AddTransactionViewModel
import com.familyexpenses.app.feature.advances.AdvancesRoute
import com.familyexpenses.app.feature.analysis.AnalysisRoute
import com.familyexpenses.app.feature.categories.CategoriesRoute
import com.familyexpenses.app.feature.dashboard.DashboardRoute
import com.familyexpenses.app.feature.expense.AddExpenseRoute
import com.familyexpenses.app.feature.history.HistoryRoute
import com.familyexpenses.app.feature.history.HistoryViewModel
import com.familyexpenses.app.feature.history.MovementDetailRoute
import com.familyexpenses.app.feature.history.MovementDetailViewModel
import com.familyexpenses.app.feature.home.HomeRoute
import com.familyexpenses.app.feature.income.AddIncomeRoute
import com.familyexpenses.app.feature.recurrence.RecurrenceRoute
import com.familyexpenses.app.feature.settings.AccountsSettingsRoute
import com.familyexpenses.app.feature.settings.DataSecurityRoute
import com.familyexpenses.app.feature.settings.SettingsRoute
import com.familyexpenses.app.feature.transfer.TransferRoute
import com.familyexpenses.app.feature.transfer.TransferViewModel

@Composable
fun FamilyExpensesNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") { backStackEntry ->
            val savedTxId = backStackEntry.savedStateHandle.get<String>("saved_transaction_id")
            HomeRoute(
                onFamilyAccountClick = {
                    navController.navigate("account-detail/${DatabaseSeeder.FAMILY_ACCOUNT_ID}")
                },
                onPersonalAccountClick = {
                    navController.navigate("account-detail/${DatabaseSeeder.PERSONAL_ACCOUNT_ID}")
                },
                onAddExpenseClick = {
                    navController.navigate("add-expense")
                },
                onAddIncomeClick = {
                    navController.navigate("add-income")
                },
                onPendingSettlementClick = {
                    navController.navigate("pending-settlement")
                },
                onHistoryClick = {
                    navController.navigate("history") {
                        launchSingleTop = true
                    }
                },
                onAnalysisClick = {
                    navController.navigate("analysis") {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                savedTransactionId = savedTxId,
                onClearSavedTransactionId = {
                    backStackEntry.savedStateHandle.remove<String>("saved_transaction_id")
                },
            )
        }
        composable("dashboard") { backStackEntry ->
            val savedTxId = backStackEntry.savedStateHandle.get<String>("saved_transaction_id")
            DashboardRoute(
                onAddPersonalIncomeClick = {
                    navController.navigate("add-income")
                },
                onAddPersonalExpenseClick = {
                    navController.navigate("add-expense")
                },
                onAddFamilyIncomeClick = {
                    navController.navigate("add-income")
                },
                onAddFamilyExpenseClick = {
                    navController.navigate("add-expense")
                },
                onAddFamilyExpensePaidFromPersonalClick = {
                    navController.navigate("add-expense")
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
                savedTransactionId = savedTxId,
                onClearSavedTransactionId = {
                    backStackEntry.savedStateHandle.remove<String>("saved_transaction_id")
                },
            )
        }
        composable("add-expense") {
            AddExpenseRoute(
                onNavigateBack = { navController.popBackStack() },
                onExpenseSaved = { transactionId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("saved_transaction_id", transactionId)
                    navController.popBackStack()
                },
            )
        }
        composable("add-income") {
            AddIncomeRoute(
                onNavigateBack = { navController.popBackStack() },
                onIncomeSaved = { transactionId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("saved_transaction_id", transactionId)
                    navController.popBackStack()
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
        composable(
            route = "history?${HistoryViewModel.INITIAL_ACCOUNT_ID_ARG}={${HistoryViewModel.INITIAL_ACCOUNT_ID_ARG}}&${HistoryViewModel.INITIAL_CATEGORY_ID_ARG}={${HistoryViewModel.INITIAL_CATEGORY_ID_ARG}}&${HistoryViewModel.INITIAL_TYPE_FILTER_ARG}={${HistoryViewModel.INITIAL_TYPE_FILTER_ARG}}&${HistoryViewModel.INITIAL_PERIOD_TYPE_ARG}={${HistoryViewModel.INITIAL_PERIOD_TYPE_ARG}}&${HistoryViewModel.INITIAL_PERIOD_START_ARG}={${HistoryViewModel.INITIAL_PERIOD_START_ARG}}&${HistoryViewModel.INITIAL_PERIOD_END_ARG}={${HistoryViewModel.INITIAL_PERIOD_END_ARG}}&showBottomBar={showBottomBar}",
            arguments = listOf(
                navArgument(HistoryViewModel.INITIAL_ACCOUNT_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(HistoryViewModel.INITIAL_CATEGORY_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(HistoryViewModel.INITIAL_TYPE_FILTER_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(HistoryViewModel.INITIAL_PERIOD_TYPE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(HistoryViewModel.INITIAL_PERIOD_START_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(HistoryViewModel.INITIAL_PERIOD_END_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("showBottomBar") {
                    type = NavType.BoolType
                    defaultValue = true
                },
            ),
        ) { backStackEntry ->
            val showBottomBar = backStackEntry.arguments?.getBoolean("showBottomBar") ?: true
            HistoryRoute(
                showBottomBar = showBottomBar,
                onBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.popBackStack("home", inclusive = false)
                },
                onAnalysisClick = {
                    navController.navigate("analysis") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onMovementClick = { kind, id ->
                    navController.navigate("movement-detail/${kind.name}/$id")
                },
            )
        }
        composable("analysis") {
            AnalysisRoute(
                onHomeClick = {
                    navController.popBackStack("home", inclusive = false)
                },
                onHistoryClick = {
                    navController.navigate("history") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onDrillDownToHistory = { req ->
                    navController.navigate(
                        "history?" +
                            "${HistoryViewModel.INITIAL_ACCOUNT_ID_ARG}=${req.accountId}&" +
                            "${HistoryViewModel.INITIAL_CATEGORY_ID_ARG}=${req.categoryId}&" +
                            "${HistoryViewModel.INITIAL_TYPE_FILTER_ARG}=${req.typeFilterName}&" +
                            "${HistoryViewModel.INITIAL_PERIOD_TYPE_ARG}=${req.periodTypeName}&" +
                            "${HistoryViewModel.INITIAL_PERIOD_START_ARG}=${req.periodStartIso}&" +
                            "${HistoryViewModel.INITIAL_PERIOD_END_ARG}=${req.periodEndIso}&" +
                            "showBottomBar=false",
                    )
                },
            )
        }
        composable(
            route = "movement-detail/{${MovementDetailViewModel.MOVEMENT_KIND_ARG}}/{${MovementDetailViewModel.MOVEMENT_ID_ARG}}",
            arguments = listOf(
                navArgument(MovementDetailViewModel.MOVEMENT_KIND_ARG) {
                    type = NavType.StringType
                },
                navArgument(MovementDetailViewModel.MOVEMENT_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) {
            MovementDetailRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("pending-settlement") {
            AdvancesRoute(
                onBack = { navController.popBackStack() },
                onReimburseClick = { debtorAccountId ->
                    navController.navigate(
                        "transfer?${TransferViewModel.INITIAL_ACCOUNT_ID_ARG}=$debtorAccountId&${TransferViewModel.INITIAL_TYPE_ARG}=${com.familyexpenses.app.core.model.TransferType.REIMBURSEMENT.name}",
                    )
                },
                onMovementClick = { kind, id ->
                    navController.navigate("movement-detail/${kind.name}/$id")
                },
            )
        }
        composable("recurrence") {
            RecurrenceRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onCategoriesClick = { navController.navigate("categories") },
                onAccountsClick = { navController.navigate("accounts-settings") },
                onRecurrenceClick = { navController.navigate("recurrence") },
                onDataSecurityClick = { navController.navigate("data-security") },
            )
        }
        composable("data-security") {
            DataSecurityRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable("categories") {
            CategoriesRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable("accounts-settings") {
            AccountsSettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "account-detail/{${AccountDetailViewModel.ACCOUNT_ID_ARG}}",
            arguments = listOf(
                navArgument(AccountDetailViewModel.ACCOUNT_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val accountIdArg = backStackEntry.arguments?.getString(AccountDetailViewModel.ACCOUNT_ID_ARG)
                ?: DatabaseSeeder.PERSONAL_ACCOUNT_ID
            val savedTransferId = backStackEntry.savedStateHandle.get<String>("saved_transfer_id")
            AccountDetailRoute(
                onNavigateBack = { navController.popBackStack() },
                onTransferClick = { accountId ->
                    navController.navigate("transfer?${TransferViewModel.INITIAL_ACCOUNT_ID_ARG}=$accountId")
                },
                onMovementsClick = {
                    navController.navigate(
                        "history?${HistoryViewModel.INITIAL_ACCOUNT_ID_ARG}=$accountIdArg&showBottomBar=false",
                    )
                },
                onAdvancesClick = {
                    navController.navigate("pending-settlement")
                },
                savedTransferId = savedTransferId,
                onClearSavedTransferId = {
                    backStackEntry.savedStateHandle.remove<String>("saved_transfer_id")
                },
            )
        }
        composable(
            route = "transfer?${TransferViewModel.INITIAL_ACCOUNT_ID_ARG}={${TransferViewModel.INITIAL_ACCOUNT_ID_ARG}}&${TransferViewModel.INITIAL_TYPE_ARG}={${TransferViewModel.INITIAL_TYPE_ARG}}",
            arguments = listOf(
                navArgument(TransferViewModel.INITIAL_ACCOUNT_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = DatabaseSeeder.PERSONAL_ACCOUNT_ID
                },
                navArgument(TransferViewModel.INITIAL_TYPE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            TransferRoute(
                onNavigateBack = { navController.popBackStack() },
                onTransferSaved = { transferId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("saved_transfer_id", transferId)
                    navController.popBackStack()
                },
            )
        }
    }
}
