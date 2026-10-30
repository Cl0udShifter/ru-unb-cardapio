package br.unb.ru.ui.viewmodel

import android.app.Application
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unb.ru.data.model.DailyMenu
import br.unb.ru.data.model.MealType
import br.unb.ru.data.model.StudentCard
import br.unb.ru.data.model.WeeklyMenu
import br.unb.ru.data.repository.MenuRepository
import br.unb.ru.data.repository.NotificationConfig
import br.unb.ru.data.repository.NotificationPreferences
import br.unb.ru.data.repository.StudentCardRepository
import br.unb.ru.notification.NotificationScheduler
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
    val isSettingsOpen: Boolean = false,
    val isCardOpen: Boolean = false,
    val studentCard: StudentCard = StudentCard(),
    val isSyncingCard: Boolean = false,
    val cardSyncError: String? = null,
    val notificationConfig: NotificationConfig = NotificationConfig(),
    val errorMessage: String? = null
) {
    val currentDay: DailyMenu?
        get() = menu?.dias?.getOrNull(selectedDayIndex)
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MenuRepository(application)
    private val notificationPrefs = NotificationPreferences(application)
    private val cardRepository = StudentCardRepository(application)

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        // 1. Carregar preferência salva de tema
        val savedTheme = repository.getSavedThemePreference()
        val initialDark = if (savedTheme != null) {
            savedTheme
        } else {
            val currentNightMode = application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }

        // 2. Carregar configurações de notificação e agendar
        val notifConfig = notificationPrefs.getConfig()
        NotificationScheduler.scheduleAll(application)

        // 3. Carregar dados da carteirinha discente
        val savedCard = cardRepository.getStudentCard()

        _uiState.update {
            it.copy(
                isDarkTheme = initialDark,
                notificationConfig = notifConfig,
                studentCard = savedCard
            )
        }

        // 3. Determinar dia e refeição conforme horário exato do RU
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

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun setCardOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isCardOpen = isOpen, cardSyncError = null) }
    }

    fun updateStudentCard(card: StudentCard) {
        _uiState.update { it.copy(studentCard = card) }
        cardRepository.saveStudentCard(card)
    }

    fun syncCardFromSigaa(matricula: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCard = true, cardSyncError = null) }
            val result = cardRepository.syncFromSigaa(matricula, pass)
            result.fold(
                onSuccess = { card ->
                    _uiState.update { it.copy(isSyncingCard = false, studentCard = card, cardSyncError = null) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSyncingCard = false,
                            cardSyncError = err.message ?: "Erro ao sincronizar com o SIGAA"
                        )
                    }
                }
            )
        }
    }

    fun updateNotificationConfig(config: NotificationConfig) {
        _uiState.update { it.copy(notificationConfig = config) }
        notificationPrefs.saveConfig(config)
        NotificationScheduler.scheduleAll(getApplication())
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
