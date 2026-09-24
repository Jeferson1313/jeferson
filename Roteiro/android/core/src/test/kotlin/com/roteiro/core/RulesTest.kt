package com.roteiro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RulesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private val mercado = PlaceInfo(1, "Supermercado", -23.55, -46.65, 100)
    private val casa = PlaceInfo(2, "Casa", -23.56, -46.66, 100)

    private fun task(id: Long, title: String, place: Long? = 1, remind: RemindWhen = RemindWhen.ARRIVE) =
        ItemInfo(id, ItemKind.TASK, title, place, remind)

    @Test fun arrivalGroupsAllItemsInOneNotice() {
        val items = listOf(task(1, "Comprar café"), task(2, "Comprar sabão"), ItemInfo(3, ItemKind.MEMORY, "Ver preço do arroz", 1))
        val n = Rules.arrivalNotice(mercado, items)!!
        assertEquals("Você chegou ao Supermercado.", n.title)
        assertEquals("3 coisas estão esperando por você aqui.", n.body)
        assertEquals(3, n.lines.size)
    }

    @Test fun arrivalWithSingleItemNamesIt() {
        val n = Rules.arrivalNotice(casa, listOf(task(1, "Alimentar o Thor", 2)))!!
        assertEquals("Você chegou em casa.", n.title)
        assertEquals("Alimentar o Thor.", n.body)
    }

    @Test fun arrivalIsSilentWhenNothingPendingOrDisabled() {
        assertNull(Rules.arrivalNotice(mercado, listOf(task(1, "Feito", 1).copy(done = true))))
        assertNull(Rules.arrivalNotice(mercado, listOf(task(1, "Adiado", 1).copy(snoozed = true))))
        assertNull(Rules.arrivalNotice(mercado.copy(notifyArrive = false), listOf(task(1, "Café"))))
        assertNull(Rules.arrivalNotice(mercado, listOf(task(1, "Sem aviso", 1, RemindWhen.NONE))))
    }

    @Test fun leaveOnlyWarnsAboutTasks() {
        val items = listOf(ItemInfo(3, ItemKind.MEMORY, "Ver preço do arroz", 1), task(1, "Pegar documento no RH", 1, RemindWhen.LEAVE))
        val n = Rules.leaveNotice(mercado, items)!!
        assertEquals("Você está saindo do Supermercado.", n.title)
        assertEquals("Você ainda precisa: pegar documento no RH.", n.body)
        assertNull(Rules.leaveNotice(mercado, items.filter { it.kind == ItemKind.MEMORY }))
    }

    @Test fun leaveWithManyTasksListsFirstTwo() {
        val items = (1L..3L).map { task(it, "Tarefa $it") }
        assertEquals("Ainda faltam 3 coisas: tarefa 1, tarefa 2…", Rules.leaveNotice(mercado, items)!!.body)
    }

    @Test fun repeatResetsWhenPeriodTurns() {
        val daily = task(1, "Tomar remédio").copy(repeat = Repeat.DAILY, done = true, doneAt = at(2026, 9, 24, 22))
        assertFalse(Rules.shouldReset(daily, at(2026, 9, 24, 23), zone))
        assertTrue(Rules.shouldReset(daily, at(2026, 9, 25, 7), zone))

        val weekly = daily.copy(repeat = Repeat.WEEKLY)
        assertFalse(Rules.shouldReset(weekly, at(2026, 9, 30), zone))
        assertTrue(Rules.shouldReset(weekly, at(2026, 10, 1), zone))

        val once = daily.copy(repeat = Repeat.ONCE)
        assertFalse(Rules.shouldReset(once, at(2027, 1, 1), zone))
    }

    @Test fun nextTimeTrigger() {
        val now = at(2026, 9, 24, 18, 0)
        assertEquals(at(2026, 9, 24, 20, 0), Rules.nextTimeTrigger(20 * 60, Repeat.ONCE, now, false, zone))
        assertEquals(at(2026, 9, 25, 8, 0), Rules.nextTimeTrigger(8 * 60, Repeat.DAILY, now, false, zone))
        assertEquals(at(2026, 10, 1, 8, 0), Rules.nextTimeTrigger(8 * 60, Repeat.WEEKLY, at(2026, 9, 24, 8, 0), true, zone))
        assertNull(Rules.nextTimeTrigger(8 * 60, Repeat.ONCE, now, true, zone))
    }

    @Test fun detectionUsesAccuracy() {
        val here = Geo.detect(-23.55, -46.65, 20f, listOf(mercado, casa))
        assertTrue(here is Detection.Inside && here.place.id == 1L)
        // 160 m do mercado com precisão ruim: "talvez"
        val maybe = Geo.detect(-23.5514, -46.65, 140f, listOf(mercado))
        assertTrue(maybe is Detection.Maybe)
        assertEquals(Detection.Outside, Geo.detect(-23.60, -46.70, 10f, listOf(mercado)))
        assertEquals(Detection.Outside, Geo.detect(0.0, 0.0, 10f, emptyList()))
    }

    @Test fun distanceAndFormatting() {
        val d = Geo.distanceM(-23.5505, -46.6333, -23.5614, -46.6559)
        assertTrue(d in 2500.0..2700.0)
        assertEquals("350 m", Geo.formatDistance(347.0))
        assertEquals("1,2 km", Geo.formatDistance(1234.0))
        assertEquals("12 km", Geo.formatDistance(12_400.0))
    }

    @Test fun portuguesePrepositions() {
        assertEquals("em casa", Words.em("Casa"))
        assertEquals("no Trabalho", Words.em("Trabalho"))
        assertEquals("na Farmácia", Words.em("Farmácia"))
        assertEquals("da Academia", Words.de("Academia"))
        assertEquals("à Oficina do Marcos", Words.ao("Oficina do Marcos"))
        assertEquals("pegar documento no RH", Words.lowerFirst("Pegar documento no RH"))
        assertEquals("RH: entregar", Words.lowerFirst("RH: entregar"))
    }

    @Test fun summaries() {
        assertEquals("Vamos lembrar você ao chegar no Supermercado, toda semana.",
            Words.taskSummary("Supermercado", RemindWhen.ARRIVE, Repeat.WEEKLY, null))
        assertEquals("Vamos lembrar você às 08:05.", Words.taskSummary(null, RemindWhen.TIME, Repeat.ONCE, 8 * 60 + 5))
        assertNotNull(Words.memorySummary("Casa", MemoryShow.ALWAYS))
    }
}

