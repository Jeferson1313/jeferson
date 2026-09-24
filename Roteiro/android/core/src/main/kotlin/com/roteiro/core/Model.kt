package com.roteiro.core

/** Tarefa (algo para fazer) ou memória (algo para ver/anotar no lugar). */
enum class ItemKind { TASK, MEMORY }

/** Quando o app deve lembrar de um item. */
enum class RemindWhen { ARRIVE, LEAVE, TIME, NONE }

enum class Repeat { ONCE, DAILY, WEEKLY, MONTHLY }

/** Memória aparece sempre que o usuário estiver no lugar, ou só na próxima visita. */
enum class MemoryShow { ALWAYS, NEXT_VISIT }

/** Visão mínima de um lugar, usada pelas regras (independente do banco). */
data class PlaceInfo(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val notifyArrive: Boolean = true,
    val notifyLeave: Boolean = true,
)

/** Visão mínima de um item, usada pelas regras (independente do banco). */
data class ItemInfo(
    val id: Long,
    val kind: ItemKind,
    val title: String,
    val placeId: Long?,
    val remind: RemindWhen = RemindWhen.ARRIVE,
    val repeat: Repeat = Repeat.ONCE,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val snoozed: Boolean = false,
    val archived: Boolean = false,
)

/** Texto pronto de uma notificação. */
data class Notice(
    val title: String,
    val body: String,
    val lines: List<String>,
)
