package br.unb.ru.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.repository.DietPreference
import br.unb.ru.data.repository.NotificationConfig
import br.unb.ru.ui.theme.NotionTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsBottomSheet(
    config: NotificationConfig,
    onConfigChange: (NotificationConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Notificações do RU",
                style = NotionTypography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Escolha quais refeições e em quais dias da semana deseja receber alertas (1h antes)",
                style = NotionTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
            )

            // Café
            MealNotificationSection(
                title = "Café da Manhã",
                timeLabel = "Alerta às 06h00 (refeição das 7h às 9h30)",
                isEnabled = config.cafeEnabled,
                onEnabledChange = { onConfigChange(config.copy(cafeEnabled = it)) },
                selectedDays = config.cafeDays,
                onDaysChange = { onConfigChange(config.copy(cafeDays = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // Almoço
            MealNotificationSection(
                title = "Almoço",
                timeLabel = "Alerta às 10h00 (refeição das 11h às 14h30)",
                isEnabled = config.almocoEnabled,
                onEnabledChange = { onConfigChange(config.copy(almocoEnabled = it)) },
                selectedDays = config.almocoDays,
                onDaysChange = { onConfigChange(config.copy(almocoDays = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // Jantar
            MealNotificationSection(
                title = "Jantar",
                timeLabel = "Alerta às 16h00 (refeição das 17h às 19h30)",
                isEnabled = config.jantarEnabled,
                onEnabledChange = { onConfigChange(config.copy(jantarEnabled = it)) },
                selectedDays = config.jantarDays,
                onDaysChange = { onConfigChange(config.copy(jantarDays = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )

            // Seção Dieta
            Text(
                text = "DIETA PREFERIDA",
                style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Destaca seu prato no cardápio e no texto das notificações",
                style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DietPreference.values().forEach { diet ->
                    val isSelected = config.dietPreference == diet
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onConfigChange(config.copy(dietPreference = diet)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diet.label,
                            style = NotionTypography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dica de compatibilidade de bateria
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Text(
                    text = "Segundo plano no Android: Diferentes fabricantes (Xiaomi, Samsung, Motorola, Realme e outros) aplicam otimizações rígidas de bateria. Para garantir os avisos pontuais mesmo com o app fechado, defina a bateria do aplicativo como 'Sem restrições' nas configurações do sistema.",
                    style = NotionTypography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MealNotificationSection(
    title: String,
    timeLabel: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = NotionTypography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeLabel,
                    style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        AnimatedVisibility(visible = isEnabled) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = "Dias ativos:",
                    style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysLabels = listOf(
                        Pair(1, "SEG"),
                        Pair(2, "TER"),
                        Pair(3, "QUA"),
                        Pair(4, "QUI"),
                        Pair(5, "SEX"),
                        Pair(6, "SÁB"),
                        Pair(7, "DOM")
                    )
                    daysLabels.forEach { (dayNum, label) ->
                        val isDaySelected = selectedDays.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                            .background(
                                if (isDaySelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDaySelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                            .clickable {
                                val newDays = selectedDays.toMutableSet()
                                if (isDaySelected) newDays.remove(dayNum) else newDays.add(dayNum)
                                onDaysChange(newDays)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = NotionTypography.labelSmall.copy(fontSize = 9.5.sp),
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}
