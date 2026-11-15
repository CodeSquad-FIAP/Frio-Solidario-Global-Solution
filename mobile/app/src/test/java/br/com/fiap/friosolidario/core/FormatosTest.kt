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
}
