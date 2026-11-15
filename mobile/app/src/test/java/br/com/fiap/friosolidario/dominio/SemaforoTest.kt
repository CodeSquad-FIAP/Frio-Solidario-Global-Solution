package br.com.fiap.friosolidario.dominio

import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.data.LeituraDto
import br.com.fiap.friosolidario.data.TipoEquipamento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SemaforoTest {

    private fun equipamento(min: Double, max: Double, umidMax: Double = 85.0) =
        EquipamentoDto(1, 1, "Banco", "Geladeira", TipoEquipamento.GELADEIRA, min, max, umidMax)

    private fun leitura(temperatura: Double, umidade: Double = 70.0, porta: Boolean = false) =
        LeituraDto(1, 1, temperatura, umidade, porta, Instant.parse("2026-11-15T12:00:00Z"))

    private val geladeira = equipamento(2.0, 8.0) // largura 6 -> margem de atenção 0,6

    @Test
    fun `no meio da faixa fica verde`() {
        assertEquals(Semaforo.VERDE, semaforo(geladeira, leitura(5.0)))
        assertEquals(Semaforo.VERDE, semaforo(geladeira, leitura(7.3)))
    }

    @Test
    fun `acima da maxima ou abaixo da minima fica vermelho`() {
        assertEquals(Semaforo.VERMELHO, semaforo(geladeira, leitura(8.1)))
        assertEquals(Semaforo.VERMELHO, semaforo(geladeira, leitura(1.9)))
    }

    @Test
    fun `exatamente no limite ainda esta dentro da faixa, em atencao`() {
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(8.0)))
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(2.0)))
    }

    @Test
    fun `perto de um limite fica amarelo`() {
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(7.5)))
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(2.4)))
    }

    @Test
    fun `a cor vem da faixa do proprio equipamento, nao de valor fixo`() {
        val freezer = equipamento(-25.0, -18.0)
        assertEquals(Semaforo.VERDE, semaforo(freezer, leitura(-21.0)))
        assertEquals(Semaforo.VERMELHO, semaforo(freezer, leitura(-17.9)))
        assertEquals("5 graus é bom na geladeira e péssimo no freezer", Semaforo.VERMELHO, semaforo(freezer, leitura(5.0)))
    }

    @Test
    fun `umidade acima do limite fica amarelo mesmo com temperatura boa`() {
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(5.0, umidade = 85.5)))
        assertEquals(Semaforo.VERDE, semaforo(geladeira, leitura(5.0, umidade = 85.0)))
    }

    @Test
    fun `porta aberta fica amarelo`() {
        assertEquals(Semaforo.AMARELO, semaforo(geladeira, leitura(5.0, porta = true)))
    }

    @Test
    fun `temperatura fora da faixa vence umidade e porta`() {
        assertEquals(Semaforo.VERMELHO, semaforo(geladeira, leitura(9.0, umidade = 99.0, porta = true)))
    }

    @Test
    fun `sem leitura nao tem cor`() {
        assertEquals(Semaforo.SEM_LEITURA, semaforo(geladeira, null))
    }

    @Test
    fun `faixa estreita nao fica amarela o tempo todo`() {
        val estreita = equipamento(-19.0, -18.0) // largura 1 -> margem limitada a 0,25
        assertEquals(Semaforo.VERDE, semaforo(estreita, leitura(-18.5)))
        assertEquals(Semaforo.AMARELO, semaforo(estreita, leitura(-18.2)))
        assertEquals(Semaforo.VERMELHO, semaforo(estreita, leitura(-17.9)))
    }
}
