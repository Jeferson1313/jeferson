package com.roteiro.app.data

import com.roteiro.core.ItemKind
import com.roteiro.core.RemindWhen
import com.roteiro.core.Rules
import kotlinx.coroutines.flow.Flow

/**
 * Fonte única de dados do app. Toda mudança que afeta cercas virtuais ou
 * lembretes por horário chama [onPlacesChanged] / [onItemsChanged].
 */
class Repository(
    private val db: AppDatabase,
    val prefs: Prefs,
    private val onPlacesChanged: suspend () -> Unit,
    private val onItemsChanged: suspend () -> Unit,
) {
    val places: Flow<List<PlaceEntity>> = db.places().observeAll()
    val items: Flow<List<ItemEntity>> = db.items().observeAll()

    suspend fun allPlaces() = db.places().getAll()
    suspend fun allItems() = db.items().getAll()
    suspend fun place(id: Long) = db.places().get(id)
    suspend fun item(id: Long) = db.items().get(id)

    /* ---------- Lugares ---------- */

    suspend fun savePlace(place: PlaceEntity): Long {
        val id = if (place.id == 0L) db.places().insert(place) else place.id.also { db.places().update(place) }
        onPlacesChanged()
        return id
    }

    suspend fun deletePlace(place: PlaceEntity) {
        db.items().detachFromPlace(place.id)
        db.places().delete(place)
        val ctx = prefs.context.value
        if (ctx.currentPlaceId == place.id) prefs.clearCurrent()
        onPlacesChanged()
        onItemsChanged()
    }

    /* ---------- Itens ---------- */

    suspend fun saveItem(item: ItemEntity): Long {
        val id = if (item.id == 0L) db.items().insert(item) else item.id.also { db.items().update(item) }
        onItemsChanged()
        return id
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val it = db.items().get(id) ?: return
        db.items().update(it.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null, snoozed = false))
        onItemsChanged()
    }

    /** "Na próxima vez que eu vier aqui". */
    suspend fun snoozeToNextVisit(id: Long) {
        val it = db.items().get(id) ?: return
        db.items().update(it.copy(snoozed = true))
    }

    /** Adia todas as tarefas pendentes de um lugar (botão "Próxima vez" do aviso de saída). */
    suspend fun snoozePlaceTasks(placeId: Long) {
        val pending = db.items().getByPlace(placeId).filter { it.kind == ItemKind.TASK && it.isAlive }
        db.items().updateAll(pending.map { it.copy(snoozed = true) })
    }

    suspend fun completePlaceTasks(placeId: Long) {
        val now = System.currentTimeMillis()
        val pending = db.items().getByPlace(placeId).filter { it.kind == ItemKind.TASK && it.isAlive }
        db.items().updateAll(pending.map { it.copy(done = true, doneAt = now) })
        onItemsChanged()
    }

    suspend fun archive(id: Long) {
        val it = db.items().get(id) ?: return
        db.items().update(it.copy(archived = true))
        onItemsChanged()
    }

    suspend fun moveTo(id: Long, placeId: Long) {
        val it = db.items().get(id) ?: return
        val remind = if (it.remind == RemindWhen.NONE || it.remind == RemindWhen.TIME) it.remind else RemindWhen.ARRIVE
        db.items().update(it.copy(placeId = placeId, remind = remind, snoozed = false))
    }

    /** Memória: guarda o valor anotado no local e, opcionalmente, arquiva. */
    suspend fun saveMemoryValue(id: Long, value: String?, archive: Boolean) {
        val it = db.items().get(id) ?: return
        db.items().update(
            it.copy(
                fieldValue = value?.takeIf { v -> v.isNotBlank() } ?: it.fieldValue,
                done = archive || it.done,
                doneAt = if (archive) System.currentTimeMillis() else it.doneAt,
                archived = archive || it.archived,
            )
        )
    }

    /* ---------- Visitas ---------- */

    /** Ao chegar: itens adiados voltam; memórias "só na próxima vez" deixam de ser adiadas. */
    suspend fun onVisit(placeId: Long) {
        val items = db.items().getByPlace(placeId)
        db.items().updateAll(items.filter { it.snoozed }.map { it.copy(snoozed = false) })
    }

    /** Tarefas repetidas voltam a ficar pendentes quando o período vira. */
    suspend fun resetRepeats(now: Long = System.currentTimeMillis()) {
        val toReset = db.items().getAll().filter { Rules.shouldReset(it.toInfo(), now) }
        if (toReset.isNotEmpty()) {
            db.items().updateAll(toReset.map { it.copy(done = false, doneAt = null, archived = false) })
            onItemsChanged()
        }
    }

    suspend fun markFired(id: Long, at: Long) {
        val it = db.items().get(id) ?: return
        db.items().update(it.copy(lastFiredAt = at))
    }

    suspend fun deleteEverything() {
        db.items().deleteAll()
        db.places().deleteAll()
        prefs.clearAll()
        onPlacesChanged()
        onItemsChanged()
    }
}
