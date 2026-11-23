package com.roteiro.app.context

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roteiro.app.RoteiroApp
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Executa trabalho assíncrono curto dentro de um BroadcastReceiver. */
private fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e("Roteiro", "Falha no receiver", e)
        } finally {
            pending.finish()
        }
    }
}

private val Context.container get() = (applicationContext as RoteiroApp).container

/** Recebe as transições das cercas virtuais: permanência = chegada, saída = saída. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w("GeofenceReceiver", "Erro de cerca: ${event.errorCode}")
            return
        }
        val requestIds = event.triggeringGeofences.orEmpty().map { it.requestId }
        val placeIds = requestIds.mapNotNull { it.toLongOrNull() }
        val storeIds = requestIds.filter { it.startsWith(GeofenceSync.STORE_PREFIX) }.map { it.removePrefix(GeofenceSync.STORE_PREFIX) }
        val refresh = GeofenceSync.REFRESH_ID in requestIds
        val where = event.triggeringLocation
        val c = context.container
        launchAsync {
            when (event.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    placeIds.forEach { c.engine.onArrive(it, fromSystem = true) }
                    storeIds.forEach { c.engine.onStoreDwell(it) }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    placeIds.forEach { c.engine.onLeave(it, fromSystem = true) }
                    storeIds.forEach { c.engine.onStoreExit(it) }
                    // Saiu da região da última busca: busca estabelecimentos em volta da nova posição.
                    if (refresh && where != null) c.refreshStores(where.latitude, where.longitude)
                }
            }
        }
    }

    companion object {
        const val ACTION = "com.roteiro.app.GEOFENCE"
    }
}

/** Botões das notificações: "Peguei/Feito", "Próxima vez", "Mais tarde". */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val notifId = intent.getIntExtra(EXTRA_NOTIF, -1)
        launchAsync {
            when (intent.action) {
                ACTION_DONE -> c.repo.setDone(intent.getLongExtra(EXTRA_ITEM, -1), true)
                ACTION_SNOOZE_PLACE -> c.repo.snoozePlaceTasks(intent.getLongExtra(EXTRA_PLACE, -1))
                ACTION_DISMISS -> Unit
            }
            if (notifId >= 0) c.notifier.cancel(notifId)
        }
    }

    companion object {
        const val ACTION_DONE = "com.roteiro.app.DONE"
        const val ACTION_SNOOZE_PLACE = "com.roteiro.app.SNOOZE_PLACE"
        const val ACTION_DISMISS = "com.roteiro.app.DISMISS"
        const val EXTRA_ITEM = "item"
        const val EXTRA_PLACE = "place"
        const val EXTRA_NOTIF = "notif"
    }
}

/** Lembrete por horário disparado pelo AlarmManager. */
class TimeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val itemId = intent.getLongExtra(EXTRA_ITEM, -1)
        launchAsync {
            c.repo.resetRepeats()
            val item = c.repo.item(itemId) ?: return@launchAsync
            if (!item.done && !item.archived) {
                val placeName = item.placeId?.let { c.repo.place(it)?.name }
                c.notifier.showTime(item, placeName)
                c.repo.markFired(item.id, System.currentTimeMillis())
            }
            c.syncReminders()
        }
    }

    companion object {
        const val EXTRA_ITEM = "item"
    }
}

/** Cercas virtuais e alarmes não sobrevivem a um reinício: registra tudo de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val c = context.container
        launchAsync { c.syncAll() }
    }
}
