package com.roteiro.app.context

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class Fix(val lat: Double, val lng: Double, val accuracyM: Float, val time: Long)

/** Posição atual sob demanda (quando o app abre). Nada de GPS contínuo. */
class LocationSource(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun current(): Fix? {
        if (!Permissions.hasLocation(context)) return null
        return try {
            val cts = CancellationTokenSource()
            val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                ?: client.lastLocation.await()
            loc?.let { Fix(it.latitude, it.longitude, it.accuracy, it.time) }
        } catch (e: Exception) {
            null
        }
    }
}
