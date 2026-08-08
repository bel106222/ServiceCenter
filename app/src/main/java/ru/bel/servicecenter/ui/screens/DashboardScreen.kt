package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DashboardItem(val title: String, val onClick: () -> Unit)

@Composable
fun DashboardScreen(items: List<DashboardItem>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Главное меню", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { item.onClick() }
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}