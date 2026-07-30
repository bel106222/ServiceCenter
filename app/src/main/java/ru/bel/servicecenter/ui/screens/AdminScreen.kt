package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.AdminController

/**
 * Экран административных функций.
 * Содержит кнопку очистки и пересоздания базы данных (требуется пароль).
 */
@Composable
fun AdminScreen(
    adminController: AdminController = viewModel(),
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    val message by adminController.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Панель администратора", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль администратора") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { adminController.clearAndReseedDatabase(password) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Очистить и пересоздать БД")
        }

        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it)
        }

        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack) {
            Text("Назад")
        }
    }
}