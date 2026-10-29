package br.unb.ru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unb.ru.data.model.DailyMenu
import br.unb.ru.ui.theme.*

@Composable
fun DaySelectorRail(
    dias: List<DailyMenu>,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    todayIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(dias) { index, dia ->
            val isSelected = index == selectedDayIndex
            val isToday = index == todayIndex

            val shortDay = when (index) {
                0 -> "SEG"
                1 -> "TER"
                2 -> "QUA"
                3 -> "QUI"
                4 -> "SEX"
                5 -> "SÁB"
                6 -> "DOM"
                else -> "DIA"
            }

            // Extract day number (e.g. "26" from "26/10/2026")
            val dayNumber = dia.data.split("/").firstOrNull() ?: "${index + 1}"

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onDaySelected(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = shortDay,
                        style = NotionTypography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    color = if (isSelected) Color(0xFF81C784) else NotionTodayDot,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dayNumber,
                    style = NotionTypography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
