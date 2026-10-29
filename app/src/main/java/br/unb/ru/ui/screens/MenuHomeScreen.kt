package br.unb.ru.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.model.MealType
import br.unb.ru.ui.components.BreakfastCard
import br.unb.ru.ui.components.DaySelectorRail
import br.unb.ru.ui.components.LunchDinnerCard
import br.unb.ru.ui.components.MealBottomControl
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
    modifier: Modifier = Modifier
) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                    // Swipe left -> Next day
                                    onDaySelected(state.selectedDayIndex + 1)
                                } else if (totalDrag > 100f && state.selectedDayIndex > 0) {
                                    // Swipe right -> Previous day
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
                                    Text(
                                        text = meal.title,
                                        style = NotionTypography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
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

                                Spacer(modifier = Modifier.height(16.dp))

                                // RU Info footer
                                Text(
                                    text = "Mesa de apoio: água, café, açúcar, adoçante e temperos. Cardápio sujeito a alterações pela administração do RU.",
                                    style = NotionTypography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 17.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
