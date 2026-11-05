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
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.unb.ru.data.model.MealType

@Composable
fun MealTopControl(
    selectedMeal: MealType,
    onMealSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 1.dp
    ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealType.values().forEach { meal ->
                    val isSelected = meal == selectedMeal
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(durationMillis = 180),
                        label = "meal_pill_bg"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        animationSpec = tween(durationMillis = 180),
                        label = "meal_pill_icon"
                    )

                    val icon = when (meal) {
                        MealType.CAFE -> Icons.Outlined.Coffee
                        MealType.ALMOCO -> Icons.Outlined.Restaurant
                        MealType.JANTAR -> Icons.Outlined.SoupKitchen
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 34.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { onMealSelected(meal) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = meal.title,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
}
