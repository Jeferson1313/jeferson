package com.roteiro.app.context

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

data class Fix(val lat: Double, val lng: Double, val accuracyM: Float, val time: Long)

/**
 * Posição do usuário.
 * - [current]: uma leitura pontual (ao abrir o app, no botão "minha localização").
 * - [updates]: acompanhamento contínuo, só enquanto um mapa está na tela.
 * Com o app fechado, quem acompanha é o próprio Android, pelas cercas virtuais.
 */
class LocationSource(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Leitura pontual. [precise] = GPS (mais exato, usado quando o usuário pede);
     * senão, modo econômico (Wi-Fi/antenas), suficiente para saber o lugar ao abrir o app.
     */
    @SuppressLint("MissingPermission")
    suspend fun current(precise: Boolean = false): Fix? {
        if (!Permissions.hasLocation(context)) return null
        return try {
            val cts = CancellationTokenSource()
            val priority = if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val loc = client.getCurrentLocation(priority, cts.token).await()
                ?: client.lastLocation.await()
            loc?.let { Fix(it.latitude, it.longitude, it.accuracy, it.time) }
        } catch (e: Exception) {
            null
        }
    }

    /** Posições em tempo real (GPS, a cada ~2 s). Para ao parar de coletar o Flow. */
    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long = 2_000): Flow<Fix> {
        if (!Permissions.hasLocation(context)) return emptyFlow()
        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(1_000)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(Fix(it.latitude, it.longitude, it.accuracy, it.time)) }
                }
            }
            // Mostra logo a última posição conhecida enquanto o GPS "esquenta".
            try {
                client.lastLocation.await()?.let { trySend(Fix(it.latitude, it.longitude, it.accuracy, it.time)) }
            } catch (e: Exception) { /* sem posição anterior */ }
            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                close()
            }
            awaitClose { client.removeLocationUpdates(callback) }
        }
    }
}
