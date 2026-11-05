package br.unb.ru.ui.components

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.unb.ru.data.model.RestaurantStatement
import br.unb.ru.data.model.StudentCard
import br.unb.ru.ui.theme.NotionTypography
import br.unb.ru.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscenteScreen(
    card: StudentCard,
    statement: RestaurantStatement,
    isSyncing: Boolean,
    isRefreshingStatement: Boolean,
    syncProgressMessage: String?,
    syncError: String?,
    onSyncFromSigaa: (matricula: String, pass: String) -> Unit,
    onRefreshStatement: () -> Unit,
    onDisconnect: () -> Unit,
    onCheckForUpdate: () -> Unit = {},
    isCheckingUpdate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showSyncDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }

    // Close sync dialog upon successful sync
    LaunchedEffect(isSyncing, card.isConfigured, syncError) {
        if (!isSyncing && card.isConfigured && syncError == null) {
            showSyncDialog = false
        }
    }

    // High brightness trigger when QR card is flipped
    DisposableEffect(isFlipped) {
        if (isFlipped && activity != null) {
            val params = activity.window.attributes
            val previousBrightness = params.screenBrightness
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            activity.window.attributes = params

            onDispose {
                params.screenBrightness = previousBrightness
                activity.window.attributes = params
            }
        } else {
            onDispose { }
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ru_card_flip"
    )

    // Load profile photo in background if URL available
    var avatarBitmap by remember(card.fotoPerfilUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(card.fotoPerfilUrl) {
        if (card.fotoPerfilUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val req = Request.Builder()
                        .url(card.fotoPerfilUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App")
                        .build()
                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val stream = resp.body?.byteStream()
                        if (stream != null) {
                            avatarBitmap = BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val qrBitmap = remember(card.codigoQr) {
        if (card.codigoQr.isNotBlank()) {
            QrCodeGenerator.generateQrBitmap(card.codigoQr, sizePx = 600)
        } else null
    }

    val initials = remember(card.nome) {
        val parts = card.nome.trim().split(Regex("\\s+"))
        if (parts.size > 1) {
            "${parts.first().firstOrNull() ?: 'U'}${parts.last().firstOrNull() ?: 'B'}".uppercase()
        } else {
            card.nome.take(2).uppercase().ifBlank { "UB" }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho da Aba Discente
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Painel Discente",
                    style = NotionTypography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (card.isConfigured) {
                        "Atualizado em ${statement.ultimaAtualizacao.ifBlank { "Hoje" }}"
                    } else {
                        "Não conectado"
                    },
                    style = NotionTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (card.isConfigured) {
                IconButton(
                    onClick = onRefreshStatement,
                    enabled = !isRefreshingStatement && !isSyncing
                ) {
                    if (isRefreshingStatement || isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Sincronizar dados",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        if (!card.isConfigured) {
            // ==========================================
            // ESTADO DESCONECTADO (SKELETONS + CTA)
            // ==========================================
            // Skeleton 1: Perfil
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            // CTA Central Primário
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSyncDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronizar com SIGAA",
                        style = NotionTypography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Conecte sua conta do SIGAA da UnB para acessar sua carteirinha, saldo do RU e histórico acadêmico.",
                    style = NotionTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Skeleton 2: Métricas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

        } else {
            // ==========================================
            // ESTADO CONECTADO (DASHBOARD COMPLETO)
            // ==========================================

            // CARD 1: PERFIL DO ESTUDANTE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar com foto ou iniciais
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "Foto do discente",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = initials,
                                    style = NotionTypography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Informações do aluno
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.nome.ifBlank { "Estudante UnB" },
                                style = NotionTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Matrícula: ${card.matricula}",
                                style = NotionTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = card.curso.ifBlank { "Graduação" },
                                style = NotionTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Badges: Status, Nível, Semestre
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x254CAF50)
                                ) {
                                    Text(
                                        text = card.status.ifBlank { "ATIVO" },
                                        style = NotionTypography.labelSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                // Nível
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = card.nivel.ifBlank { "GRADUAÇÃO" },
                                        style = NotionTypography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                // Semestre
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x25EAB308)
                                ) {
                                    Text(
                                        text = card.semestreAtual.ifBlank { "2026.2" },
                                        style = NotionTypography.labelSmall,
                                        color = Color(0xFFEAB308),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: RENDIMENTO ACADÊMICO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "RENDIMENTO ACADÊMICO",
                        style = NotionTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caixas de IRA e MP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "IRA",
                                    style = NotionTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = card.ira.ifBlank { "--" },
                                    style = NotionTypography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEAB308)
                                )
                                Text(
                                    text = "Índice de Rendimento",
                                    style = NotionTypography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "MÉDIA PONDERADA",
                                    style = NotionTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = card.mediaPonderada.ifBlank { "--" },
                                    style = NotionTypography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ponderada Geral",
                                    style = NotionTypography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Barra de Progresso de Integralização
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Integralização Curricular",
                            style = NotionTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${card.percentualConcluido}%",
                            style = NotionTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progressFraction = (card.percentualConcluido / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Concluídas: ${card.chConcluida}h",
                            style = NotionTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "Total: ${card.chTotal}h",
                            style = NotionTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    if (card.chObrigatoriaPendente > 0 || card.chOptativaPendente > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pendentes: Obrigatórias: ${card.chObrigatoriaPendente}h • Optativas: ${card.chOptativaPendente}h",
                            style = NotionTypography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // CARD 3: RU COM 3D FLIP CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rotation > 90f) Color.White else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (rotation > 90f) Color(0xFFDDDDDD) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                if (rotation <= 90f) {
                    // ==========================================
                    // FRENTE: SALDO E EXTRATO RECENTE DO RU
                    // ==========================================
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RESTAURANTE UNIVERSITÁRIO",
                                    style = NotionTypography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Saldo Disponível",
                                    style = NotionTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Hero Saldo
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = statement.saldoAtual.ifBlank { "R$ 0,00" },
                                    style = NotionTypography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Botão Girar para QR Code
                        OutlinedButton(
                            onClick = { isFlipped = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FlipCameraAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Girar para QR Code da Catraca",
                                style = NotionTypography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Extrato Recente
                        Text(
                            text = "TRANSAÇÕES RECENTES",
                            style = NotionTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val transactions = statement.transacoes.take(4)
                        if (transactions.isEmpty()) {
                            Text(
                                text = "Nenhuma transação recente registrada",
                                style = NotionTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                transactions.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.descricao,
                                                    style = NotionTypography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.dataHora,
                                                    style = NotionTypography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            Text(
                                                text = item.valor,
                                                style = NotionTypography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // VERSO: CARTEIRINHA E QR CODE (3D FLIPPED)
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationY = 180f // Desespelha o verso
                            }
                            .clickable { isFlipped = false }
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LEITOR DA CATRACA",
                                    style = NotionTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF555555),
                                    letterSpacing = 0.5.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF1F1F1),
                                    border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                                    modifier = Modifier.clickable { isFlipped = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RotateLeft,
                                            contentDescription = null,
                                            tint = Color(0xFF222222),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Saldo",
                                            style = NotionTypography.labelSmall,
                                            color = Color(0xFF222222),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Frame do QR Code em fundo branco puro
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code Catraca",
                                        modifier = Modifier.size(200.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = card.nome.ifBlank { "Estudante UnB" },
                                    style = NotionTypography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111)
                                )
                                Text(
                                    text = "Matrícula: ${card.matricula}",
                                    style = NotionTypography.bodyMedium,
                                    color = Color(0xFF555555)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF3F3F3)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BrightnessHigh,
                                        contentDescription = null,
                                        tint = Color(0xFF333333),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Brilho no máximo • Toque para virar",
                                        style = NotionTypography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                        }
                    }
                }
            }

                        // CARD DE ATUALIZAÇÕES E VERSÃO DO APP
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckForUpdate() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Versão do Aplicativo",
                                style = NotionTypography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Toque para verificar atualizações",
                                style = NotionTypography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "v${br.unb.ru.BuildConfig.VERSION_NAME}",
                                style = NotionTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // RODAPÉ: DESCONECTAR CONTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { showDisconnectConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Desconectar conta",
                        style = NotionTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Dialog de Confirmação de Desconexão
    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = {
                Text(
                    text = "Desconectar conta?",
                    style = NotionTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Deseja realmente desconectar sua conta do SIGAA deste dispositivo? Seus dados sincronizados serão removidos.",
                    style = NotionTypography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectConfirm = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Desconectar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Login/Sincronização com o SIGAA
    if (showSyncDialog) {
        var matInput by remember { mutableStateOf("") }
        var passInput by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { if (!isSyncing) showSyncDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Conectar ao SIGAA UnB",
                            style = NotionTypography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isSyncing) {
                            IconButton(onClick = { showSyncDialog = false }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                            }
                        }
                    }

                    Text(
                        text = "Informe suas credenciais do SIGAA / SSO da UnB para sincronizar sua carteirinha estudantil, extrato do RU e histórico acadêmico.",
                        style = NotionTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = matInput,
                        onValueChange = { matInput = it },
                        label = { Text("Matrícula") },
                        placeholder = { Text("Ex: 231000000") },
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text("Senha do SIGAA") },
                        placeholder = { Text("Senha institucional") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val errorToShow = localError ?: syncError
                    if (errorToShow != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x20EF4444),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorToShow,
                                color = Color(0xFFEF4444),
                                style = NotionTypography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (isSyncing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = syncProgressMessage ?: "Conectando ao SSO UnB...",
                                style = NotionTypography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSyncing) {
                            TextButton(onClick = { showSyncDialog = false }) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                if (matInput.isBlank()) {
                                    localError = "Informe sua matrícula."
                                    return@Button
                                }
                                if (passInput.isBlank()) {
                                    localError = "Informe sua senha do SIGAA."
                                    return@Button
                                }
                                localError = null
                                onSyncFromSigaa(matInput.trim(), passInput.trim())
                            },
                            enabled = !isSyncing
                        ) {
                            Text(if (isSyncing) "Sincronizando..." else "Conectar e Sincronizar")
                        }
                    }
                }
            }
        }
    }
}
