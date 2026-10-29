package br.unb.ru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.unb.ru.ui.theme.NotionTypography

@Composable
fun DietaryChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = NotionTypography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}
