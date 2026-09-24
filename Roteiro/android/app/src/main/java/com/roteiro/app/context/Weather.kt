package com.roteiro.app.context

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Tempo no momento, guardado da última vez que houve internet. */
data class Weather(val tempC: Int, val code: Int, val at: Long) {
    /** Descrição curta a partir do código WMO usado pelo Open-Meteo. */
    val description: String
        get() = when (code) {
            0 -> "Céu limpo"
            1 -> "Poucas nuvens"
            2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Neblina"
            51, 53, 55, 56, 57 -> "Garoa"
            61, 63, 66 -> "Chuva"
            65, 67 -> "Chuva forte"
            71, 73, 75, 77, 85, 86 -> "Neve"
            80, 81 -> "Pancadas de chuva"
            82 -> "Chuva forte"
            95, 96, 99 -> "Tempestade"
            else -> "Tempo variável"
        }

    val kind: Kind
        get() = when (code) {
            0, 1 -> Kind.CLEAR
            2, 3 -> Kind.CLOUDY
            45, 48 -> Kind.FOG
            in 51..67, 80, 81, 82 -> Kind.RAIN
            95, 96, 99 -> Kind.STORM
            in 71..86 -> Kind.SNOW
            else -> Kind.CLOUDY
        }

    enum class Kind { CLEAR, CLOUDY, FOG, RAIN, STORM, SNOW }
}

/** Busca o tempo atual no Open-Meteo (gratuito, sem chave) no máximo a cada 30 minutos. */
class WeatherSource(context: Context) {
    private val sp = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val _weather = MutableStateFlow(read())
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    suspend fun refreshIfStale(lat: Double, lng: Double, now: Long = System.currentTimeMillis()) {
        val current = _weather.value
        if (current != null && now - current.at < STALE_MS) return
        val fresh = fetch(lat, lng) ?: return
        _weather.value = fresh
        sp.edit { putInt("t", fresh.tempC); putInt("c", fresh.code); putLong("at", fresh.at) }
    }

    private fun read(): Weather? =
        if (!sp.contains("at")) null else Weather(sp.getInt("t", 0), sp.getInt("c", 0), sp.getLong("at", 0))

    private suspend fun fetch(lat: Double, lng: Double): Weather? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&timezone=auto".format(java.util.Locale.US, lat, lng))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "Roteiro/1.0 (Android)")
            }
            if (conn.responseCode != 200) return@withContext null
            val current = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
            Weather(current.getDouble("temperature_2m").roundToInt(), current.getInt("weather_code"), System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w("WeatherSource", "Sem tempo atualizado", e)
            null
        }
    }

    private companion object {
        const val STALE_MS = 30 * 60_000L
    }
}
