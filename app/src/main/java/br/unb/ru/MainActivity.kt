package br.unb.ru

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import br.unb.ru.data.model.MealType
import br.unb.ru.notification.MenuNotificationReceiver
import br.unb.ru.ui.screens.MenuHomeScreen
import br.unb.ru.ui.theme.RUUnBTheme
import br.unb.ru.ui.viewmodel.MenuViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MenuViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        handleIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsState()
            RUUnBTheme(darkTheme = state.isDarkTheme) {
                MenuHomeScreen(
                    state = state,
                    onDaySelected = { viewModel.selectDay(it) },
                    onMealSelected = { viewModel.selectMeal(it) },
                    onGoToToday = { viewModel.goToToday() },
                    onRefresh = { viewModel.loadMenu() },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenSettings = {
                        requestNotificationPermission()
                        viewModel.setSettingsOpen(true)
                    },
                    onCloseSettings = { viewModel.setSettingsOpen(false) },
                    onUpdateNotificationConfig = { viewModel.updateNotificationConfig(it) },
                    onOpenCard = { viewModel.setCardOpen(true) },
                    onCloseCard = { viewModel.setCardOpen(false) },
                    onSaveCard = { viewModel.updateStudentCard(it) },
                    onSyncCardFromSigaa = { mat, pass -> viewModel.syncCardFromSigaa(mat, pass) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val mealKey = intent?.getStringExtra(MenuNotificationReceiver.EXTRA_MEAL_TYPE)
        if (mealKey != null) {
            try {
                val mealType = MealType.valueOf(mealKey)
                viewModel.selectMeal(mealType)
                viewModel.goToToday()
            } catch (_: Exception) {}
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
