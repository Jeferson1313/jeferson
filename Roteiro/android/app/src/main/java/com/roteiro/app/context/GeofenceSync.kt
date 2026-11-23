package com.roteiro.app.context

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roteiro.app.data.PlaceEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

/**
 * Registra uma cerca virtual por lugar no sistema (Google Play Services).
 * O sistema acorda o [GeofenceReceiver] ao entrar, permanecer e sair.
 * As cercas somem ao reiniciar o aparelho: o [BootReceiver] e a abertura do app chamam [sync] de novo.
 */
class GeofenceSync(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java).setAction(GeofenceReceiver.ACTION)
        // O Play Services preenche o intent, por isso ele precisa ser mutável.
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    @SuppressLint("MissingPermission")
    /**
     * [stores]: estabelecimentos por perto para lembretes do tipo "qualquer mercado".
     * [refreshCenter]: centro da última busca de estabelecimentos; ao sair dele, buscamos de novo.
     */
    suspend fun sync(places: List<PlaceEntity>, dwellMinutes: Int, stores: List<Store> = emptyList(), refreshCenter: Pair<Double, Double>? = null) {
        try {
            client.removeGeofences(pendingIntent).await()
        } catch (e: Exception) {
            Log.w(TAG, "removeGeofences falhou", e)
        }
        val geo = places.filter { it.isGeo }
        if ((geo.isEmpty() && stores.isEmpty()) || !Permissions.hasLocation(context)) return

        val placeFences = geo.take(MAX_PLACES).map { p ->
            Geofence.Builder()
                .setRequestId(p.id.toString())
                .setCircularRegion(p.lat, p.lng, p.radiusM.coerceAtLeast(MIN_RADIUS_M).toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(dwellMinutes.coerceAtLeast(1) * 60_000)
                .setNotificationResponsiveness(30_000)
                .build()
        }
        // Estabelecimentos: basta passar devagar ou parar 1 min por perto.
        val room = MAX_FENCES - placeFences.size - 1
        val storeFences = stores.take(room.coerceAtLeast(0)).map { s ->
            Geofence.Builder()
                .setRequestId(STORE_PREFIX + s.id)
                .setCircularRegion(s.lat, s.lng, StoreFinder.STORE_RADIUS_M.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(60_000)
                .setNotificationResponsiveness(30_000)
                .build()
        }
        val refreshFence = refreshCenter?.takeIf { stores.isNotEmpty() }?.let { (lat, lng) ->
            Geofence.Builder()
                .setRequestId(REFRESH_ID)
                .setCircularRegion(lat, lng, StoreFinder.REFRESH_DISTANCE_M.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(120_000)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(placeFences + storeFences + listOfNotNull(refreshFence))
            .build()
        try {
            client.addGeofences(request, pendingIntent).await()
        } catch (e: Exception) {
            // Localização desligada ou sem permissão "o tempo todo": o app segue funcionando ao ser aberto.
            Log.w(TAG, "addGeofences falhou", e)
        }
    }

    companion object {
        private const val TAG = "GeofenceSync"
        private const val MAX_FENCES = 100 // limite do Android por app
        private const val MAX_PLACES = 60
        /** Aceitamos raios pequenos, mas abaixo de ~80 m o Android pode demorar a perceber. */
        private const val MIN_RADIUS_M = PlaceEntity.MIN_RADIUS_M
        const val STORE_PREFIX = "s:"
        const val REFRESH_ID = "refresh"
    }
}
