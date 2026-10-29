package br.unb.ru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.unb.ru.ui.screens.MenuHomeScreen
import br.unb.ru.ui.theme.RUUnBTheme
import br.unb.ru.ui.viewmodel.MenuViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RUUnBTheme {
                val state by viewModel.uiState.collectAsState()
                MenuHomeScreen(
                    state = state,
                    onDaySelected = { viewModel.selectDay(it) },
                    onMealSelected = { viewModel.selectMeal(it) },
                    onGoToToday = { viewModel.goToToday() },
                    onRefresh = { viewModel.loadMenu() }
                )
            }
        }
    }
}
