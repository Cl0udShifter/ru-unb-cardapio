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
import br.unb.ru.data.repository.DietPreference
import br.unb.ru.ui.theme.*

private data class DishEntry(
    val tag: String,
    val name: String,
    val type: DietPreference
)

@Composable
fun LunchDinnerCard(
    menu: LunchDinnerMenu,
    isJantar: Boolean,
    dietPreference: DietPreference = DietPreference.PADRAO,
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

        val rawDishes = buildList {
            if (menu.pratoPrincipal.isNotBlank()) {
                add(DishEntry("Padrão", menu.pratoPrincipal, DietPreference.PADRAO))
            }
            if (menu.vegetariano.isNotBlank()) {
                add(DishEntry("Ovolacto", menu.vegetariano, DietPreference.OVOLACTO))
            }
            if (menu.vegano.isNotBlank()) {
                add(DishEntry("Vegano", menu.vegano, DietPreference.VEGANO))
            }
        }

        val sortedDishes = when (dietPreference) {
            DietPreference.PADRAO -> rawDishes
            DietPreference.OVOLACTO -> rawDishes.sortedWith(compareBy { entry ->
                when (entry.type) {
                    DietPreference.OVOLACTO -> 0
                    DietPreference.VEGANO -> 1
                    DietPreference.PADRAO -> 2
                }
            })
            DietPreference.VEGANO -> rawDishes.sortedWith(compareBy { entry ->
                when (entry.type) {
                    DietPreference.VEGANO -> 0
                    DietPreference.OVOLACTO -> 1
                    DietPreference.PADRAO -> 2
                }
            })
        }

        sortedDishes.forEachIndexed { idx, dish ->
            val isHighlight = (idx == 0 && dish.type == dietPreference) ||
                    (dietPreference == DietPreference.PADRAO && dish.type == DietPreference.PADRAO)
            val (tagBg, tagText) = when (dish.type) {
                DietPreference.PADRAO -> Pair(TagStandardBg, TagStandardText)
                DietPreference.OVOLACTO -> Pair(TagOvolactoBg, TagOvolactoText)
                DietPreference.VEGANO -> Pair(TagVeggieBg, TagVeggieText)
            }
            DishRow(
                tag = dish.tag,
                tagBg = tagBg,
                tagText = tagText,
                dishName = dish.name,
                isHighlight = isHighlight
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

        // Section: Sobremesa e Bebida (Texto puro em linhas simples, sem pílulas coloridas)
        SectionHeader(title = "SOBREMESA & BEBIDA")
        if (menu.sobremesa.isNotBlank()) {
            SimpleItemRow(label = "Sobremesa", value = menu.sobremesa)
        }
        if (menu.bebida.isNotBlank()) {
            SimpleItemRow(label = "Refresco", value = menu.bebida)
        }
    }
}

@Composable
fun BreakfastCard(
    menu: BreakfastMenu,
    dietPreference: DietPreference = DietPreference.PADRAO,
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

        val rawComplements = buildList {
            if (menu.pratoPrincipal.isNotBlank()) {
                add(DishEntry("Padrão", menu.pratoPrincipal, DietPreference.PADRAO))
            }
            if (menu.vegetariano.isNotBlank()) {
                add(DishEntry("Ovolacto", menu.vegetariano, DietPreference.OVOLACTO))
            }
            if (menu.vegano.isNotBlank()) {
                add(DishEntry("Vegano", menu.vegano, DietPreference.VEGANO))
            }
        }

        val sortedComplements = when (dietPreference) {
            DietPreference.PADRAO -> rawComplements
            DietPreference.OVOLACTO -> rawComplements.sortedWith(compareBy { entry ->
                when (entry.type) {
                    DietPreference.OVOLACTO -> 0
                    DietPreference.VEGANO -> 1
                    DietPreference.PADRAO -> 2
                }
            })
            DietPreference.VEGANO -> rawComplements.sortedWith(compareBy { entry ->
                when (entry.type) {
                    DietPreference.VEGANO -> 0
                    DietPreference.OVOLACTO -> 1
                    DietPreference.PADRAO -> 2
                }
            })
        }

        sortedComplements.forEachIndexed { idx, dish ->
            val isHighlight = (idx == 0 && dish.type == dietPreference) ||
                    (dietPreference == DietPreference.PADRAO && dish.type == DietPreference.PADRAO)
            val (tagBg, tagText) = when (dish.type) {
                DietPreference.PADRAO -> Pair(TagStandardBg, TagStandardText)
                DietPreference.OVOLACTO -> Pair(TagOvolactoBg, TagOvolactoText)
                DietPreference.VEGANO -> Pair(TagVeggieBg, TagVeggieText)
            }
            DishRow(
                tag = dish.tag,
                tagBg = tagBg,
                tagText = tagText,
                dishName = dish.name,
                isHighlight = isHighlight
            )
        }

        if (menu.opcaoExtra.isNotBlank()) {
            SimpleItemRow(label = "Extra", value = menu.opcaoExtra)
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
            SimpleItemRow(label = "Fruta", value = menu.fruta)
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
            text = dishName.cleanTrailingText(),
            style = if (isHighlight) NotionTypography.titleSmall else NotionTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun String.cleanTrailingText(): String =
    this.replace(Regex("""\s+(ou|e)\s*$""", RegexOption.IGNORE_CASE), "").trim()

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
            text = value.cleanTrailingText(),
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
