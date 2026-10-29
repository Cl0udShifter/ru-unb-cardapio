package br.unb.ru.ui.viewmodel

import android.app.Application
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unb.ru.data.model.DailyMenu
import br.unb.ru.data.model.MealType
import br.unb.ru.data.model.WeeklyMenu
import br.unb.ru.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class MenuUiState(
    val isLoading: Boolean = true,
    val menu: WeeklyMenu? = null,
    val selectedDayIndex: Int = 0,
    val selectedMeal: MealType = MealType.ALMOCO,
    val todayIndex: Int = -1,
    val isDarkTheme: Boolean = false,
    val errorMessage: String? = null
) {
    val currentDay: DailyMenu?
        get() = menu?.dias?.getOrNull(selectedDayIndex)
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MenuRepository(application)

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        // 1. Carregar preferência salva de tema (ou herdar do sistema)
        val savedTheme = repository.getSavedThemePreference()
        val initialDark = if (savedTheme != null) {
            savedTheme
        } else {
            val currentNightMode = application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }

        _uiState.update { it.copy(isDarkTheme = initialDark) }

        // 2. Determinar dia e refeição conforme horário exato do RU
        determineTodayAndMeal()
        loadMenu()
    }

    private fun determineTodayAndMeal() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val todayIdx = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        // Horários do RU:
        // Café: 07h00 às 09h30 (encerra às 9h30 = 570 min)
        // Almoço: 11h00 às 14h30 (encerra às 14h30 = 870 min)
        // Jantar: 17h00 às 19h30 (encerra às 19h30 = 1170 min)
        val initialMeal = when {
            timeInMinutes <= 9 * 60 + 30 -> MealType.CAFE
            timeInMinutes <= 14 * 60 + 30 -> MealType.ALMOCO
            else -> MealType.JANTAR
        }

        _uiState.update {
            it.copy(
                selectedDayIndex = todayIdx,
                todayIndex = todayIdx,
                selectedMeal = initialMeal
            )
        }
    }

    fun loadMenu() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getMenu()
            result.onSuccess { weeklyMenu ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        menu = weeklyMenu,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Erro desconhecido ao carregar cardápio"
                    )
                }
            }
        }
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = newTheme) }
        repository.saveThemePreference(newTheme)
    }

    fun selectDay(index: Int) {
        val totalDays = _uiState.value.menu?.dias?.size ?: 7
        if (index in 0 until totalDays) {
            _uiState.update { it.copy(selectedDayIndex = index) }
        }
    }

    fun selectMeal(meal: MealType) {
        _uiState.update { it.copy(selectedMeal = meal) }
    }

    fun goToToday() {
        val today = _uiState.value.todayIndex
        if (today in 0..6) {
            selectDay(today)
        }
    }
}
