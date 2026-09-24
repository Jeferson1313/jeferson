package com.roteiro.core

import java.time.Instant
import java.time.ZoneId

/**
 * Regras do produto, sem dependência de Android.
 * Tudo aqui é testado em core/src/test.
 */
object Rules {

    /** Item ainda precisa de atenção (não feito, não arquivado, não adiado). */
    fun isAlive(item: ItemInfo) = !item.done && !item.archived && !item.snoozed

    fun pendingAt(items: List<ItemInfo>, placeId: Long): List<ItemInfo> =
        items.filter { it.placeId == placeId && isAlive(it) && it.remind != RemindWhen.TIME }

    /** Tarefas que justificam o aviso "Você está saindo…". */
    fun pendingTasksOnLeave(items: List<ItemInfo>, placeId: Long): List<ItemInfo> =
        pendingAt(items, placeId).filter { it.kind == ItemKind.TASK && it.remind != RemindWhen.NONE }

    /**
     * Aviso de chegada: uma notificação por chegada, com o total de itens.
     * Não avisa se o lugar tem o aviso desligado ou se não há nada pendente.
     */
    fun arrivalNotice(place: PlaceInfo, items: List<ItemInfo>): Notice? {
        if (!place.notifyArrive) return null
        val pending = pendingAt(items, place.id).filter { it.remind != RemindWhen.NONE && it.remind != RemindWhen.LEAVE }
        if (pending.isEmpty()) return null
        val title = "Você chegou ${Words.ao(place.name)}."
        val body = if (pending.size == 1) "${pending[0].title}."
        else "${pending.size} coisas estão esperando por você aqui."
        return Notice(title, body, pending.take(MAX_LINES).map { it.title })
    }

    /** Aviso de saída: só quando ficou alguma tarefa para trás. */
    fun leaveNotice(place: PlaceInfo, items: List<ItemInfo>): Notice? {
        if (!place.notifyLeave) return null
        val pending = pendingTasksOnLeave(items, place.id)
        if (pending.isEmpty()) return null
        val title = "Você está saindo ${Words.de(place.name)}."
        val body = if (pending.size == 1) "Você ainda precisa: ${Words.lowerFirst(pending[0].title)}."
        else "Ainda faltam ${pending.size} coisas: " +
            pending.take(2).joinToString(", ") { Words.lowerFirst(it.title) } +
            if (pending.size > 2) "…" else "."
        return Notice(title, body, pending.take(MAX_LINES).map { it.title })
    }

    /**
     * Uma tarefa repetida que foi concluída volta a ficar pendente quando o período vira.
     * Diária: no dia seguinte. Semanal: 7 dias depois. Mensal: no mesmo dia do mês seguinte.
     */
    fun shouldReset(item: ItemInfo, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!item.done || item.repeat == Repeat.ONCE || item.archived) return false
        val doneAt = item.doneAt ?: return true
        val doneDay = Instant.ofEpochMilli(doneAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val next = when (item.repeat) {
            Repeat.DAILY -> doneDay.plusDays(1)
            Repeat.WEEKLY -> doneDay.plusWeeks(1)
            Repeat.MONTHLY -> doneDay.plusMonths(1)
            Repeat.ONCE -> return false
        }
        return !today.isBefore(next)
    }

    /**
     * Próximo horário (epoch ms) para um lembrete por horário, a partir de [now].
     * Retorna null para lembretes de uma vez cujo horário de hoje já passou e que já foram avisados.
     */
    fun nextTimeTrigger(
        timeOfDayMin: Int,
        repeat: Repeat,
        now: Long,
        alreadyFiredToday: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val nowDt = Instant.ofEpochMilli(now).atZone(zone)
        var candidate = nowDt.toLocalDate().atStartOfDay(zone).plusMinutes(timeOfDayMin.toLong())
        if (!candidate.isAfter(nowDt) || alreadyFiredToday) {
            if (repeat == Repeat.ONCE && alreadyFiredToday) return null
            candidate = when (repeat) {
                Repeat.ONCE, Repeat.DAILY -> candidate.plusDays(1)
                Repeat.WEEKLY -> candidate.plusWeeks(1)
                Repeat.MONTHLY -> candidate.plusMonths(1)
            }
        }
        return candidate.toInstant().toEpochMilli()
    }

    /**
     * Aviso ao passar perto de um estabelecimento de um tipo ("qualquer mercado").
     * [items] são os itens pendentes presos a esse tipo.
     */
    fun categoryNotice(category: Category, storeName: String?, items: List<ItemInfo>, distanceM: Int? = null): Notice? {
        val pending = items.filter { isAlive(it) && it.remind != RemindWhen.TIME && it.remind != RemindWhen.NONE }
        if (pending.isEmpty()) return null
        val title = "Tem ${category.nearby} aqui perto."
        val where = listOfNotNull(storeName, distanceM?.let { "a $it m" }).joinToString(", ")
        val what = if (pending.size == 1) Words.lowerFirst(pending[0].title)
            else pending.take(2).joinToString(", ") { Words.lowerFirst(it.title) } + if (pending.size > 2) "…" else ""
        val body = (if (where.isNotEmpty()) "$where: " else "") + what + if (what.endsWith("…")) "" else "."
        return Notice(title, body.replaceFirstChar { it.uppercase() }, pending.take(MAX_LINES).map { it.title })
    }

    const val MAX_LINES = 3
}
