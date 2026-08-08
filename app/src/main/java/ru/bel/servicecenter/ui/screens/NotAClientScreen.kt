package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotAClientScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Вы пока не являетесь нашим клиентом!",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Вы можете выбрать уже существующего (если ваша компания уже является нашим клиентом) или создать нового (например, если вы являетесь частным лицом или ваша компания только начинает сотрудничество с сервисным центром).",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Продолжить")
        }
    }
}