package br.unb.ru.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.unb.ru.data.model.MealType
import java.util.Calendar

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        // Café da manhã: refeição às 07:00 -> notificação às 06:00
        scheduleMealAlarm(context, MealType.CAFE, targetHour = 6, targetMinute = 0)
        // Almoço: refeição às 11:00 -> notificação às 10:00
        scheduleMealAlarm(context, MealType.ALMOCO, targetHour = 10, targetMinute = 0)
        // Jantar: refeição às 17:00 -> notificação às 16:00
        scheduleMealAlarm(context, MealType.JANTAR, targetHour = 16, targetMinute = 0)
    }

    fun cancelMealAlarm(context: Context, mealType: MealType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MenuNotificationReceiver::class.java).apply {
            putExtra(MenuNotificationReceiver.EXTRA_MEAL_TYPE, mealType.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mealType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleMealAlarm(
        context: Context,
        mealType: MealType,
        targetHour: Int,
        targetMinute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Se já passou do horário hoje, agenda para amanhã
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MenuNotificationReceiver::class.java).apply {
            putExtra(MenuNotificationReceiver.EXTRA_MEAL_TYPE, mealType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mealType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
