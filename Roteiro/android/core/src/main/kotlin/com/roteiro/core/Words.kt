package com.roteiro.core

import java.util.Locale

/**
 * Preposições em português para nomes de lugar escolhidos pelo usuário.
 * Sem gênero gramatical conhecido, usamos uma regra simples pela primeira palavra:
 * termina em "a" → feminino ("na Farmácia"), senão masculino ("no Trabalho").
 * "Casa" sozinha é tratada como em "em casa".
 */
object Words {
    private val PT = Locale.forLanguageTag("pt-BR")

    private fun isHome(name: String) = name.trim().equals("casa", ignoreCase = true)

    private fun feminine(name: String): Boolean {
        val first = name.trim().split(' ').firstOrNull()?.lowercase(PT) ?: return false
        return first.endsWith("a") || first.endsWith("ã") || first.endsWith("ção") || first.endsWith("dade")
    }

    private fun isAny(name: String) = name.trim().lowercase(PT).startsWith("qualquer ")
    private fun anyLower(name: String) = name.trim().lowercase(PT)

    /** "em casa", "no Trabalho", "na Farmácia", "em qualquer mercado". */
    fun em(name: String): String = when {
        isAny(name) -> "em ${anyLower(name)}"
        isHome(name) -> "em casa"
        feminine(name) -> "na $name"
        else -> "no $name"
    }

    /** "de casa", "do Trabalho", "da Farmácia". */
    fun de(name: String): String = when {
        isAny(name) -> "de ${anyLower(name)}"
        isHome(name) -> "de casa"
        feminine(name) -> "da $name"
        else -> "do $name"
    }

    /** "em casa", "ao Trabalho", "à Farmácia" (depois de "chegou"). */
    fun ao(name: String): String = when {
        isAny(name) -> "perto de ${anyLower(name)}"
        isHome(name) -> "em casa"
        feminine(name) -> "à $name"
        else -> "ao $name"
    }

    /** Primeira letra minúscula, para usar no meio da frase. */
    fun lowerFirst(text: String): String =
        if (text.length > 1 && text[1].isUpperCase()) text // siglas como "RH"
        else text.replaceFirstChar { it.lowercase(PT) }

    fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

    fun time(minutesOfDay: Int): String = String.format(PT, "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)

    /** Saudação pela hora do dia. */
    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    /** Frase-resumo mostrada antes de salvar uma tarefa. [category] = lugar do tipo "qualquer mercado". */
    fun taskSummary(placeName: String?, remind: RemindWhen, repeat: Repeat, timeOfDayMin: Int?, category: Category? = null): String {
        if (category != null && remind == RemindWhen.ARRIVE) {
            val rep = when (repeat) { Repeat.ONCE -> ""; Repeat.DAILY -> ", todo dia"; Repeat.WEEKLY -> ", toda semana"; Repeat.MONTHLY -> ", todo mês" }
            return "Vamos lembrar você ao passar perto de ${category.any.lowercase(PT)}$rep."
        }
        val rep = when (repeat) {
            Repeat.ONCE -> ""
            Repeat.DAILY -> ", todo dia"
            Repeat.WEEKLY -> ", toda semana"
            Repeat.MONTHLY -> ", todo mês"
        }
        return when (remind) {
            RemindWhen.ARRIVE -> if (placeName != null) "Vamos lembrar você ao chegar ${em(placeName)}$rep." else "Escolha um lugar para lembrar ao chegar."
            RemindWhen.LEAVE -> if (placeName != null) "Vamos lembrar você ao sair ${de(placeName)}$rep." else "Escolha um lugar para lembrar ao sair."
            RemindWhen.TIME -> "Vamos lembrar você às ${time(timeOfDayMin ?: 0)}$rep."
            RemindWhen.NONE -> if (placeName != null) "Fica ${em(placeName)}, sem aviso$rep." else "Fica na sua lista, sem aviso$rep."
        }
    }

    /** Frase-resumo mostrada antes de salvar uma memória. */
    fun memorySummary(placeName: String?, show: MemoryShow, category: Category? = null): String = when {
        category != null -> "Aparece quando você passar perto de ${category.any.lowercase(PT)}."
        placeName == null -> "Escolha o lugar onde esta memória deve aparecer."
        show == MemoryShow.ALWAYS -> "Aparece na tela Agora sempre que você estiver ${em(placeName)}. Não tem prazo."
        else -> "Aparece na próxima vez que você estiver ${em(placeName)}."
    }
}
