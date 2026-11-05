package br.unb.ru.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.model.Campus
import br.unb.ru.data.model.MealType
import br.unb.ru.data.model.StudentCard
import br.unb.ru.data.repository.NotificationConfig
import br.unb.ru.ui.components.AppUpdateDialog
import br.unb.ru.ui.components.BreakfastCard
import br.unb.ru.ui.components.CampusSelectorBottomSheet
import br.unb.ru.ui.components.DaySelectorRail
import br.unb.ru.ui.components.DiscenteScreen
import br.unb.ru.ui.components.MealTopControl
import br.unb.ru.ui.components.LunchDinnerCard
import br.unb.ru.ui.components.MealBottomControl
import br.unb.ru.ui.components.NotificationSettingsBottomSheet
import br.unb.ru.ui.components.StudentCardScreen
import br.unb.ru.ui.theme.NotionTypography
import br.unb.ru.ui.viewmodel.MenuUiState
import br.unb.ru.util.QrCodeGenerator

enum class MainScreenTab {
    CARDAPIO,
    DISCENTE
}

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
    onRefreshStatement: () -> Unit,
    onDisconnectCard: () -> Unit,
    onOpenCampusSelector: () -> Unit = {},
    onCloseCampusSelector: () -> Unit = {},
    onCampusSelected: (Campus) -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    onConfirmUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onClearUpdateMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(MainScreenTab.CARDAPIO) }
    var isDiscenteFlipped by remember { mutableStateOf(false) }

    // Screen brightness control on flip
    DisposableEffect(isDiscenteFlipped) {
        if (isDiscenteFlipped && activity != null) {
            val params = activity.window.attributes
            val prevBrightness = params.screenBrightness
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            activity.window.attributes = params
            onDispose {
                params.screenBrightness = prevBrightness
                activity.window.attributes = params
            }
        } else {
            onDispose { }
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isDiscenteFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "global_screen_flip"
    )

    val qrBitmap = remember(state.studentCard.codigoQr) {
        if (state.studentCard.codigoQr.isNotBlank()) {
            QrCodeGenerator.generateQrBitmap(state.studentCard.codigoQr, sizePx = 800)
        } else null
    }

    LaunchedEffect(state.updateMessage) {
        state.updateMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearUpdateMessage()
        }
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            if (currentTab == MainScreenTab.CARDAPIO) {
                onRefresh()
            } else {
                onRefreshStatement()
            }
        }
    }
    val isBusy = if (currentTab == MainScreenTab.CARDAPIO) state.isLoading else state.isRefreshingStatement
    LaunchedEffect(isBusy) {
        if (!isBusy) {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
    ) {
        if (rotation <= 90f) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cardápio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { currentTab = MainScreenTab.CARDAPIO },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val isSelected = currentTab == MainScreenTab.CARDAPIO
                                Icon(
                                    imageVector = Icons.Outlined.RestaurantMenu,
                                    contentDescription = "Cardápio",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cardápio",
                                    style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Discente
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { currentTab = MainScreenTab.DISCENTE },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val isSelected = currentTab == MainScreenTab.DISCENTE
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = "Discente",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Discente",
                                    style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (currentTab == MainScreenTab.CARDAPIO) {
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
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenCampusSelector() }
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UnB • ${state.selectedCampus.displayName}",
                            style = NotionTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Trocar campus",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {


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

            // Sub-cabeçalho Integrado: Refeição & Horário à esquerda + Seletor de Refeições à direita
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.selectedMeal.title,
                        style = NotionTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (state.selectedMeal) {
                            MealType.CAFE -> "07h às 09h30"
                            MealType.ALMOCO -> "11h às 14h30"
                            MealType.JANTAR -> "17h às 19h30"
                        },
                        style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }

                MealTopControl(
                    selectedMeal = state.selectedMeal,
                    onMealSelected = onMealSelected
                )
            }

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


                                when (meal) {
                                    MealType.CAFE -> {
                                        BreakfastCard(
                                            menu = currentDay.cafe,
                                            dietPreference = state.notificationConfig.effectiveDietPreference
                                        )
                                    }
                                    MealType.ALMOCO -> {
                                        LunchDinnerCard(
                                            menu = currentDay.almoco,
                                            isJantar = false,
                                            dietPreference = state.notificationConfig.effectiveDietPreference
                                        )
                                    }
                                    MealType.JANTAR -> {
                                        LunchDinnerCard(
                                            menu = currentDay.jantar,
                                            isJantar = true,
                                            dietPreference = state.notificationConfig.effectiveDietPreference
                                        )
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
    } else {
        DiscenteScreen(
            card = state.studentCard,
            statement = state.statement,
            isSyncing = state.isSyncingCard,
            isRefreshingStatement = state.isRefreshingStatement,
            syncProgressMessage = state.syncProgressMessage,
            syncError = state.cardSyncError,
            onSyncFromSigaa = onSyncCardFromSigaa,
            onRefreshStatement = onRefreshStatement,
            onDisconnect = onDisconnectCard,
            onCheckForUpdate = onCheckForUpdate,
            isCheckingUpdate = state.isCheckingUpdate,
            onFlipToQr = { isDiscenteFlipped = true },
            modifier = Modifier.statusBarsPadding()
        )
    }

        if (pullRefreshState.verticalOffset > 0f || pullRefreshState.isRefreshing) {
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    } // Fechamento do Box outer
    } // Fechamento do Scaffold
    } else {
        // ==========================================
        // VERSO: 100% TELA CHEIA QR CODE (Desespelhado a 180f)
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = 180f }
                .clickable { isDiscenteFlipped = false },
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // QR Code Grande (290dp)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5))
                ) {
                    Box(
                        modifier = Modifier.padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code Catraca",
                                modifier = Modifier.size(290.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = state.studentCard.nome.ifBlank { "Estudante UnB" },
                    style = NotionTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matrícula: ${state.studentCard.matricula}",
                    style = NotionTypography.bodyMedium,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Toque para virar",
                        style = NotionTypography.bodySmall,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    } // Fechamento do Box outer 3D flip

    // BottomSheet de Notificações
    if (state.isSettingsOpen) {
        NotificationSettingsBottomSheet(
            config = state.notificationConfig,
            onConfigChange = onUpdateNotificationConfig,
            onDismissRequest = onCloseSettings
        )
    }

    // BottomSheet de Seleção de Campus
    if (state.isCampusSelectorOpen) {
        CampusSelectorBottomSheet(
            selectedCampus = state.selectedCampus,
            onCampusSelected = onCampusSelected,
            onDismissRequest = onCloseCampusSelector
        )
    }

    // Tela Cheia da Carteirinha Digital UnB
    if (state.isCardOpen) {
        StudentCardScreen(
            card = state.studentCard,
            statement = state.statement,
            isSyncing = state.isSyncingCard,
            isRefreshingStatement = state.isRefreshingStatement,
            syncProgressMessage = state.syncProgressMessage,
            syncError = state.cardSyncError,
            onSaveCard = onSaveCard,
            onSyncFromSigaa = onSyncCardFromSigaa,
            onRefreshStatement = onRefreshStatement,
            onDisconnect = onDisconnectCard,
            onClose = onCloseCard,
            onCheckForUpdate = onCheckForUpdate,
            isCheckingUpdate = state.isCheckingUpdate
        )
    }

    // Modal de Atualização do App
    state.availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            downloadProgress = state.updateDownloadProgress,
            isDownloading = state.isDownloadingUpdate,
            downloadedFileReady = state.downloadedApkFile != null && state.downloadedApkFile.exists(),
            onConfirmUpdate = onConfirmUpdate,
            onDismiss = onDismissUpdate
        )
    }
}
