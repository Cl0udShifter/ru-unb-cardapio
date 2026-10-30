package br.unb.ru.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import br.unb.ru.data.model.MealType
import br.unb.ru.data.repository.NotificationPreferences
import java.util.Calendar

object NotificationScheduler {

    const val ACTION_MEAL_ALERT = "br.unb.ru.action.MEAL_ALERT"
    const val EXTRA_TEST = "extra_is_test"

    // Horários das notificações (1 hora antes do início de cada refeição)
    private const val CAFE_HOUR = 6
    private const val CAFE_MINUTE = 0

    private const val ALMOCO_HOUR = 10
    private const val ALMOCO_MINUTE = 0

    private const val JANTAR_HOUR = 16
    private const val JANTAR_MINUTE = 0

    fun scheduleAll(context: Context) {
        val config = NotificationPreferences(context).getConfig()

        if (config.cafeEnabled) {
            scheduleMeal(context, MealType.CAFE)
        } else {
            cancelMealAlarm(context, MealType.CAFE)
        }

        if (config.almocoEnabled) {
            scheduleMeal(context, MealType.ALMOCO)
        } else {
            cancelMealAlarm(context, MealType.ALMOCO)
        }

        if (config.jantarEnabled) {
            scheduleMeal(context, MealType.JANTAR)
        } else {
            cancelMealAlarm(context, MealType.JANTAR)
        }
    }

    fun scheduleMeal(context: Context, mealType: MealType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val (targetHour, targetMinute) = when (mealType) {
            MealType.CAFE -> Pair(CAFE_HOUR, CAFE_MINUTE)
            MealType.ALMOCO -> Pair(ALMOCO_HOUR, ALMOCO_MINUTE)
            MealType.JANTAR -> Pair(JANTAR_HOUR, JANTAR_MINUTE)
        }

        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Se o horário de hoje já passou, agenda para amanhã
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MenuNotificationReceiver::class.java).apply {
            action = ACTION_MEAL_ALERT
            putExtra(MenuNotificationReceiver.EXTRA_MEAL_TYPE, mealType.name)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mealType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, targetCalendar.timeInMillis, pendingIntent)
    }

    fun cancelMealAlarm(context: Context, mealType: MealType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MenuNotificationReceiver::class.java).apply {
            action = ACTION_MEAL_ALERT
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

    fun triggerTestNotification(context: Context) {
        val intent = Intent(context, MenuNotificationReceiver::class.java).apply {
            action = ACTION_MEAL_ALERT
            putExtra(MenuNotificationReceiver.EXTRA_MEAL_TYPE, MealType.ALMOCO.name)
            putExtra(EXTRA_TEST, true)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
