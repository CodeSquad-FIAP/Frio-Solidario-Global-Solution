package br.com.fiap.friosolidario.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val HORA = DateTimeFormatter.ofPattern("HH:mm", PT_BR)
private val DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm", PT_BR)
private val DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)

// O espaço entre o número e a unidade é inseparável (\u00A0): em tela estreita ou com fonte
// grande a quebra de linha nunca deixa "°C" ou "kg" sozinho na linha de baixo.
fun temperatura(valor: Double): String = String.format(PT_BR, "%.1f\u00A0°C", valor)

fun umidade(valor: Double): String = String.format(PT_BR, "%.0f%%", valor)

fun quilos(valor: Double): String = String.format(PT_BR, "%.1f\u00A0kg", valor)

/** A API manda UTC; a tela mostra no fuso do aparelho. */
fun hora(instante: Instant, fuso: ZoneId = ZoneId.systemDefault()): String = HORA.format(instante.atZone(fuso))

fun diaEHora(instante: Instant, fuso: ZoneId = ZoneId.systemDefault()): String =
    DIA_HORA.format(instante.atZone(fuso))

fun dia(data: LocalDate): String = DIA.format(data)

/** "há 4 min": quanto tempo faz do dado, e não a hora em que a tela abriu. */
fun tempoDecorrido(de: Instant, agora: Instant = Instant.now()): String {
    val minutos = Duration.between(de, agora).toMinutes()
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        minutos < 60 * 24 -> "há ${minutos / 60} h"
        minutos < 60 * 48 -> "há 1 dia"
        else -> "há ${minutos / (60 * 24)} dias"
    }
}

/** "17 min", "2 h 5 min", "2 dias 4 h": quanto tempo um alerta ficou aberto. */
fun duracao(de: Instant, ate: Instant): String {
    val minutos = Duration.between(de, ate).toMinutes()
    val dias = minutos / (60 * 24)
    val horas = minutos / 60 % 24
    val resto = minutos % 60
    return when {
        minutos < 1 -> "menos de 1 min"
        dias > 0 -> listOfNotNull(if (dias == 1L) "1 dia" else "$dias dias", "$horas h".takeIf { horas > 0 }).joinToString(" ")
        horas > 0 -> listOfNotNull("$horas h", "$resto min".takeIf { resto > 0 }).joinToString(" ")
        else -> "$resto min"
    }
}

/** "vence em 5 dias", "venceu ontem": o prazo do lote doado contado a partir de hoje. */
fun prazoDeValidade(validade: LocalDate, hoje: LocalDate = LocalDate.now()): String =
    when (val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, validade)) {
        0L -> "vence hoje"
        1L -> "vence amanhã"
        -1L -> "venceu ontem"
        else -> if (dias > 0) "vence em $dias dias" else "venceu há ${-dias} dias"
    }
