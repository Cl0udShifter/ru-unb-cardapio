package br.unb.ru.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.model.MealType
import br.unb.ru.data.model.StudentCard
import br.unb.ru.data.repository.NotificationConfig
import br.unb.ru.ui.components.BreakfastCard
import br.unb.ru.ui.components.DaySelectorRail
import br.unb.ru.ui.components.LunchDinnerCard
import br.unb.ru.ui.components.MealBottomControl
import br.unb.ru.ui.components.NotificationSettingsBottomSheet
import br.unb.ru.ui.components.StudentCardScreen
import br.unb.ru.ui.theme.NotionTypography
import br.unb.ru.ui.viewmodel.MenuUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuHomeScreen(
    state: MenuUiState,
    onDaySelected: (Int) -> Unit,
    onMealSelected: (MealType) -> Unit,
    onGoToToday: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onUpdateNotificationConfig: (NotificationConfig) -> Unit,
    onOpenCard: () -> Unit,
    onCloseCard: () -> Unit,
    onSaveCard: (StudentCard) -> Unit,
    onSyncCardFromSigaa: (matricula: String, pass: String) -> Unit,
    onDisconnectCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MealBottomControl(
                selectedMeal = state.selectedMeal,
                onMealSelected = onMealSelected,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cardápio RU",
                        style = NotionTypography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "UnB • Campus Darcy Ribeiro",
                        style = NotionTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botão Carteirinha Discente / QR Catraca
                    IconButton(onClick = onOpenCard) {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = "Carteirinha do Estudante",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Botão Notificações / Configurações
                    IconButton(onClick = onOpenSettings) {
                        val hasActiveAlerts = state.notificationConfig.cafeEnabled ||
                                state.notificationConfig.almocoEnabled ||
                                state.notificationConfig.jantarEnabled

                        Icon(
                            imageVector = if (hasActiveAlerts) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = "Configurar Notificações",
                            tint = if (hasActiveAlerts) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    }

                    // Botão Tema Claro / Escuro
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (state.isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (state.isDarkTheme) "Mudar para modo claro" else "Mudar para modo escuro",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (state.selectedDayIndex != state.todayIndex && state.todayIndex != -1) {
                        IconButton(onClick = onGoToToday) {
                            Icon(
                                imageVector = Icons.Outlined.Today,
                                contentDescription = "Ir para Hoje",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Atualizar",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Day Selector Rail
            val dias = state.menu?.dias ?: emptyList()
            if (dias.isNotEmpty()) {
                DaySelectorRail(
                    dias = dias,
                    selectedDayIndex = state.selectedDayIndex,
                    onDaySelected = onDaySelected,
                    todayIndex = state.todayIndex
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Swipeable and Scrollable Meal Card Area
            var totalDrag by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.selectedDayIndex, dias.size) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (totalDrag < -100f && state.selectedDayIndex < dias.size - 1) {
                                    onDaySelected(state.selectedDayIndex + 1)
                                } else if (totalDrag > 100f && state.selectedDayIndex > 0) {
                                    onDaySelected(state.selectedDayIndex - 1)
                                }
                                totalDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    }
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    val currentDay = state.currentDay
                    if (currentDay == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum cardápio disponível para este dia.",
                                style = NotionTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = Pair(state.selectedDayIndex, state.selectedMeal),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "meal_content_transition"
                        ) { (_, meal) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                // Day Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${currentDay.diaSemana}, ${currentDay.data}",
                                        style = NotionTypography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = meal.title,
                                            style = NotionTypography.titleSmall,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = when (meal) {
                                                MealType.CAFE -> "07h às 09h30"
                                                MealType.ALMOCO -> "11h às 14h30"
                                                MealType.JANTAR -> "17h às 19h30"
                                            },
                                            style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                        )
                                    }
                                }

                                when (meal) {
                                    MealType.CAFE -> {
                                        BreakfastCard(menu = currentDay.cafe)
                                    }
                                    MealType.ALMOCO -> {
                                        LunchDinnerCard(menu = currentDay.almoco, isJantar = false)
                                    }
                                    MealType.JANTAR -> {
                                        LunchDinnerCard(menu = currentDay.jantar, isJantar = true)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Mesa de apoio callout (Notion Style)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Mesa de apoio",
                                                style = NotionTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Água, café, açúcar, adoçante e temperos disponíveis no salão.",
                                                style = NotionTypography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Aviso formal do RU
                                Text(
                                    text = "Cardápio sujeito a alterações sem aviso prévio pela administração do RU.",
                                    style = NotionTypography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 28.dp)
                                )
                            }
                        }
                    }
                }
            } // Fechamento do Box de gestos
        } // Fechamento do Column principal

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    } // Fechamento do Box outer
    } // Fechamento do Scaffold

    // BottomSheet de Notificações
    if (state.isSettingsOpen) {
        NotificationSettingsBottomSheet(
            config = state.notificationConfig,
            onConfigChange = onUpdateNotificationConfig,
            onDismissRequest = onCloseSettings
        )
    }

    // Tela Cheia da Carteirinha Digital UnB
    if (state.isCardOpen) {
        StudentCardScreen(
            card = state.studentCard,
            statement = state.statement,
            isSyncing = state.isSyncingCard,
            syncProgressMessage = state.syncProgressMessage,
            syncError = state.cardSyncError,
            onSaveCard = onSaveCard,
            onSyncFromSigaa = onSyncCardFromSigaa,
            onDisconnect = onDisconnectCard,
            onClose = onCloseCard
        )
    }
}
