package com.familyexpenses.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState.value)
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Base lista para Fase 1",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Compose, Room, Hilt, Navigation y WorkManager ya estan conectados.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(uiState.accountNames) { accountName ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = accountName,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
