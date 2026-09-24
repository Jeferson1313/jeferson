package com.roteiro.app

import android.app.Application
import android.content.Context
import com.roteiro.app.context.ContextEngine
import com.roteiro.app.context.GeofenceSync
import com.roteiro.app.context.LocationSource
import com.roteiro.app.context.Notifier
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
