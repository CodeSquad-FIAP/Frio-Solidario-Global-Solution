package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AlertaRepositoryTest {

    private val servidor = MockWebServer()
    private var expiracoes = 0
    private lateinit var repositorio: AlertaRepository

    @Before
    fun preparar() {
        servidor.start()
        repositorio = AlertaRepository(servidor.api(), aoExpirar = { expiracoes++ })
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun alerta(id: Long, severidade: String, status: String, extra: String = "") =
        """{"id":$id,"equipamentoId":1,"equipamento":"Câmara fria","leituraId":9,"tipo":"TEMP_ALTA",
            "severidade":"$severidade","status":"$status","criadoEm":"2026-11-15T10:00:00+00:00"$extra}"""

    @Test
    fun `fila pede so os abertos e mantem a ordem da api`() = runTest {
        servidor.enqueue(json("[${alerta(6, "CRITICA", "ABERTO")},${alerta(4, "MEDIA", "ABERTO")}]"))

        val fila = (repositorio.abertos() as Resultado.Ok).valor

        assertEquals("/api/alertas?status=ABERTO", servidor.takeRequest().path)
        assertEquals(listOf(6L, 4L), fila.map { it.id })
        assertEquals(Severidade.CRITICA, fila[0].severidade)
        assertEquals(Instant.parse("2026-11-15T10:00:00Z"), fila[0].criadoEm)
        assertNull(fila[0].acaoTomada)
    }

    @Test
    fun `historico traz os resolvidos, o mais recente primeiro, com a acao tomada`() = runTest {
        servidor.enqueue(
            json(
                "[" + alerta(2, "CRITICA", "RESOLVIDO", ""","resolvidoEm":"2026-11-14T08:00:00+00:00","resolvidoPorId":2,"acaoTomada":"DESCARTOU"""") +
                    "," + alerta(3, "BAIXA", "RESOLVIDO", ""","resolvidoEm":"2026-11-15T09:00:00+00:00","resolvidoPorId":2,"acaoTomada":null""") + "]",
            ),
        )

        val historico = (repositorio.historico() as Resultado.Ok).valor

        assertEquals("/api/alertas?status=RESOLVIDO", servidor.takeRequest().path)
        assertEquals("ordenado pela resolução, não pela severidade", listOf(3L, 2L), historico.map { it.id })
        assertNull("alerta antigo, resolvido sem ação", historico[0].acaoTomada)
        assertEquals(AcaoTomada.DESCARTOU, historico[1].acaoTomada)
    }

    @Test
    fun `resolver manda a acao escolhida no corpo do patch`() = runTest {
        servidor.enqueue(
            json(alerta(6, "CRITICA", "RESOLVIDO", ""","resolvidoEm":"2026-11-15T10:05:00+00:00","resolvidoPorId":1,"acaoTomada":"TRANSFERIU_LOTE"""")),
        )

        val resolvido = (repositorio.resolver(6, AcaoTomada.TRANSFERIU_LOTE) as Resultado.Ok).valor

        val pedido = servidor.takeRequest()
        assertEquals("PATCH", pedido.method)
        assertEquals("/api/alertas/6/resolver", pedido.path)
        assertEquals("""{"acao":"TRANSFERIU_LOTE"}""", pedido.body.readUtf8())
        assertEquals(StatusAlerta.RESOLVIDO, resolvido.status)
        assertEquals(AcaoTomada.TRANSFERIU_LOTE, resolvido.acaoTomada)
    }

    @Test
    fun `alerta ja resolvido por outra pessoa vira conflito com a frase da api`() = runTest {
        servidor.enqueue(json("""{"status":409,"detail":"Alerta 6 ja foi resolvido."}""", 409))

        assertEquals(
            Resultado.Falha(ErroApp.Conflito("Alerta 6 ja foi resolvido.")),
            repositorio.resolver(6, AcaoTomada.AJUSTOU),
        )
    }

    @Test
    fun `token vencido avisa a sessao`() = runTest {
        servidor.enqueue(json("""{"status":401}""", 401))

        assertEquals(Resultado.Falha(ErroApp.SessaoExpirada), repositorio.abertos())
        assertEquals(1, expiracoes)
    }

    @Test
    fun `api desligada vira sem conexao`() = runTest {
        servidor.shutdown()

        assertEquals(Resultado.Falha(ErroApp.SemConexao), repositorio.abertos())
    }
}
