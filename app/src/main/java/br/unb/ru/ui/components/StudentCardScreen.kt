package br.unb.ru.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.unb.ru.data.model.RestaurantStatement
import br.unb.ru.data.model.StudentCard
import br.unb.ru.ui.theme.NotionTypography
import br.unb.ru.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardScreen(
    card: StudentCard,
    statement: RestaurantStatement,
    isSyncing: Boolean,
    isRefreshingStatement: Boolean,
    syncProgressMessage: String?,
    syncError: String?,
    onSaveCard: (StudentCard) -> Unit,
    onSyncFromSigaa: (matricula: String, pass: String) -> Unit,
    onRefreshStatement: () -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit,
    onCheckForUpdate: () -> Unit = {},
    isCheckingUpdate: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isFullScreenQr by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showRuGuide by remember { mutableStateOf(false) }

    LaunchedEffect(isSyncing, card.isConfigured, syncError) {
        if (!isSyncing && card.isConfigured && syncError == null) {
            showSyncDialog = false
        }
    }

    val qrBitmap = remember(card.codigoQr) {
        if (card.codigoQr.isNotBlank()) {
            QrCodeGenerator.generateQrBitmap(card.codigoQr, sizePx = 650)
        } else null
    }

    // Tela cheia dedicada
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Carteirinha Digital",
                            style = NotionTypography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "UnB • Restaurante Universitário",
                            style = NotionTypography.bodyMedium.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Row {
                        if (!card.isConfigured) {
                            IconButton(onClick = { showSyncDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = "Conectar SIGAA",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!card.isConfigured) {
                    // Estado Vazio / Primeiro Acesso
                    EmptyCardState(
                        onConnectSigaa = { showSyncDialog = true },
                        onEditManual = { showEditDialog = true }
                    )
                } else {
                    // Cartão de Identidade Estudantil Estilo Notion Editorial
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cabeçalho da Carteirinha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UNIVERSIDADE DE BRASÍLIA",
                                    style = NotionTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )

                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // QR Code Container em Alto Contraste
                            Box(
                                modifier = Modifier
                                    .size(230.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E2E0), RoundedCornerShape(14.dp))
                                    .clickable { isFullScreenQr = true }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code da Catraca",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "QR Code indisponível",
                                        style = NotionTypography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isFullScreenQr = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fullscreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Toque no QR para tela cheia e brilho máximo",
                                    style = NotionTypography.bodyMedium.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )

                            // Dados do Aluno
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CardInfoRow(label = "NOME", value = card.nome)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CardInfoRow(label = "MATRÍCULA", value = card.matricula)
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        CardInfoRow(label = "VALIDADE", value = card.validade)
                                    }
                                }
                                CardInfoRow(label = "CURSO", value = card.curso)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seção de Saldo e Extrato do RU
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "EXTRATO NO RU",
                                        style = NotionTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    if (statement.ultimaAtualizacao.isNotBlank()) {
                                        Text(
                                            text = "Atualizado em ${statement.ultimaAtualizacao}",
                                            style = NotionTypography.bodyMedium.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onRefreshStatement,
                                    enabled = !isRefreshingStatement
                                ) {
                                    if (isRefreshingStatement) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRefreshingStatement) "Atualizando..." else "Atualizar",
                                        style = NotionTypography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cartão de Saldo Atual
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "SALDO ATUAL",
                                    style = NotionTypography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = statement.saldoAtual.ifBlank { "R$ 0,00" },
                                    style = NotionTypography.titleLarge.copy(fontSize = 24.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "HISTÓRICO",
                                style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (statement.transacoes.isEmpty()) {
                                Text(
                                    text = "Nenhuma transação registrada ainda nesta conta.",
                                    style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    statement.transacoes.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.descricao,
                                                    style = NotionTypography.bodyMedium.copy(fontSize = 13.sp),
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = item.dataHora,
                                                    style = NotionTypography.bodyMedium.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                                )
                                            }
                                            Text(
                                                text = item.valor,
                                                style = NotionTypography.bodyMedium.copy(fontSize = 13.5.sp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (item.isDebito) MaterialTheme.colorScheme.onSurface else Color(0xFF2E7D32)
                                            )
                                        }
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botões e Guia do RU
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRuGuide = !showRuGuide },
                            modifier = Modifier.weight(1.15f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Informações do RU",
                                style = NotionTypography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(0.85f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Trocar Conta",
                                style = NotionTypography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sobre o App e Atualizações
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onCheckForUpdate() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Versão v${br.unb.ru.BuildConfig.VERSION_NAME}",
                                style = NotionTypography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "Buscar atualização",
                                style = NotionTypography.labelSmall.copy(fontSize = 11.5.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Guia do RU
                    AnimatedVisibility(visible = showRuGuide) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Informações e Regras do RU",
                                        style = NotionTypography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Seção Preços
                                Text(
                                    text = "PREÇOS POR GRUPO",
                                    style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GuideItem(
                                        title = "Grupo 1 (Isentos): R$ 0,00",
                                        description = "Estudantes de graduação e pós-graduação assistidos pelo PNAES, CadÚnico, cotistas de baixa renda, indígenas, quilombolas e outros critérios de vulnerabilidade."
                                    )
                                    GuideItem(
                                        title = "Grupo 2 (Estudantes não isentos): R$ 2,00 café / R$ 4,50 almoço ou jantar",
                                        description = "Estudantes de graduação, pós-graduação e estagiários do DGP/UnB não isentos."
                                    )
                                    GuideItem(
                                        title = "Grupo 4 (Escolas públicas não PNAES): R$ 1,50 café / R$ 2,50 almoço ou jantar",
                                        description = "Estudantes de escolas públicas não contemplados pelo PNAES."
                                    )
                                    GuideItem(
                                        title = "Grupo 3 (Servidores e visitantes): R$ 7,05 café / R$ 15,20 almoço ou jantar",
                                        description = "Servidores, trabalhadores terceirizados e visitantes (valor integral do contrato)."
                                    )
                                }

                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                // Seção Refeitórios
                                Text(
                                    text = "REFEITÓRIOS E RESTAURANTE EXECUTIVO",
                                    style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GuideItem(
                                        title = "6 Refeitórios Tradicionais (por andar)",
                                        description = "Existem 6 refeitórios: 2 no térreo, 2 no 1º andar e 2 no 2º andar."
                                    )
                                    GuideItem(
                                        title = "Restaurante Executivo (Piso Superior - 4º andar): R$ 60,42/kg",
                                        description = "Modalidade self-service por peso sem subsídio da universidade. O valor final depende exclusivamente do peso do prato. Formas de pagamento aceitas: Pix, cartão de débito, crédito e dinheiro em espécie."
                                    )
                                }

                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                // Seção Regras e Convivência
                                Text(
                                    text = "REGRAS E CONVIVÊNCIA",
                                    style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GuideBulletItem(text = "Prato principal: Não é permitido repetir o prato principal.")
                                    GuideBulletItem(text = "Opções Vegetariana e Ovolacto: Ficam disponíveis em um refeitório específico divulgado pela administração em tela ou cartaz na parede. A mesa de servir dessas opções fica separada do prato principal padrão.")
                                    GuideBulletItem(text = "Não segure lugar: Ao terminar de comer, retire-se da mesa para liberar espaço. As filas são grandes e os lugares são limitados.")
                                    GuideBulletItem(text = "Devolução das bandejas: Não deixe nada na mesa. Pratos, talheres, copos e bandejas devem ser levados até a área de higienização, no lado oposto às catracas de entrada.")
                                    GuideBulletItem(text = "Catraca do RU: Aponte o QR Code para o leitor de vidro (a cerca de 10 a 15 cm). Use o toque para tela cheia com brilho máximo se houver reflexo.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Tela Cheia com Brilho Máximo para a Catraca
    if (isFullScreenQr && qrBitmap != null) {
        FullScreenQrDialog(
            bitmap = qrBitmap,
            matricula = card.matricula,
            nome = card.nome,
            activity = activity,
            onDismiss = { isFullScreenQr = false }
        )
    }

    // Diálogo Sincronizar via SIGAA (com feedback e animação)
    if (showSyncDialog) {
        SigaaSyncDialog(
            initialMatricula = card.matricula,
            isSyncing = isSyncing,
            progressMessage = syncProgressMessage,
            errorMessage = syncError,
            onConfirm = { mat, pass ->
                onSyncFromSigaa(mat, pass)
            },
            onDismiss = {
                if (!isSyncing) showSyncDialog = false
            }
        )
    }

    // Diálogo Editar Dados Manualmente
    if (showEditDialog) {
        EditCardDialog(
            currentCard = card,
            onSave = { updated ->
                onSaveCard(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun EmptyCardState(
    onConnectSigaa: () -> Unit,
    onEditManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Conecte sua Conta UnB",
            style = NotionTypography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sincronize com o SIGAA para gerar automaticamente o seu QR Code permanente para as catracas do RU e acompanhar seu saldo e extrato acumulado.",
            style = NotionTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onConnectSigaa,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Conectar com SIGAA", style = NotionTypography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onEditManual,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
            )
        ) {
            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preencher Manualmente", style = NotionTypography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun CardInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = NotionTypography.labelSmall.copy(fontSize = 9.5.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = NotionTypography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FullScreenQrDialog(
    bitmap: android.graphics.Bitmap,
    matricula: String,
    nome: String,
    activity: Activity?,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = originalBrightness
                window.attributes = lp
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Aponte para o leitor da catraca",
                    style = NotionTypography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Brilho no máximo ativado • Toque para fechar",
                    style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code Tela Cheia",
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = nome,
                    style = NotionTypography.titleSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Matrícula: $matricula",
                    style = NotionTypography.bodyMedium,
                    color = Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
private fun SigaaSyncDialog(
    initialMatricula: String,
    isSyncing: Boolean,
    progressMessage: String?,
    errorMessage: String?,
    onConfirm: (matricula: String, pass: String) -> Unit,
    onDismiss: () -> Unit
) {
    var matricula by remember { mutableStateOf(initialMatricula) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Sincronizar com SIGAA", style = NotionTypography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSyncing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = progressMessage ?: "Conectando ao SIGAA...",
                            style = NotionTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Insira sua matrícula e senha do SIGAA / SSO UnB. O app solicitará a carteirinha oficial com QR Code e sincronizará seu extrato do restaurante.",
                        style = NotionTypography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = matricula,
                        onValueChange = { matricula = it },
                        label = { Text("Matrícula") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha do SIGAA") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isSyncing) {
                Button(
                    onClick = { onConfirm(matricula, password) },
                    enabled = matricula.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Sincronizar")
                }
            }
        },
        dismissButton = {
            if (!isSyncing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun EditCardDialog(
    currentCard: StudentCard,
    onSave: (StudentCard) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf(currentCard.nome) }
    var matricula by remember { mutableStateOf(currentCard.matricula) }
    var curso by remember { mutableStateOf(currentCard.curso) }
    var validade by remember { mutableStateOf(currentCard.validade) }
    var codigoQr by remember { mutableStateOf(currentCard.codigoQr) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Editar Dados da Carteirinha", style = NotionTypography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matricula,
                    onValueChange = { matricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = curso,
                    onValueChange = { curso = it },
                    label = { Text("Curso") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = validade,
                    onValueChange = { validade = it },
                    label = { Text("Validade (Ex: Março / 2027)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = codigoQr,
                    onValueChange = { codigoQr = it },
                    label = { Text("Código Numérico do QR (16 dígitos)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentCard.copy(
                            nome = nome,
                            matricula = matricula,
                            curso = curso,
                            validade = validade,
                            codigoQr = codigoQr
                        )
                    )
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun GuideItem(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = title,
            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = NotionTypography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 15.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun GuideBulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            style = NotionTypography.bodyMedium.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
