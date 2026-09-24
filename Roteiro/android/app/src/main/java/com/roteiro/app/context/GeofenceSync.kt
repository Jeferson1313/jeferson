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
    suspend fun sync(places: List<PlaceEntity>, dwellMinutes: Int) {
        try {
            client.removeGeofences(pendingIntent).await()
        } catch (e: Exception) {
            Log.w(TAG, "removeGeofences falhou", e)
        }
        if (places.isEmpty() || !Permissions.hasLocation(context)) return

        val fences = places.take(MAX_FENCES).map { p ->
            Geofence.Builder()
                .setRequestId(p.id.toString())
                .setCircularRegion(p.lat, p.lng, p.radiusM.coerceAtLeast(MIN_RADIUS_M).toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(dwellMinutes.coerceAtLeast(1) * 60_000)
                .setNotificationResponsiveness(30_000)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(fences)
            .build()
        try {
            client.addGeofences(request, pendingIntent).await()
        } catch (e: Exception) {
            // Localização desligada ou sem permissão "o tempo todo": o app segue funcionando ao ser aberto.
            Log.w(TAG, "addGeofences falhou", e)
        }
    }

    private companion object {
        const val TAG = "GeofenceSync"
        const val MAX_FENCES = 100 // limite do Android por app
        const val MIN_RADIUS_M = 80 // abaixo disso o sistema erra muito
    }
}
