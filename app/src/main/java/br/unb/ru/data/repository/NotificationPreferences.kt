package br.unb.ru.data.repository

import android.content.Context

enum class DietPreference(val label: String, val key: String) {
    PADRAO("Padrão", "padrao"),
    OVOLACTO("Ovolacto", "ovolacto"),
    VEGANO("Vegano", "vegano")
}

data class NotificationConfig(
    val cafeEnabled: Boolean = true,
    val almocoEnabled: Boolean = true,
    val jantarEnabled: Boolean = true,
    val weekendEnabled: Boolean = true,
    val dietPreference: DietPreference = DietPreference.PADRAO
)

class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ru_notification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CAFE = "notify_cafe"
        private const val KEY_ALMOCO = "notify_almoco"
        private const val KEY_JANTAR = "notify_jantar"
        private const val KEY_WEEKEND = "notify_weekend"
        private const val KEY_DIET = "notify_diet"
    }

    fun getConfig(): NotificationConfig {
        val dietStr = prefs.getString(KEY_DIET, DietPreference.PADRAO.key)
        val diet = DietPreference.values().firstOrNull { it.key == dietStr } ?: DietPreference.PADRAO
        return NotificationConfig(
            cafeEnabled = prefs.getBoolean(KEY_CAFE, true),
            almocoEnabled = prefs.getBoolean(KEY_ALMOCO, true),
            jantarEnabled = prefs.getBoolean(KEY_JANTAR, true),
            weekendEnabled = prefs.getBoolean(KEY_WEEKEND, true),
            dietPreference = diet
        )
    }

    fun saveConfig(config: NotificationConfig) {
        prefs.edit()
            .putBoolean(KEY_CAFE, config.cafeEnabled)
            .putBoolean(KEY_ALMOCO, config.almocoEnabled)
            .putBoolean(KEY_JANTAR, config.jantarEnabled)
            .putBoolean(KEY_WEEKEND, config.weekendEnabled)
            .putString(KEY_DIET, config.dietPreference.key)
            .apply()
    }
}
