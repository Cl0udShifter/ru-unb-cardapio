package br.unb.ru.data.model

import com.google.gson.annotations.SerializedName

data class WeeklyMenu(
    @SerializedName("campus") val campus: String = "Darcy Ribeiro",
    @SerializedName("vigencia") val vigencia: String = "",
    @SerializedName("atualizado_em") val atualizadoEm: String = "",
    @SerializedName("pdf_url") val pdfUrl: String = "",
    @SerializedName("dias") val dias: List<DailyMenu> = emptyList()
)

data class DailyMenu(
    @SerializedName("dia_semana") val diaSemana: String = "",
    @SerializedName("data") val data: String = "",
    @SerializedName("cafe") val cafe: BreakfastMenu = BreakfastMenu(),
    @SerializedName("almoco") val almoco: LunchDinnerMenu = LunchDinnerMenu(),
    @SerializedName("jantar") val jantar: LunchDinnerMenu = LunchDinnerMenu()
)

data class BreakfastMenu(
    @SerializedName("bebidas") val bebidas: String = "",
    @SerializedName("panificacao") val panificacao: String = "",
    @SerializedName("gordura") val gordura: String = "",
    @SerializedName("opcao_extra") val opcaoExtra: String = "",
    @SerializedName("prato_principal") val pratoPrincipal: String = "",
    @SerializedName("vegetariano") val vegetariano: String = "",
    @SerializedName("vegano") val vegano: String = "",
    @SerializedName("fruta") val fruta: String = ""
)

data class LunchDinnerMenu(
    @SerializedName("salada_1") val salada1: String = "",
    @SerializedName("salada_2") val salada2: String = "",
    @SerializedName("molho") val molho: String = "",
    @SerializedName("prato_principal") val pratoPrincipal: String = "",
    @SerializedName("vegetariano") val vegetariano: String = "",
    @SerializedName("vegano") val vegano: String = "",
    @SerializedName("guarnicao") val guarnicao: String = "",
    @SerializedName("acompanhamentos") val acompanhamentos: String = "",
    @SerializedName("sobremesa") val sobremesa: String = "",
    @SerializedName("bebida") val bebida: String = "",
    @SerializedName("sopa") val sopa: String = "",
    @SerializedName("torrada") val torrada: String = ""
)

enum class MealType(val title: String, val shortName: String) {
    CAFE("Café da Manhã", "Café"),
    ALMOCO("Almoço", "Almoço"),
    JANTAR("Jantar", "Jantar")
}

enum class Campus(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val assetFile: String,
    val remoteFileName: String
) {
    DARCY(
        id = "darcy",
        displayName = "Darcy Ribeiro",
        subtitle = "Plano Piloto",
        assetFile = "menu_darcy.json",
        remoteFileName = "menu_darcy.json"
    ),
    GAMA(
        id = "gama",
        displayName = "FGA (Gama)",
        subtitle = "Faculdade do Gama",
        assetFile = "menu_gama.json",
        remoteFileName = "menu_gama.json"
    ),
    CEILANDIA(
        id = "ceilandia",
        displayName = "FCE (Ceilândia)",
        subtitle = "Faculdade de Ceilândia",
        assetFile = "menu_ceilandia.json",
        remoteFileName = "menu_ceilandia.json"
    ),
    PLANALTINA(
        id = "planaltina",
        displayName = "FUP (Planaltina)",
        subtitle = "Faculdade de Planaltina",
        assetFile = "menu_planaltina.json",
        remoteFileName = "menu_planaltina.json"
    );

    companion object {
        fun fromId(id: String?): Campus {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARCY
        }
    }
}
