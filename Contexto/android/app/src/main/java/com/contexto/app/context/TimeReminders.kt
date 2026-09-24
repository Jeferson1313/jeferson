package com.contexto.app.context

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.contexto.app.data.ItemEntity
import com.contexto.core.RemindWhen
import com.contexto.core.Rules
import java.util.Calendar

/**
 * Agenda os lembretes por horário no AlarmManager.
 * Usa alarmes inexatos (sem a permissão de alarme exato): podem atrasar alguns minutos,
 * o que é aceitável para lembretes pessoais.
 */
class TimeReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun syncAll(items: List<ItemEntity>, now: Long = System.currentTimeMillis()) {
        items.forEach { item ->
            cancel(item.id)
            val t = item.timeOfDayMin ?: return@forEach
            if (item.remind != RemindWhen.TIME || item.done || item.archived) return@forEach
            val firedToday = item.lastFiredAt?.let { it >= todayAt(t, now) } ?: false
            val next = Rules.nextTimeTrigger(t, item.repeat, now, firedToday) ?: return@forEach
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(item.id, create = true)!!)
        }
    }

    private fun cancel(itemId: Long) {
        pending(itemId, create = false)?.let { alarms.cancel(it); it.cancel() }
    }

    private fun pending(itemId: Long, create: Boolean): PendingIntent? {
        val intent = Intent(context, TimeReminderReceiver::class.java).putExtra(TimeReminderReceiver.EXTRA_ITEM, itemId)
        val flags = PendingIntent.FLAG_IMMUTABLE or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, (itemId % Int.MAX_VALUE).toInt(), intent, flags)
    }

    private fun todayAt(minutes: Int, now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.MINUTE, minutes)
    }.timeInMillis
}
