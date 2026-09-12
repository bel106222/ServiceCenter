package ru.bel.servicecenter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.bel.servicecenter.ui.navigation.AppNavigation
import ru.bel.servicecenter.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем Compose-содержимое с темой и навигацией
        setContent {
            AppTheme {
                AppNavigation(onExit = { finish() })
            }
        }
    }
}