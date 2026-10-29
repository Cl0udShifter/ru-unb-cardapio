package br.unb.ru.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.unb.ru.data.model.MealType
import br.unb.ru.ui.theme.NotionTypography

@Composable
fun MealBottomControl(
    selectedMeal: MealType,
    onMealSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0x20000000)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MealType.values().forEach { meal ->
                val isSelected = meal == selectedMeal
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "meal_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    animationSpec = tween(durationMillis = 200),
                    label = "meal_text"
                )

                val icon = when (meal) {
                    MealType.CAFE -> Icons.Outlined.Coffee
                    MealType.ALMOCO -> Icons.Outlined.Restaurant
                    MealType.JANTAR -> Icons.Outlined.SoupKitchen
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onMealSelected(meal) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = meal.title,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = meal.shortName,
                        style = NotionTypography.titleSmall,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
