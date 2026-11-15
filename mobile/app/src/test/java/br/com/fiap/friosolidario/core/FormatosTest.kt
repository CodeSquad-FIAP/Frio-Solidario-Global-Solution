package br.com.fiap.friosolidario.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FormatosTest {

    private val agora = Instant.parse("2026-11-15T12:00:00Z")

    @Test
    fun `temperatura usa virgula e uma casa`() {
        // Espaço inseparável: o número nunca fica em uma linha e a unidade na outra.
        assertEquals("7,6\u00A0°C", temperatura(7.6))
        assertEquals("-18,0\u00A0°C", temperatura(-18.0))
        assertEquals("4,0\u00A0°C", temperatura(3.96))
    }

    @Test
    fun `umidade sem casas e sem espaco antes do sinal`() {
        assertEquals("72%", umidade(72.4))
    }

    @Test
    fun `tempo decorrido em minutos horas e dias`() {
        assertEquals("agora", tempoDecorrido(agora.minusSeconds(20), agora))
        assertEquals("há 4 min", tempoDecorrido(agora.minusSeconds(4 * 60 + 10), agora))
        assertEquals("há 2 h", tempoDecorrido(agora.minusSeconds(2 * 3600 + 300), agora))
        assertEquals("há 3 dias", tempoDecorrido(agora.minusSeconds(3 * 86400L + 5), agora))
        assertEquals("há 1 dia", tempoDecorrido(agora.minusSeconds(86400L + 5), agora))
    }

    @Test
    fun `relogio do sensor adiantado nao mostra tempo negativo`() {
        assertEquals("agora", tempoDecorrido(agora.plusSeconds(90), agora))
    }

    @Test
    fun `duracao entre a abertura e a resolucao do alerta`() {
        assertEquals("menos de 1 min", duracao(agora, agora.plusSeconds(40)))
        assertEquals("17 min", duracao(agora, agora.plusSeconds(17 * 60 + 30)))
        assertEquals("2 h 5 min", duracao(agora, agora.plusSeconds(2 * 3600 + 5 * 60)))
        assertEquals("3 h", duracao(agora, agora.plusSeconds(3 * 3600)))
        assertEquals("2 dias 4 h", duracao(agora, agora.plusSeconds(2 * 86400L + 4 * 3600)))
        assertEquals("menos de 1 min", duracao(agora, agora.minusSeconds(60)))
    }

    @Test
    fun `prazo de validade do lote em relacao a hoje`() {
        val hoje = java.time.LocalDate.of(2026, 11, 15)
        assertEquals("vence hoje", prazoDeValidade(hoje, hoje))
        assertEquals("vence amanhã", prazoDeValidade(hoje.plusDays(1), hoje))
        assertEquals("vence em 5 dias", prazoDeValidade(hoje.plusDays(5), hoje))
        assertEquals("venceu ontem", prazoDeValidade(hoje.minusDays(1), hoje))
        assertEquals("venceu há 3 dias", prazoDeValidade(hoje.minusDays(3), hoje))
    }
}
