package br.com.fiap.friosolidario.dominio

import br.com.fiap.friosolidario.data.LeituraDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EscalaDoGraficoTest {

    private val inicio = Instant.parse("2026-11-14T12:00:00Z")
    private val fim = Instant.parse("2026-11-15T12:00:00Z")

    private fun leitura(temperatura: Double, em: Instant = inicio) = LeituraDto(1, 1, temperatura, 70.0, false, em)

    @Test
    fun `eixo sempre mostra os dois limites da faixa, com folga`() {
        val escala = escalaPara(listOf(leitura(2.0), leitura(3.0)), tempMin = 0.0, tempMax = 4.0, inicio, fim)

        assertEquals(-0.4, escala.minY, 1e-9)
        assertEquals(4.4, escala.maxY, 1e-9)
    }

    @Test
    fun `ponto fora da faixa entra no eixo em vez de sair do grafico`() {
        val escala = escalaPara(listOf(leitura(9.0), leitura(-2.0)), tempMin = 0.0, tempMax = 4.0, inicio, fim)

        assertTrue(escala.maxY > 9.0)
        assertTrue(escala.minY < -2.0)
    }

    @Test
    fun `sem leituras o eixo usa so a faixa`() {
        val escala = escalaPara(emptyList(), tempMin = -25.0, tempMax = -18.0, inicio, fim)

        assertEquals(-25.7, escala.minY, 1e-9)
        assertEquals(-17.3, escala.maxY, 1e-9)
    }

    @Test
    fun `topo e o maior valor e base e o menor`() {
        val escala = escalaPara(listOf(leitura(2.0)), 0.0, 4.0, inicio, fim)

        assertEquals(0f, escala.y(escala.maxY, altura = 200f), 1e-3f)
        assertEquals(200f, escala.y(escala.minY, altura = 200f), 1e-3f)
        assertEquals(100f, escala.y((escala.minY + escala.maxY) / 2, altura = 200f), 1e-3f)
    }

    @Test
    fun `tempo vai da esquerda para a direita`() {
        val escala = escalaPara(emptyList(), 0.0, 4.0, inicio, fim)

        assertEquals(0f, escala.x(inicio, largura = 300f), 1e-3f)
        assertEquals(300f, escala.x(fim, largura = 300f), 1e-3f)
        assertEquals(150f, escala.x(Instant.parse("2026-11-15T00:00:00Z"), largura = 300f), 1e-3f)
    }

    @Test
    fun `janela de tempo vazia nao divide por zero`() {
        val escala = escalaPara(listOf(leitura(2.0)), 0.0, 4.0, inicio, inicio)

        val x = escala.x(inicio, largura = 300f)
        assertFalse(x.isNaN() || x.isInfinite())
        assertEquals(150f, x, 1e-3f)
    }

    @Test
    fun `faixa degenerada com todos os valores iguais nao divide por zero`() {
        val escala = escalaPara(listOf(leitura(4.0), leitura(4.0)), tempMin = 4.0, tempMax = 4.0, inicio, fim)

        assertTrue(escala.maxY > escala.minY)
        val y = escala.y(4.0, altura = 200f)
        assertFalse(y.isNaN() || y.isInfinite())
        assertEquals(100f, y, 1e-3f)
    }

    @Test
    fun `resumo da serie conta minima maxima media e fora da faixa`() {
        val resumo = resumoDaSerie(listOf(leitura(1.0), leitura(3.0), leitura(8.0), leitura(-1.0)), tempMin = 0.0, tempMax = 4.0)!!

        assertEquals(-1.0, resumo.minima, 1e-9)
        assertEquals(8.0, resumo.maxima, 1e-9)
        assertEquals(2.75, resumo.media, 1e-9)
        assertEquals(2, resumo.foraDaFaixa)
        assertEquals(4, resumo.total)
    }

    @Test
    fun `serie vazia nao tem resumo`() {
        assertEquals(null, resumoDaSerie(emptyList(), 0.0, 4.0))
    }

    @Test
    fun `marcas do eixo sao numeros redondos dentro do eixo`() {
        val escala = escalaPara(listOf(leitura(8.1), leitura(0.7)), tempMin = 0.0, tempMax = 4.0, inicio, fim)

        assertEquals(listOf(0.0, 2.0, 4.0, 6.0, 8.0), escala.marcasDoEixoY())
    }

    @Test
    fun `marcas do eixo em faixa negativa e estreita`() {
        assertEquals(
            listOf(-24.0, -22.0, -20.0, -18.0),
            escalaPara(emptyList(), tempMin = -25.0, tempMax = -18.0, inicio, fim).marcasDoEixoY(),
        )
        assertEquals(
            listOf(-19.0, -18.5, -18.0),
            escalaPara(emptyList(), tempMin = -19.0, tempMax = -18.0, inicio, fim).marcasDoEixoY(),
        )
    }

    @Test
    fun `eixo muito largo nao gera dezenas de marcas`() {
        val marcas = escalaPara(listOf(leitura(-40.0), leitura(80.0)), 0.0, 4.0, inicio, fim).marcasDoEixoY()

        assertTrue(marcas.size in 3..7)
    }
}
