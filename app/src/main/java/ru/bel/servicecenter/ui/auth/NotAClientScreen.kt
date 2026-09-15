package ru.bel.servicecenter.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран-предупреждение для пользователя, который ещё не привязан к клиенту.
 * Показывается один раз при регистрации или при входе, если client_id пуст.
 */
@Composable
fun NotAClientScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Вы пока не являетесь нашим клиентом",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Чтобы продолжить, выберите уже существующего клиента " +
                    "(если ваша компания уже обслуживается в нашем сервисном центре) " +
                    "или создайте нового (например, если вы частное лицо или " +
                    "ваша компания только начинает сотрудничество с нами).",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }
    }
}