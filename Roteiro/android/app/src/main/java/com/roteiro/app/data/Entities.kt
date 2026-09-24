package com.roteiro.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.roteiro.core.ItemInfo
import com.roteiro.core.ItemKind
import com.roteiro.core.MemoryShow
import com.roteiro.core.PlaceInfo
import com.roteiro.core.RemindWhen
import com.roteiro.core.Repeat

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Chave do ícone em [com.roteiro.app.ui.components.PlaceIcons]. */
    val icon: String = "place",
    val lat: Double,
    val lng: Double,
    val radiusM: Int = DEFAULT_RADIUS_M,
    val address: String? = null,
    val notifyArrive: Boolean = true,
    val notifyLeave: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toInfo() = PlaceInfo(id, name, lat, lng, radiusM, notifyArrive, notifyLeave)

    companion object {
        const val DEFAULT_RADIUS_M = 100
    }
}

@Entity(tableName = "items", indices = [Index("placeId")])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ItemKind,
    val title: String,
    val placeId: Long? = null,
    val remind: RemindWhen = RemindWhen.ARRIVE,
    val repeat: Repeat = Repeat.ONCE,
    /** Minutos desde 00:00, para lembretes por horário. */
    val timeOfDayMin: Int? = null,
    val note: String? = null,
    /** Memória com "campo para anotar lá": rótulo e último valor anotado. */
    val fieldLabel: String? = null,
    val fieldValue: String? = null,
    val memoryShow: MemoryShow = MemoryShow.ALWAYS,
    val done: Boolean = false,
    val doneAt: Long? = null,
    /** Adiado para a próxima visita ao lugar. */
    val snoozed: Boolean = false,
    val archived: Boolean = false,
    /** Último disparo do lembrete por horário (epoch ms). */
    val lastFiredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isTask get() = kind == ItemKind.TASK
    val isAlive get() = !done && !archived && !snoozed

    fun toInfo() = ItemInfo(id, kind, title, placeId, remind, repeat, done, doneAt, snoozed, archived)
}
