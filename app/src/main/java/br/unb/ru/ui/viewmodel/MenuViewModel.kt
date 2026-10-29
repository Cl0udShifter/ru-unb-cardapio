package br.unb.ru.ui.viewmodel

import android.app.Application
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
        determineTodayAndMeal()
        loadMenu()
    }

    private fun determineTodayAndMeal() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert to 0=Segunda ... 6=Domingo
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
        val initialMeal = when {
            hour < 9 || (hour == 9 && minute <= 30) -> MealType.CAFE
            hour < 14 || (hour == 14 && minute <= 30) -> MealType.ALMOCO
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
