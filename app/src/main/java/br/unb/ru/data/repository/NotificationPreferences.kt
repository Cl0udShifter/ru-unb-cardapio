package br.unb.ru.data.repository

import android.content.Context

enum class DietPreference(val label: String, val key: String) {
    PADRAO("Padrão", "padrao"),
    OVOLACTO("Ovolacto", "ovolacto"),
    VEGANO("Vegano", "vegano")
}

data class NotificationConfig(
    val cafeEnabled: Boolean = true,
    val cafeDays: Set<Int> = setOf(1, 2, 3, 4, 5), // 1=Seg..5=Sex
    val almocoEnabled: Boolean = true,
    val almocoDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val jantarEnabled: Boolean = true,
    val jantarDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val dietPreference: DietPreference = DietPreference.PADRAO
) {
    val hasAnyNotificationActive: Boolean
        get() = (cafeEnabled && cafeDays.isNotEmpty()) ||
                (almocoEnabled && almocoDays.isNotEmpty()) ||
                (jantarEnabled && jantarDays.isNotEmpty())

    val effectiveDietPreference: DietPreference
        get() = dietPreference
}

class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ru_notification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CAFE = "notify_cafe"
        private const val KEY_CAFE_DAYS = "notify_cafe_days"
        private const val KEY_ALMOCO = "notify_almoco"
        private const val KEY_ALMOCO_DAYS = "notify_almoco_days"
        private const val KEY_JANTAR = "notify_jantar"
        private const val KEY_JANTAR_DAYS = "notify_jantar_days"
        private const val KEY_DIET = "notify_diet"
    }

    fun getConfig(): NotificationConfig {
        val dietStr = prefs.getString(KEY_DIET, DietPreference.PADRAO.key)
        val diet = DietPreference.values().firstOrNull { it.key == dietStr } ?: DietPreference.PADRAO

        fun parseDays(key: String, default: Set<Int>): Set<Int> {
            val str = prefs.getString(key, null) ?: return default
            if (str.isBlank()) return emptySet()
            return str.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }

        return NotificationConfig(
            cafeEnabled = prefs.getBoolean(KEY_CAFE, true),
            cafeDays = parseDays(KEY_CAFE_DAYS, setOf(1, 2, 3, 4, 5)),
            almocoEnabled = prefs.getBoolean(KEY_ALMOCO, true),
            almocoDays = parseDays(KEY_ALMOCO_DAYS, setOf(1, 2, 3, 4, 5)),
            jantarEnabled = prefs.getBoolean(KEY_JANTAR, true),
            jantarDays = parseDays(KEY_JANTAR_DAYS, setOf(1, 2, 3, 4, 5)),
            dietPreference = diet
        )
    }

    fun saveConfig(config: NotificationConfig) {
        prefs.edit()
            .putBoolean(KEY_CAFE, config.cafeEnabled)
            .putString(KEY_CAFE_DAYS, config.cafeDays.joinToString(","))
            .putBoolean(KEY_ALMOCO, config.almocoEnabled)
            .putString(KEY_ALMOCO_DAYS, config.almocoDays.joinToString(","))
            .putBoolean(KEY_JANTAR, config.jantarEnabled)
            .putString(KEY_JANTAR_DAYS, config.jantarDays.joinToString(","))
            .putString(KEY_DIET, config.dietPreference.key)
            .apply()
    }
}
