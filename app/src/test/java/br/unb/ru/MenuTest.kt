package br.unb.ru

import br.unb.ru.data.model.MealType
import br.unb.ru.data.model.WeeklyMenu
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader

class MenuTest {

    @Test
    fun testMenuJsonDeserialization() {
        val stream = javaClass.classLoader?.getResourceAsStream("sample_menu.json")
            ?: javaClass.getResourceAsStream("/sample_menu.json")

        // If not found in classpath directly, test with mock json
        val json = if (stream != null) {
            InputStreamReader(stream).use { it.readText() }
        } else {
            """
            {
              "campus": "Darcy Ribeiro",
              "vigencia": "26/10/2026 a 01/11/2026",
              "dias": [
                {
                  "dia_semana": "Segunda-feira",
                  "data": "26/10/2026",
                  "cafe": { "prato_principal": "Salsicha ao molho" },
                  "almoco": { "prato_principal": "Silveirinha de carne" },
                  "jantar": { "prato_principal": "Frango à pizzaiolo" }
                }
              ]
            }
            """.trimIndent()
        }

        val menu = Gson().fromJson(json, WeeklyMenu::class.java)
        assertNotNull(menu)
        assertEquals("Darcy Ribeiro", menu.campus)
        assertTrue(menu.dias.isNotEmpty())
        assertEquals("Segunda-feira", menu.dias[0].diaSemana)
        assertEquals("Silveirinha de carne", menu.dias[0].almoco.pratoPrincipal)
    }

    @Test
    fun testMealTypeEnumValues() {
        assertEquals("Café da Manhã", MealType.CAFE.title)
        assertEquals("Almoço", MealType.ALMOCO.title)
        assertEquals("Jantar", MealType.JANTAR.title)
    }
}
