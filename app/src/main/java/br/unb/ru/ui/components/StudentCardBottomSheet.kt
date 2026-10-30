package br.unb.ru.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import br.unb.ru.data.model.StudentCard
import br.unb.ru.ui.theme.NotionTypography
import br.unb.ru.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardBottomSheet(
    card: StudentCard,
    onSaveCard: (StudentCard) -> Unit,
    onSyncFromSigaa: (matricula: String, pass: String) -> Unit,
    isSyncing: Boolean,
    syncError: String?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isFullScreenQr by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showRuGuide by remember { mutableStateOf(false) }

    val qrBitmap = remember(card.codigoQr) {
        QrCodeGenerator.generateQrBitmap(card.codigoQr, sizePx = 650)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Carteirinha Digital UnB",
                        style = NotionTypography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Acesso Rápido ao Restaurante Universitário",
                        style = NotionTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row {
                    IconButton(onClick = { showSyncDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Sincronizar com SIGAA",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar Dados",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    // Tarja institucional UnB
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = card.vinculo,
                                style = NotionTypography.labelSmall.copy(fontSize = 9.5.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR Code Container em Alto Contraste (fundo branco puro para leitura óptica garantida)
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E2E0), RoundedCornerShape(14.dp))
                            .clickable { isFullScreenQr = true }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code da Carteirinha",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dica de toque para tela cheia
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
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    // Dados do Discente
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

            if (syncError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = syncError,
                    style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botões de Ação Úteis para o RU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botão Recarregar Saldo
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sigaa.unb.br/sigaa/verTelaLogin.do"))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Comprar Créditos", style = NotionTypography.bodyMedium.copy(fontSize = 12.5.sp))
                }

                // Botão Guia Calouro / RU
                OutlinedButton(
                    onClick = { showRuGuide = !showRuGuide },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Guia do RU", style = NotionTypography.bodyMedium.copy(fontSize = 12.5.sp))
                }
            }

            // Guia do RU Expansível
            AnimatedVisibility(visible = showRuGuide) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Informações Importantes do RU UnB",
                            style = NotionTypography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Valor por refeição: R\$ 6,10 (estudantes de graduação/pós) ou gratuito para discentes atendidos pela assistência estudantil.",
                            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "• Leitura na catraca: Aponte a tela do celular voltada para o leitor de vidro (cerca de 10 a 15 cm de distância). Ative o brilho máximo se a catraca demorar para ler.",
                            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "• Mesa de apoio: Ao lado das estações de servir, você encontra temperos, azeite, vinagre, açúcar e café.",
                            style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
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

    // Diálogo Sincronizar via SIGAA
    if (showSyncDialog) {
        SigaaSyncDialog(
            initialMatricula = card.matricula,
            isSyncing = isSyncing,
            onConfirm = { mat, pass ->
                onSyncFromSigaa(mat, pass)
                showSyncDialog = false
            },
            onDismiss = { showSyncDialog = false }
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
    // Aumenta o brilho da tela para 100% durante a exibição na catraca
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Insira sua matrícula e senha do SIGAA/SSO UnB. O app solicitará a emissão da carteirinha oficial e salvará o QR Code automaticamente no aparelho.",
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(matricula, password) },
                enabled = !isSyncing && matricula.isNotBlank() && password.isNotBlank()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sincronizar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("Cancelar")
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
