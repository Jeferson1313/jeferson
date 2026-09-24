package com.contexto.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Distância em metros entre dois pontos (fórmula de haversine). */
    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    /** "80 m", "350 m", "1,2 km", "12 km". */
    fun formatDistance(meters: Double): String = when {
        meters < 1000 -> "${((meters / 10).roundToInt() * 10).coerceAtLeast(10)} m"
        meters < 10_000 -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f km", meters / 1000)
        else -> "${(meters / 1000).roundToInt()} km"
    }

    /**
     * Onde o usuário está, dada uma posição e sua precisão.
     * - [Detection.Inside]: a posição cai dentro do raio de um lugar e a precisão é boa.
     * - [Detection.Maybe]: um lugar está perto, mas a precisão não permite afirmar.
     * - [Detection.Outside]: nenhum lugar por perto.
     */
    fun detect(lat: Double, lng: Double, accuracyM: Float, places: List<PlaceInfo>): Detection {
        val nearest = places
            .map { it to distanceM(lat, lng, it.lat, it.lng) }
            .minByOrNull { it.second }
            ?: return Detection.Outside
        val (place, dist) = nearest
        val goodFix = accuracyM <= GOOD_ACCURACY_M
        return when {
            dist <= place.radiusM && goodFix -> Detection.Inside(place, dist)
            dist <= place.radiusM + accuracyM -> Detection.Maybe(place, dist)
            else -> Detection.Outside
        }
    }

    const val GOOD_ACCURACY_M = 75f
}

sealed interface Detection {
    data class Inside(val place: PlaceInfo, val distanceM: Double) : Detection
    data class Maybe(val place: PlaceInfo, val distanceM: Double) : Detection
    data object Outside : Detection
}