class CategoryTest {
    private fun task(id: Long, title: String) = ItemInfo(id, ItemKind.TASK, title, 99)

    @Test fun marketNoticeNamesStoreAndItem() {
        val n = Rules.categoryNotice(Category.MARKET, "Mercado Dia", listOf(task(1, "Comprar pão")), 80)!!
        org.junit.Assert.assertEquals("Tem um mercado aqui perto.", n.title)
        org.junit.Assert.assertEquals("Mercado Dia, a 80 m: comprar pão.", n.body)
    }

    @Test fun noticeWithoutStoreNameAndMany() {
        val n = Rules.categoryNotice(Category.PHARMACY, null, listOf(task(1, "Protetor solar"), task(2, "Dipirona"), task(3, "Curativo")))!!
        org.junit.Assert.assertEquals("Tem uma farmácia aqui perto.", n.title)
        org.junit.Assert.assertEquals("Protetor solar, dipirona…", n.body)
    }

    @Test fun silentWhenNothingPending() {
        org.junit.Assert.assertNull(Rules.categoryNotice(Category.MARKET, "X", listOf(task(1, "a").copy(done = true))))
    }

    @Test fun summariesAndGreeting() {
        org.junit.Assert.assertEquals("Vamos lembrar você ao passar perto de qualquer mercado.",
            Words.taskSummary("Qualquer mercado", RemindWhen.ARRIVE, Repeat.ONCE, null, Category.MARKET))
        org.junit.Assert.assertEquals("em qualquer mercado", Words.em("Qualquer mercado"))
        org.junit.Assert.assertEquals("perto de qualquer farmácia", Words.ao("Qualquer farmácia"))
        org.junit.Assert.assertEquals("Bom dia", Words.greeting(7))
        org.junit.Assert.assertEquals("Boa tarde", Words.greeting(14))
        org.junit.Assert.assertEquals("Boa noite", Words.greeting(22))
        org.junit.Assert.assertEquals("Boa noite", Words.greeting(3))
    }
}

class CategoryTagsTest {
    @Test fun marketIsBroad() {
        listOf("supermarket", "convenience", "grocery", "butcher", "greengrocer", "bakery").forEach {
            org.junit.Assert.assertTrue(it, Category.MARKET.matches(mapOf("shop" to it)))
        }
        org.junit.Assert.assertFalse(Category.MARKET.matches(mapOf("amenity" to "pharmacy")))
        org.junit.Assert.assertTrue(Category.BAKERY.matches(mapOf("shop" to "bakery", "name" to "Padaria Pão Quente")))
        org.junit.Assert.assertTrue(Category.PHARMACY.matches(mapOf("amenity" to "pharmacy")))
    }
}
