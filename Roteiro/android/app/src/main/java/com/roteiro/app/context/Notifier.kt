package com.roteiro.app.context

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roteiro.app.MainActivity
import com.roteiro.app.R
import com.roteiro.app.data.ItemEntity
import com.roteiro.app.data.PlaceEntity
import com.roteiro.app.data.Prefs
import com.roteiro.core.Notice
import java.util.Calendar

/**
 * Todas as notificações do app. Regras do design:
 * uma notificação por chegada, no máximo 2 ações, resolvíveis sem abrir o app,
 * sem som no horário silencioso.
 */
class Notifier(private val context: Context, private val prefs: Prefs) {
    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CH_PLACES, "Chegadas e saídas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos quando você chega ou sai de um lugar com pendências."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CH_TIME, "Horários", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lembretes marcados para um horário."
            }
        )
    }

    fun showArrival(place: PlaceEntity, notice: Notice) {
        val id = arrivalId(place.id)
        val builder = base(CH_PLACES, notice)
            .setSubText(place.name)
            .setContentIntent(openApp(id, MainActivity.OPEN_AGORA, place.id))
            .addAction(0, "Ver", openApp(id + 1, MainActivity.OPEN_AGORA, place.id))
            .addAction(0, "Mais tarde", action(id, NotificationActionReceiver.ACTION_DISMISS) { putExtra(NotificationActionReceiver.EXTRA_NOTIF, id) })
        post(id, builder)
    }

    fun showLeave(place: PlaceEntity, notice: Notice, pending: List<ItemEntity>) {
        val id = leaveId(place.id)
        val builder = base(CH_PLACES, notice)
            .setSubText(place.name)
            .setContentIntent(openApp(id, MainActivity.OPEN_LEAVE, place.id))
        if (pending.size == 1) {
            builder.addAction(0, "Peguei", action(id, NotificationActionReceiver.ACTION_DONE) {
                putExtra(NotificationActionReceiver.EXTRA_ITEM, pending[0].id)
                putExtra(NotificationActionReceiver.EXTRA_NOTIF, id)
            })
        } else {
            builder.addAction(0, "Ver", openApp(id + 1, MainActivity.OPEN_LEAVE, place.id))
        }
        builder.addAction(0, "Próxima vez", action(id + 2, NotificationActionReceiver.ACTION_SNOOZE_PLACE) {
            putExtra(NotificationActionReceiver.EXTRA_PLACE, place.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF, id)
        })
        post(id, builder)
    }

    fun showTime(item: ItemEntity, placeName: String?) {
        val id = timeId(item.id)
        val notice = Notice(item.title, placeName?.let { "Lembrete · $it" } ?: "Lembrete", emptyList())
        val builder = base(CH_TIME, notice)
            .setContentIntent(openApp(id, MainActivity.OPEN_AGORA, null))
            .addAction(0, "Feito", action(id, NotificationActionReceiver.ACTION_DONE) {
                putExtra(NotificationActionReceiver.EXTRA_ITEM, item.id)
                putExtra(NotificationActionReceiver.EXTRA_NOTIF, id)
            })
        post(id, builder)
    }

    fun cancel(id: Int) = nm.cancel(id)
    fun cancelArrival(placeId: Long) = nm.cancel(arrivalId(placeId))
    fun cancelLeave(placeId: Long) = nm.cancel(leaveId(placeId))

    private fun base(channel: String, notice: Notice): NotificationCompat.Builder {
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(inQuietHours())
        if (notice.lines.size > 1) {
            val style = NotificationCompat.InboxStyle().setSummaryText(notice.body)
            notice.lines.forEach { style.addLine(it) }
            b.setStyle(style)
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(notice.body))
        }
        return b
    }

    private fun inQuietHours(): Boolean {
        if (!prefs.settings.value.quietHours) return false
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h >= 22 || h < 7
    }

    @SuppressLint("MissingPermission")
    private fun post(id: Int, builder: NotificationCompat.Builder) {
        if (!Permissions.hasNotifications(context)) return
        try {
            nm.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Permissão de notificação revogada entre a checagem e o envio.
        }
    }

    private fun openApp(requestCode: Int, open: String, placeId: Long?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, open)
        if (placeId != null) intent.putExtra(MainActivity.EXTRA_PLACE, placeId)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(requestCode: Int, action: String, extras: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action).apply(extras)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CH_PLACES = "places"
        const val CH_TIME = "time"
        private const val ACCENT = 0xFF2F4FF5.toInt()

        fun arrivalId(placeId: Long) = 100_000 + (placeId % 100_000).toInt() * 4
        fun leaveId(placeId: Long) = 600_000 + (placeId % 100_000).toInt() * 4
        fun timeId(itemId: Long) = 1_100_000 + (itemId % 100_000).toInt() * 4
    }
}
