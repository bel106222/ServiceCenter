package ru.bel.servicecenter.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.viewmodels.AdminViewModel

@Composable
fun AdminDatabaseScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var isClearing by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    val message by adminViewModel.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Управление базой данных", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль администратора (для очистки)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isClearing
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isClearing = true
                coroutineScope.launch {
                    adminViewModel.clearAndReseedDatabase(password)
                    isClearing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.length >= 6 && !isClearing
        ) {
            if (isClearing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Очистить и пересоздать БД")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                isChecking = true
                coroutineScope.launch {
                    // Здесь можно добавить проверку целостности, если нужно
                    // Например, вызов DataValidator
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Проверить целостность данных")
            }
        }

        message?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(msg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
}