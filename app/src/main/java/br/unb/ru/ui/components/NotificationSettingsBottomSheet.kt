package br.unb.ru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Notificações do RU",
                style = NotionTypography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Avisos automáticos enviados 1 hora antes de cada refeição",
                style = NotionTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
            )

            // Switch: Café
            NotificationSwitchRow(
                title = "☕ Café da Manhã",
                subtitle = "Alerta às 06h00 (refeição das 7h às 9h30)",
                checked = config.cafeEnabled,
                onCheckedChange = { onConfigChange(config.copy(cafeEnabled = it)) }
            )

            // Switch: Almoço
            NotificationSwitchRow(
                title = "🍽️ Almoço",
                subtitle = "Alerta às 10h00 (refeição das 11h às 14h30)",
                checked = config.almocoEnabled,
                onCheckedChange = { onConfigChange(config.copy(almocoEnabled = it)) }
            )

            // Switch: Jantar
            NotificationSwitchRow(
                title = "🥣 Jantar",
                subtitle = "Alerta às 16h00 (refeição das 17h às 19h30)",
                checked = config.jantarEnabled,
                onCheckedChange = { onConfigChange(config.copy(jantarEnabled = it)) }
            )

            // Switch: Finais de Semana
            NotificationSwitchRow(
                title = "📅 Alertas nos fins de semana",
                subtitle = "Sábado e Domingo (quando houver cardápio)",
                checked = config.weekendEnabled,
                onCheckedChange = { onConfigChange(config.copy(weekendEnabled = it)) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )

            // Seção Dieta
            Text(
                text = "DIETA EXIBIDA NO PUSH",
                style = NotionTypography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "O texto da notificação destacará o prato da sua preferência",
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
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                text = subtitle,
                style = NotionTypography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
