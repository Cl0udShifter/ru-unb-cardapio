package br.unb.ru.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.unb.ru.MainActivity
import br.unb.ru.R
import br.unb.ru.data.model.MealType
import br.unb.ru.data.repository.DietPreference
import br.unb.ru.data.repository.MenuRepository
import br.unb.ru.data.repository.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MenuNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEAL_TYPE = "extra_meal_type"
        const val CHANNEL_ID = "ru_unb_meal_alerts"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Avisos de Refeição (RU UnB)",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notificações enviadas 1 hora antes de cada refeição do RU"
                        enableVibration(true)
                        enableLights(true)
                        setShowBadge(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra(NotificationScheduler.EXTRA_TEST, false)
        val mealKey = intent.getStringExtra(EXTRA_MEAL_TYPE) ?: MealType.ALMOCO.name
        val mealType = try {
            MealType.valueOf(mealKey)
        } catch (_: Exception) {
            MealType.ALMOCO
        }

        // Se NÃO for teste, reagenda imediatamente o próximo alarme exato para garantir recorrência diária
        if (!isTest) {
            NotificationScheduler.scheduleMeal(context, mealType)
        }

        val config = NotificationPreferences(context).getConfig()

        // Se não for teste, valida se o usuário quer receber notificação desta refeição hoje
        if (!isTest) {
            val calendar = Calendar.getInstance()
            val currentDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            val isEnabledForToday = when (mealType) {
                MealType.CAFE -> config.cafeEnabled && config.cafeDays.contains(currentDayOfWeek)
                MealType.ALMOCO -> config.almocoEnabled && config.almocoDays.contains(currentDayOfWeek)
                MealType.JANTAR -> config.jantarEnabled && config.jantarDays.contains(currentDayOfWeek)
            }
            if (!isEnabledForToday) return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dishText: String = if (isTest) {
                    "Notificação de teste configurada com sucesso!"
                } else {
                    val repo = MenuRepository(context)
                    val menuResult = repo.getMenu()
                    val weeklyMenu = menuResult.getOrNull()

                    val calendar = Calendar.getInstance()
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val todayIdx = when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6
                        else -> 0
                    }

                    val dailyMenu = weeklyMenu?.dias?.getOrNull(todayIdx)

                    when (mealType) {
                        MealType.CAFE -> {
                            when (config.dietPreference) {
                                DietPreference.VEGANO -> dailyMenu?.cafe?.vegano?.ifBlank { null }
                                DietPreference.OVOLACTO -> dailyMenu?.cafe?.vegetariano?.ifBlank { null }
                                DietPreference.PADRAO -> dailyMenu?.cafe?.pratoPrincipal?.ifBlank { null }
                            } ?: dailyMenu?.cafe?.pratoPrincipal ?: "Pães, café, leite e complementos"
                        }
                        MealType.ALMOCO -> {
                            when (config.dietPreference) {
                                DietPreference.VEGANO -> dailyMenu?.almoco?.vegano?.ifBlank { null }
                                DietPreference.OVOLACTO -> dailyMenu?.almoco?.vegetariano?.ifBlank { null }
                                DietPreference.PADRAO -> dailyMenu?.almoco?.pratoPrincipal?.ifBlank { null }
                            } ?: dailyMenu?.almoco?.pratoPrincipal ?: "Cardápio do almoço disponível"
                        }
                        MealType.JANTAR -> {
                            when (config.dietPreference) {
                                DietPreference.VEGANO -> dailyMenu?.jantar?.vegano?.ifBlank { null }
                                DietPreference.OVOLACTO -> dailyMenu?.jantar?.vegetariano?.ifBlank { null }
                                DietPreference.PADRAO -> dailyMenu?.jantar?.pratoPrincipal?.ifBlank { null }
                            } ?: dailyMenu?.jantar?.pratoPrincipal ?: "Cardápio do jantar disponível"
                        }
                    }
                }

                showNotification(context, mealType, dishText, config.dietPreference, isTest)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        mealType: MealType,
        dishText: String,
        diet: DietPreference,
        isTest: Boolean
    ) {
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MEAL_TYPE, mealType.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            mealType.ordinal,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, iconPrefix) = if (isTest) {
            Pair("Teste de Notificação (RU UnB)", "🔔")
        } else {
            when (mealType) {
                MealType.CAFE -> Pair("Café da Manhã (7h às 9h30)", "☕")
                MealType.ALMOCO -> Pair("Almoço do RU (11h às 14h30)", "🍽️")
                MealType.JANTAR -> Pair("Jantar do RU (17h às 19h30)", "🥣")
            }
        }

        val dietLabel = if (isTest) "" else when (diet) {
            DietPreference.PADRAO -> ""
            DietPreference.OVOLACTO -> " [Ovolacto]"
            DietPreference.VEGANO -> " [Vegano]"
        }

        val body = if (isTest) {
            "$iconPrefix As notificações do RU estão prontas e ativas no seu aparelho!"
        } else {
            "$iconPrefix Hoje: $dishText$dietLabel • Toque para ver"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ru_menu)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (isTest) 999 else mealType.ordinal + 100
        manager.notify(notificationId, notification)
    }
}
