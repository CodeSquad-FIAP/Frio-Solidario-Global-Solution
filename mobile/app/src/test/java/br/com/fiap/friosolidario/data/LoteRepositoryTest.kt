package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class LoteRepositoryTest {

    private val servidor = MockWebServer()
    private lateinit var repositorio: LoteRepository

    @Before
    fun preparar() {
        servidor.start()
        repositorio = LoteRepository(servidor.api(), aoExpirar = {})
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun lote(id: Long, status: String, retirado: String = "null") =
        """{"id":$id,"instituicaoId":1,"descricao":"Maçãs","categoria":"FRUTAS","kg":85.0,
            "validade":"2026-11-24","status":"$status","retiradoEm":$retirado}"""

    @Test
    fun `lista todos os lotes com data de validade`() = runTest {
        servidor.enqueue(json("[${lote(2, "DISPONIVEL")},${lote(1, "DISTRIBUIDO", "\"2026-11-13T10:00:00+00:00\"")}]"))

        val lotes = (repositorio.listar() as Resultado.Ok).valor

        assertEquals("/api/lotes", servidor.takeRequest().path)
        assertEquals(LocalDate.of(2026, 11, 24), lotes[0].validade)
        assertEquals(StatusLote.DISTRIBUIDO, lotes[1].status)
    }

    @Test
    fun `cadastro manda descricao categoria kg e validade`() = runTest {
        servidor.enqueue(json(lote(9, "DISPONIVEL"), 201))

        val criado = (repositorio.criar("Maçãs", "Frutas", 85.0, LocalDate.of(2026, 11, 24)) as Resultado.Ok).valor

        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/api/lotes", pedido.path)
        assertEquals(
            """{"descricao":"Maçãs","categoria":"Frutas","kg":85.0,"validade":"2026-11-24"}""",
            pedido.body.readUtf8(),
        )
        assertEquals("a API cria o lote sempre disponível", StatusLote.DISPONIVEL, criado.status)
    }

    @Test
    fun `saida do estoque manda o novo status`() = runTest {
        servidor.enqueue(json(lote(2, "PERDIDO", "\"2026-11-15T10:00:00+00:00\"")))

        repositorio.registrarSaida(2, StatusLote.PERDIDO)

        val pedido = servidor.takeRequest()
        assertEquals("PATCH", pedido.method)
        assertEquals("/api/lotes/2/retirada", pedido.path)
        assertEquals("""{"status":"PERDIDO"}""", pedido.body.readUtf8())
    }

    @Test
    fun `voluntario cadastrando lote recebe sem permissao`() = runTest {
        servidor.enqueue(json("""{"status":403}""", 403))

        assertEquals(
            Resultado.Falha(ErroApp.SemPermissao),
            repositorio.criar("Maçãs", "Frutas", 85.0, LocalDate.of(2026, 11, 24)),
        )
    }
}
