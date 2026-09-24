package com.roteiro.app.context

import android.content.Context
import android.util.Log
import com.roteiro.core.Category
import com.roteiro.core.Geo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Um estabelecimento do OpenStreetMap (mercado, padaria…) perto do usuário. */
data class Store(val id: String, val category: Category, val name: String?, val lat: Double, val lng: Double)

/** Lista baixada, com o ponto de onde foi buscada. */
data class StoreCache(
    val centerLat: Double,
    val centerLng: Double,
    val fetchedAt: Long,
    val categories: Set<Category>,
    val stores: List<Store>,
)

/**
 * Busca estabelecimentos por tipo no OpenStreetMap (API Overpass, gratuita, sem chave).
 * O resultado fica num arquivo e só é buscado de novo quando o usuário se afasta
 * [REFRESH_DISTANCE_M] do ponto da última busca, passa um dia, ou surge um tipo novo.
 */
class StoreFinder(context: Context) {
    private val file = File(context.filesDir, "stores.json")
    @Volatile private var memory: StoreCache? = null

    fun cached(): StoreCache? = memory ?: read()?.also { memory = it }

    fun needsRefresh(lat: Double, lng: Double, categories: Set<Category>, now: Long = System.currentTimeMillis()): Boolean {
        if (categories.isEmpty()) return false
        val c = cached() ?: return true
        return !c.categories.containsAll(categories) ||
            now - c.fetchedAt > MAX_AGE_MS ||
            Geo.distanceM(lat, lng, c.centerLat, c.centerLng) > REFRESH_DISTANCE_M
    }

    /** Estabelecimentos mais próximos de ([lat], [lng]), no máximo [limit]. */
    fun nearest(lat: Double, lng: Double, categories: Set<Category>, limit: Int): List<Store> =
        cached()?.stores.orEmpty()
            .filter { it.category in categories }
            .sortedBy { Geo.distanceM(lat, lng, it.lat, it.lng) }
            .take(limit)

    fun find(id: String): Store? = cached()?.stores?.firstOrNull { it.id == id }

    /** Busca na internet. Em caso de falha devolve null e mantém a lista anterior. */
    suspend fun refresh(lat: Double, lng: Double, categories: Set<Category>): StoreCache? = withContext(Dispatchers.IO) {
        if (categories.isEmpty()) return@withContext null
        // Agrupa por chave: nwr["shop"~"^(supermarket|butcher|...)$"](around:...);
        val byKey = categories.flatMap { it.osm }.distinct().map { it.split("=") }.groupBy({ it[0] }, { it[1] })
        val filters = byKey.entries.joinToString("") { (k, values) ->
            "nwr[\"$k\"~\"^(${values.joinToString("|")})$\"](around:$SEARCH_RADIUS_M,$lat,$lng);"
        }
        val query = "[out:json][timeout:10];($filters);out center $MAX_RESULTS;"
        try {
            val conn = (URL(OVERPASS).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "Roteiro/1.0 (Android)")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val stores = parse(body, categories)
            val cache = StoreCache(lat, lng, System.currentTimeMillis(), categories, stores)
            memory = cache
            write(cache)
            cache
        } catch (e: Exception) {
            Log.w("StoreFinder", "Busca de estabelecimentos falhou", e)
            null
        }
    }

    private fun parse(body: String, categories: Set<Category>): List<Store> {
        val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val out = mutableListOf<Store>()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val tagsJson = e.optJSONObject("tags") ?: continue
            val tags = tagsJson.keys().asSequence().associateWith { tagsJson.optString(it) }
            val center = e.optJSONObject("center")
            val lat = if (e.has("lat")) e.getDouble("lat") else center?.optDouble("lat") ?: continue
            val lng = if (e.has("lon")) e.getDouble("lon") else center?.optDouble("lon") ?: continue
            // Uma padaria vale para "qualquer padaria" e para "qualquer mercado": uma entrada por tipo.
            categories.filter { it.matches(tags) }.forEach { category ->
                out += Store("${category.key}:${e.optString("type")}${e.optLong("id")}", category, tags["name"]?.ifBlank { null }, lat, lng)
            }
        }
        return out
    }

    private fun read(): StoreCache? = try {
        if (!file.exists()) null else {
            val o = JSONObject(file.readText())
            val arr = o.getJSONArray("stores")
            if (o.optInt("v") != CACHE_VERSION) null else StoreCache(
                o.getDouble("lat"), o.getDouble("lng"), o.getLong("at"),
                o.getString("cats").split(",").mapNotNull { Category.of(it) }.toSet(),
                (0 until arr.length()).mapNotNull { i ->
                    val s = arr.getJSONObject(i)
                    val c = Category.of(s.getString("c")) ?: return@mapNotNull null
                    Store(s.getString("id"), c, s.optString("n").ifBlank { null }, s.getDouble("lat"), s.getDouble("lng"))
                },
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun write(c: StoreCache) {
        val arr = JSONArray()
        c.stores.forEach { s -> arr.put(JSONObject().put("id", s.id).put("c", s.category.key).put("n", s.name ?: "").put("lat", s.lat).put("lng", s.lng)) }
        val o = JSONObject().put("v", CACHE_VERSION).put("lat", c.centerLat).put("lng", c.centerLng).put("at", c.fetchedAt)
            .put("cats", c.categories.joinToString(",") { it.key }).put("stores", arr)
        try { file.writeText(o.toString()) } catch (e: Exception) { Log.w("StoreFinder", "Não salvou a lista", e) }
    }

    companion object {
        /** Sobe quando muda o que conta como cada tipo; listas antigas são buscadas de novo. */
        private const val CACHE_VERSION = 2
        private const val OVERPASS = "https://overpass-api.de/api/interpreter"
        const val SEARCH_RADIUS_M = 3000
        /** Ao sair deste raio em volta da última busca, o app busca de novo. */
        const val REFRESH_DISTANCE_M = 1500.0
        private const val MAX_AGE_MS = 24 * 60 * 60_000L
        private const val MAX_RESULTS = 80
        /** Raio da cerca de cada estabelecimento. */
        const val STORE_RADIUS_M = 80
    }
}
