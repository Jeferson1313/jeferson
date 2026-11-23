package com.roteiro.app

import android.app.Application
import android.content.Context
import com.roteiro.app.context.ContextEngine
import com.roteiro.app.context.GeofenceSync
import com.roteiro.app.context.LocationSource
import com.roteiro.app.context.Notifier
import com.roteiro.app.context.StoreFinder
import com.roteiro.app.context.WeatherSource
import com.roteiro.core.Category
import com.roteiro.app.context.TimeReminders
import com.roteiro.app.data.AppDatabase
import com.roteiro.app.data.Prefs
import com.roteiro.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class RoteiroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
        MapLibre.getInstance(this)
        container.scope.launch { container.syncAll() }
    }
}

/** Injeção de dependências manual: um objeto de cada, criado uma vez. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prefs = Prefs(context)
    private val db = AppDatabase.create(context)
    val notifier = Notifier(context, prefs)
    val location = LocationSource(context)
    val stores = StoreFinder(context)
    val weather = WeatherSource(context)
    private val geofences = GeofenceSync(context)
    private val reminders = TimeReminders(context)
    private var lastCategories: Set<Category> = emptySet()

    val repo: Repository = Repository(
        db = db,
        prefs = prefs,
        onPlacesChanged = { syncGeofences() },
        onItemsChanged = { syncReminders(); onCategoriesMaybeChanged() },
    )

    val engine = ContextEngine(repo, prefs, notifier, location, stores)

    /** Cercas: lugares salvos + estabelecimentos mais próximos dos tipos com pendência. */
    suspend fun syncGeofences() {
        val active = repo.activeCategories()
        lastCategories = active
        val cache = stores.cached()
        val nearby = if (cache == null || active.isEmpty()) emptyList() else stores.nearest(cache.centerLat, cache.centerLng, active, 38)
        geofences.sync(repo.allPlaces(), prefs.settings.value.dwellMinutes, nearby, cache?.let { it.centerLat to it.centerLng })
    }

    suspend fun syncReminders() = reminders.syncAll(repo.allItems())

    /** Busca estabelecimentos em volta de ([lat], [lng]) se for preciso, e registra as cercas. */
    suspend fun refreshStores(lat: Double, lng: Double) {
        val active = repo.activeCategories()
        if (stores.needsRefresh(lat, lng, active)) stores.refresh(lat, lng, active)
        syncGeofences()
    }

    /** Um item novo de "qualquer mercado" pede uma busca de estabelecimentos. */
    private suspend fun onCategoriesMaybeChanged() {
        val active = repo.activeCategories()
        if (active == lastCategories) return
        lastCategories = active
        scope.launch { location.current()?.let { refreshStores(it.lat, it.lng) } ?: syncGeofences() }
    }

    suspend fun syncAll() {
        repo.ensureCategoryPlaces()
        repo.resetRepeats()
        syncGeofences()
        syncReminders()
    }
}
