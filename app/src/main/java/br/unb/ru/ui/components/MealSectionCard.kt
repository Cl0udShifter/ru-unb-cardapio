package br.unb.ru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import br.unb.ru.data.model.BreakfastMenu
import br.unb.ru.data.model.LunchDinnerMenu
import br.unb.ru.ui.theme.*

@Composable
fun LunchDinnerCard(
    menu: LunchDinnerMenu,
    isJantar: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        // Section: Pratos Principais
        SectionHeader(title = "PRATOS PRINCIPAIS")

        if (menu.pratoPrincipal.isNotBlank()) {
            DishRow(
                tag = "Padrão",
                tagBg = TagStandardBg,
                tagText = TagStandardText,
                dishName = menu.pratoPrincipal,
                isHighlight = true
            )
        }

        if (menu.vegetariano.isNotBlank()) {
            DishRow(
                tag = "Ovolacto",
                tagBg = TagOvolactoBg,
                tagText = TagOvolactoText,
                dishName = menu.vegetariano
            )
        }

        if (menu.vegano.isNotBlank()) {
            DishRow(
                tag = "Vegano",
                tagBg = TagVeggieBg,
                tagText = TagVeggieText,
                dishName = menu.vegano
            )
        }

        NotionDivider()

        // Section: Guarnição e Acompanhamentos
        SectionHeader(title = "GUARNIÇÃO & ACOMPANHAMENTOS")
        if (menu.guarnicao.isNotBlank()) {
            SimpleItemRow(label = "Guarnição", value = menu.guarnicao)
        }
        if (menu.acompanhamentos.isNotBlank()) {
            SimpleItemRow(label = "Acomp.", value = menu.acompanhamentos)
        }

        // Section: Sopa e Torrada (se for Jantar)
        if (isJantar && (menu.sopa.isNotBlank() || menu.torrada.isNotBlank())) {
            NotionDivider()
            SectionHeader(title = "SOPA DO DIA")
            if (menu.sopa.isNotBlank()) {
                SimpleItemRow(label = "Sopa", value = menu.sopa)
            }
            if (menu.torrada.isNotBlank()) {
                SimpleItemRow(label = "Torrada", value = menu.torrada)
            }
        }

        NotionDivider()

        // Section: Saladas e Molho
        SectionHeader(title = "SALADAS")
        if (menu.salada1.isNotBlank()) {
            SimpleItemRow(label = "Salada 1", value = menu.salada1)
        }
        if (menu.salada2.isNotBlank()) {
            SimpleItemRow(label = "Salada 2", value = menu.salada2)
        }
        if (menu.molho.isNotBlank()) {
            SimpleItemRow(label = "Molho", value = menu.molho)
        }

        NotionDivider()

        // Section: Sobremesa e Bebida
        SectionHeader(title = "SOBREMESA & BEBIDA")
        if (menu.sobremesa.isNotBlank()) {
            DishRow(
                tag = "Sobremesa",
                tagBg = TagDessertBg,
                tagText = TagDessertText,
                dishName = menu.sobremesa
            )
        }
        if (menu.bebida.isNotBlank()) {
            DishRow(
                tag = "Refresco",
                tagBg = TagSideBg,
                tagText = TagSideText,
                dishName = menu.bebida
            )
        }
    }
}

@Composable
fun BreakfastCard(
    menu: BreakfastMenu,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        SectionHeader(title = "COMPLEMENTOS")

        if (menu.pratoPrincipal.isNotBlank()) {
            DishRow(
                tag = "Padrão",
                tagBg = TagStandardBg,
                tagText = TagStandardText,
                dishName = menu.pratoPrincipal,
                isHighlight = true
            )
        }
        if (menu.vegetariano.isNotBlank()) {
            DishRow(
                tag = "Ovolacto",
                tagBg = TagOvolactoBg,
                tagText = TagOvolactoText,
                dishName = menu.vegetariano
            )
        }
        if (menu.vegano.isNotBlank()) {
            DishRow(
                tag = "Vegano",
                tagBg = TagVeggieBg,
                tagText = TagVeggieText,
                dishName = menu.vegano
            )
        }

        if (menu.opcaoExtra.isNotBlank()) {
            DishRow(
                tag = "Extra",
                tagBg = TagDessertBg,
                tagText = TagDessertText,
                dishName = menu.opcaoExtra
            )
        }

        NotionDivider()

        SectionHeader(title = "BEBIDAS & PANIFICAÇÃO")
        if (menu.bebidas.isNotBlank()) {
            SimpleItemRow(label = "Bebidas", value = menu.bebidas)
        }
        if (menu.panificacao.isNotBlank()) {
            SimpleItemRow(label = "Pães", value = menu.panificacao)
        }
        if (menu.gordura.isNotBlank()) {
            SimpleItemRow(label = "Gordura", value = menu.gordura)
        }

        if (menu.fruta.isNotBlank()) {
            NotionDivider()
            SectionHeader(title = "FRUTA DO DIA")
            DishRow(
                tag = "Fruta",
                tagBg = TagDessertBg,
                tagText = TagDessertText,
                dishName = menu.fruta
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = NotionTypography.labelSmall.copy(fontSize = 10.5.sp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun DishRow(
    tag: String,
    tagBg: Color,
    tagText: Color,
    dishName: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        DietaryChip(text = tag, backgroundColor = tagBg, textColor = tagText)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = dishName,
            style = if (isHighlight) NotionTypography.titleSmall else NotionTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SimpleItemRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = NotionTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.width(74.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = NotionTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    )
}
