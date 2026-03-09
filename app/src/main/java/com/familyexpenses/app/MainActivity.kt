package com.familyexpenses.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.familyexpenses.app.navigation.FamilyExpensesNavHost
import com.familyexpenses.app.ui.theme.FamilyExpensesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyExpensesTheme {
                FamilyExpensesNavHost()
            }
        }
    }
}
