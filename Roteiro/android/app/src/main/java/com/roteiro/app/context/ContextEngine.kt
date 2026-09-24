package com.roteiro.app.context

import com.roteiro.app.data.ItemEntity
import com.roteiro.app.data.PlaceEntity
import com.roteiro.app.data.Prefs
import com.roteiro.app.data.Repository
import com.roteiro.core.Detection
import com.roteiro.core.Geo
import com.roteiro.core.ItemKind
import com.roteiro.core.MemoryShow
import com.roteiro.core.Rules

/** Resultado da leitura de posição feita quando o app abre. */
data class LocationSnapshot(val fix: Fix, val detection: Detection)

/**
 * Decide o que acontece quando o usuário chega ou sai de um lugar.
 * Eventos do sistema (cercas virtuais) geram notificações; correções feitas
 * com o app aberto ([fromSystem] = false) só atualizam o contexto.
 */
class ContextEngine(
    private val repo: Repository,
    private val prefs: Prefs,
    private val notifier: Notifier,
    private val location: LocationSource,
    private val stores: StoreFinder,
) {
    /* ---------- Estabelecimentos ("qualquer mercado") ---------- */

    suspend fun onStoreDwell(storeId: String, notify: Boolean = true) {
        val store = stores.find(storeId) ?: return
        if (prefs.context.value.storeId == storeId) return
        val place = repo.categoryPlace(store.category) ?: return
        prefs.nearStore(storeId, store.name, store.category.key)
        repo.onVisit(place.id)
        if (!notify || !place.notifyArrive) return
        val items = repo.allItems().filter { it.placeId == place.id }.map(ItemEntity::toInfo)
        Rules.categoryNotice(store.category, store.name, items)?.let { notifier.showArrival(place, it) }
    }

    suspend fun onStoreExit(storeId: String) {
        if (prefs.context.value.storeId != storeId) return
        val store = stores.find(storeId)
        prefs.clearStore()
        store?.let { repo.categoryPlace(it.category) }?.let { notifier.cancelArrival(it.id) }
    }

    suspend fun onArrive(placeId: Long, fromSystem: Boolean) {
        val ctx = prefs.context.value
        if (ctx.currentPlaceId == placeId) return
        val place = repo.place(placeId) ?: return
        // Saída perdida (sem sinal, aparelho desligado): encerra a visita anterior sem avisar.
        ctx.currentPlaceId?.takeIf { it != placeId }?.let { prefs.leave(it) }

        prefs.arrive(placeId)
        repo.onVisit(placeId)
        repo.resetRepeats()
        notifier.cancelLeave(placeId)

        if (fromSystem) {
            val items = repo.allItems().map(ItemEntity::toInfo)
            Rules.arrivalNotice(place.toInfo(), items)?.let { notifier.showArrival(place, it) }
        }
    }

    suspend fun onLeave(placeId: Long, fromSystem: Boolean) {
        val ctx = prefs.context.value
        // Saída sem chegada confirmada = só passou perto. Nada a fazer.
        if (ctx.currentPlaceId != placeId) return
        val place = repo.place(placeId)
        prefs.leave(placeId)
        notifier.cancelArrival(placeId)
        archiveSeenOneTimeMemories(placeId)
        if (fromSystem && place != null) {
            val all = repo.allItems()
            val notice = Rules.leaveNotice(place.toInfo(), all.map(ItemEntity::toInfo)) ?: return
            val pendingIds = Rules.pendingTasksOnLeave(all.map(ItemEntity::toInfo), placeId).map { it.id }.toSet()
            notifier.showLeave(place, notice, all.filter { it.id in pendingIds })
        }
    }

    /** Memórias "só na próxima vez" já foram vistas nesta visita. */
    private suspend fun archiveSeenOneTimeMemories(placeId: Long) {
        repo.allItems()
            .filter { it.placeId == placeId && it.kind == ItemKind.MEMORY && it.memoryShow == MemoryShow.NEXT_VISIT && it.isAlive }
            .forEach { repo.archive(it.id) }
    }

    /**
     * Lê a posição atual (ao abrir o app) e corrige o contexto sem notificar.
     * Retorna a leitura para a tela mostrar distâncias e o estado "Talvez você esteja em…".
     */
    suspend fun refresh(): LocationSnapshot? {
        val fix = location.current() ?: return null
        val places = repo.allPlaces().filter { it.isGeo }
        val detection = Geo.detect(fix.lat, fix.lng, fix.accuracyM, places.map(PlaceEntity::toInfo))
        val current = prefs.context.value.currentPlaceId
        when (detection) {
            is Detection.Inside -> if (current != detection.place.id) onArrive(detection.place.id, fromSystem = false)
            Detection.Outside -> current?.let { id ->
                val p = places.firstOrNull { it.id == id }
                val far = p == null || Geo.distanceM(fix.lat, fix.lng, p.lat, p.lng) > p.radiusM + fix.accuracyM.coerceAtLeast(100f)
                if (far && fix.accuracyM <= Geo.GOOD_ACCURACY_M * 2) onLeave(id, fromSystem = false)
            }
            is Detection.Maybe -> Unit
        }
        refreshStoreContext(fix)
        return LocationSnapshot(fix, detection)
    }

    /** Com o app aberto: está perto de algum estabelecimento com pendência? (sem notificar) */
    private suspend fun refreshStoreContext(fix: Fix) {
        val active = repo.activeCategories()
        val near = stores.nearest(fix.lat, fix.lng, active, 1).firstOrNull()
            ?.takeIf { Geo.distanceM(fix.lat, fix.lng, it.lat, it.lng) <= StoreFinder.STORE_RADIUS_M + fix.accuracyM.coerceAtMost(60f) }
        val ctx = prefs.context.value
        when {
            near != null && ctx.storeId != near.id -> onStoreDwell(near.id, notify = false)
            near == null && ctx.storeId != null -> prefs.clearStore()
        }
    }

    fun notNearStore() = prefs.clearStore()

    /** O usuário escolheu manualmente onde está (ou confirmou o "Talvez"). */
    suspend fun setHereManually(placeId: Long) = onArrive(placeId, fromSystem = false)

    /** "Não é aqui?" */
    suspend fun notHere() {
        prefs.context.value.currentPlaceId?.let { prefs.leave(it) }
    }

    /** "Ainda estou aqui" (depois de um aviso de saída errado). */
    suspend fun stillHere(placeId: Long) {
        prefs.arrive(placeId)
        notifier.cancelLeave(placeId)
    }
}
