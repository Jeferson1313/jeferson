package com.contexto.app

import android.app.Application
import android.content.Context
import com.contexto.app.context.ContextEngine
import com.contexto.app.context.GeofenceSync
import com.contexto.app.context.LocationSource
import com.contexto.app.context.Notifier
import com.contexto.app.context.TimeReminders
import com.contexto.app.data.AppDatabase
import com.contexto.app.data.Prefs
import com.contexto.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class ContextoApp : Application() {
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
    private val geofences = GeofenceSync(context)
    private val reminders = TimeReminders(context)

    val repo: Repository = Repository(
        db = db,
        prefs = prefs,
        onPlacesChanged = { syncGeofences() },
        onItemsChanged = { syncReminders() },
    )

    val engine = ContextEngine(repo, prefs, notifier, location)

    suspend fun syncGeofences() = geofences.sync(repo.allPlaces(), prefs.settings.value.dwellMinutes)
    suspend fun syncReminders() = reminders.syncAll(repo.allItems())

    suspend fun syncAll() {
        repo.resetRepeats()
        syncGeofences()
        syncReminders()
    }
}
