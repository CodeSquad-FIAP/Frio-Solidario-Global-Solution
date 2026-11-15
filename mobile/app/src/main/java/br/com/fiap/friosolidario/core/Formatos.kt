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
