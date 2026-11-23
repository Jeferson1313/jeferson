package com.roteiro.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roteiro.app.AppContainer
import com.roteiro.app.RoteiroApp
import com.roteiro.app.context.Fix
import com.roteiro.app.context.LocationSnapshot
import com.roteiro.app.context.Weather
import com.roteiro.app.context.PermissionState
import com.roteiro.app.context.Permissions
import com.roteiro.app.data.ContextState
import com.roteiro.app.data.ItemEntity
import com.roteiro.app.data.PlaceEntity
import com.roteiro.app.data.Settings
import com.roteiro.core.ItemKind
import com.roteiro.core.MemoryShow
import com.roteiro.core.RemindWhen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Mensagem curta no rodapé, com "Desfazer" opcional. */
data class UiMessage(val text: String, val undo: (() -> Unit)? = null, val id: Long = System.nanoTime())

class AppViewModel(private val app: Application, private val c: AppContainer) : ViewModel() {

    val places: StateFlow<List<PlaceEntity>> = c.repo.places.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val items: StateFlow<List<ItemEntity>> = c.repo.items.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val context: StateFlow<ContextState> = c.prefs.context
    val settings: StateFlow<Settings> = c.prefs.settings

    /** Tempo no momento (última sincronização com a internet). */
    val weather: StateFlow<Weather?> = c.weather.weather

    private val _snapshot = MutableStateFlow<LocationSnapshot?>(null)
    /** Última leitura de posição (ao abrir o app). */
    val snapshot: StateFlow<LocationSnapshot?> = _snapshot.asStateFlow()

    private val _permissions = MutableStateFlow(Permissions.state(app))
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    /** Lugar do aviso "Antes de sair" aberto pela notificação. */
    private val _leaveSheet = MutableStateFlow<Long?>(null)
    val leaveSheet: StateFlow<Long?> = _leaveSheet.asStateFlow()

    private val _maybeDismissed = MutableStateFlow(false)
    val maybeDismissed: StateFlow<Boolean> = _maybeDismissed.asStateFlow()

    private var messageJob: Job? = null

    /** Chamado ao abrir/voltar para o app. */
    fun refresh() {
        val before = _permissions.value
        _permissions.value = Permissions.state(app)
        viewModelScope.launch {
            if (before != _permissions.value) c.syncGeofences()
            c.repo.resetRepeats()
            _snapshot.value = c.engine.refresh() ?: _snapshot.value
            _snapshot.value?.fix?.let { fix ->
                launch { c.weather.refreshIfStale(fix.lat, fix.lng) }
                if (c.stores.needsRefresh(fix.lat, fix.lng, c.repo.activeCategories())) c.refreshStores(fix.lat, fix.lng)
            }
        }
    }

    /** Posição em tempo real para os mapas (GPS). Só roda enquanto a tela coleta o Flow. */
    fun liveLocation(): Flow<Fix> = c.location.updates()

    /** Botão "minha localização": lê a posição agora, com GPS, e devolve para centralizar o mapa. */
    fun locateMe(onFix: (Fix?) -> Unit) = viewModelScope.launch {
        val precise = c.location.current(precise = true)
        launch { c.engine.refresh()?.let { _snapshot.value = it } }
        val fix = precise ?: _snapshot.value?.fix
        if (fix == null) show(if (_permissions.value.location) "Não conseguimos ler sua posição agora." else "Permita a localização em Você para ver onde está.")
        onFix(fix)
    }

    fun notNearStore() = c.engine.notNearStore()

    fun show(text: String, undo: (() -> Unit)? = null) {
        val m = UiMessage(text, undo)
        _message.value = m
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            delay(3500)
            if (_message.value?.id == m.id) _message.value = null
        }
    }

    fun clearMessage() { _message.value = null }

    /* ---------- Itens ---------- */

    fun toggleDone(item: ItemEntity) = viewModelScope.launch {
        val done = !item.done
        c.repo.setDone(item.id, done)
        if (done) {
            val left = item.placeId?.let { pid -> items.value.count { it.placeId == pid && it.id != item.id && it.isTask && it.isAlive } } ?: 0
            val here = item.placeId != null && item.placeId == context.value.currentPlaceId
            val text = when {
                here && left > 1 -> "Feito. Faltam $left aqui."
                here && left == 1 -> "Feito. Falta 1 aqui."
                here -> "Tudo feito aqui."
                else -> "Concluída."
            }
            show(text) { viewModelScope.launch { c.repo.setDone(item.id, false) } }
        }
    }

    fun snoozeNextVisit(item: ItemEntity) = viewModelScope.launch {
        c.repo.snoozeToNextVisit(item.id)
        show("Volta na próxima vez que você vier aqui.")
    }

    fun archive(item: ItemEntity) = viewModelScope.launch {
        c.repo.archive(item.id)
        show("Arquivado.") { viewModelScope.launch { c.repo.saveItem(item) } }
    }

    fun moveTo(item: ItemEntity, place: PlaceEntity) = viewModelScope.launch {
        c.repo.moveTo(item.id, place.id)
        show("Movido para ${place.name}.")
    }

    fun saveItem(item: ItemEntity, onSaved: () -> Unit) = viewModelScope.launch {
        c.repo.saveItem(item)
        val where = item.placeId?.let { id -> places.value.firstOrNull { it.id == id }?.name }
        val kind = if (item.kind == ItemKind.TASK) "Tarefa salva" else "Memória salva"
        show(if (where != null) "$kind em $where." else "$kind.")
        onSaved()
    }

    fun saveMemoryValue(item: ItemEntity, value: String, archive: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        c.repo.saveMemoryValue(item.id, value, archive)
        show(
            when {
                archive && value.isNotBlank() -> "Valor salvo: $value. Memória arquivada."
                archive -> "Memória arquivada."
                value.isNotBlank() -> "Valor salvo. A memória continua aqui."
                else -> "A memória continua aqui."
            }
        )
        onDone()
    }

    /** Captura rápida do botão +: vira memória do lugar atual. */
    fun quickCapture(text: String) = viewModelScope.launch {
        val placeId = context.value.currentPlaceId ?: return@launch
        c.repo.saveItem(ItemEntity(kind = ItemKind.MEMORY, title = text.trim(), placeId = placeId, remind = RemindWhen.ARRIVE, memoryShow = MemoryShow.ALWAYS))
        show("Memória salva aqui.")
    }

    /* ---------- Lugares ---------- */

    fun savePlace(place: PlaceEntity, onSaved: (Long) -> Unit) = viewModelScope.launch {
        val id = c.repo.savePlace(place)
        show(if (place.id == 0L) "Lugar salvo. Adicione o que lembrar lá." else "Lugar atualizado.")
        // Se o lugar novo é onde o usuário está, a tela Agora já passa a mostrá-lo.
        _snapshot.value = c.engine.refresh() ?: _snapshot.value
        onSaved(id)
    }

    /** Onboarding: salva a posição atual como um lugar ("Casa"). */
    fun saveCurrentLocationAs(name: String, icon: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val fix = c.location.current()
        if (fix == null) {
            show("Não conseguimos ler sua posição agora. Você pode salvar depois, em Lugares.")
            onDone(false)
            return@launch
        }
        c.repo.savePlace(PlaceEntity(name = name, icon = icon, lat = fix.lat, lng = fix.lng))
        _snapshot.value = c.engine.refresh() ?: _snapshot.value
        onDone(true)
    }

    fun deletePlace(place: PlaceEntity, onDone: () -> Unit) = viewModelScope.launch {
        c.repo.deletePlace(place)
        show("${place.name} apagado. Os itens de lá ficaram sem lugar.")
        onDone()
    }

    fun togglePlaceNotice(place: PlaceEntity, arrive: Boolean) = viewModelScope.launch {
        c.repo.savePlace(if (arrive) place.copy(notifyArrive = !place.notifyArrive) else place.copy(notifyLeave = !place.notifyLeave))
    }

    /* ---------- Contexto ---------- */

    fun setHere(placeId: Long) = viewModelScope.launch {
        c.engine.setHereManually(placeId)
        _maybeDismissed.value = false
    }

    fun notHere() = viewModelScope.launch { c.engine.notHere() }
    fun dismissMaybe() { _maybeDismissed.value = true }

    fun openLeaveSheet(placeId: Long) { _leaveSheet.value = placeId }
    fun closeLeaveSheet() { _leaveSheet.value = null }

    fun leaveNextTime(placeId: Long) = viewModelScope.launch {
        c.repo.snoozePlaceTasks(placeId)
        c.notifier.cancelLeave(placeId)
        _leaveSheet.value = null
        show("Fica para a próxima visita.")
    }

    fun leaveAllDone(placeId: Long) = viewModelScope.launch {
        c.repo.completePlaceTasks(placeId)
        c.notifier.cancelLeave(placeId)
        _leaveSheet.value = null
        show("Tudo feito.")
    }

    fun stillHere(placeId: Long) = viewModelScope.launch {
        c.engine.stillHere(placeId)
        _leaveSheet.value = null
    }

    /* ---------- Ajustes ---------- */

    fun setDwell(min: Int) = viewModelScope.launch {
        c.prefs.setDwellMinutes(min)
        c.syncGeofences()
    }

    fun setQuietHours(on: Boolean) = c.prefs.setQuietHours(on)

    fun finishOnboarding() = viewModelScope.launch {
        c.prefs.setOnboardingDone()
        refresh()
    }

    fun deleteEverything() = viewModelScope.launch {
        c.repo.deleteEverything()
        _snapshot.value = null
    }

    fun permissionsChanged() = refresh()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RoteiroApp
                AppViewModel(app, app.container)
            }
        }
    }
}
