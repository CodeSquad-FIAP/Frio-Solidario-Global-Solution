package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.dominio.Semaforo
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class EquipamentoRepositoryTest {

    private val servidor = MockWebServer()
    private var expiracoes = 0
    private lateinit var repositorio: EquipamentoRepository

    @Before
    fun preparar() {
        servidor.start()
        repositorio = EquipamentoRepository(
            servidor.api(),
            aoExpirar = { expiracoes++ },
            agora = { Instant.parse("2026-11-15T10:15:00Z") },
        )
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun <T> Resultado<T>.valor(): T = (this as Resultado.Ok).valor

    @Test
    fun `painel usa a ultima leitura que vem na propria listagem`() = runTest {
        servidor.rotas(
            "/api/equipamentos?ativo=true" to json(
                "[${equipamentoJson(1, ultimaLeitura = leituraJson(11, 1, 7.8, "2026-11-15T10:10:00+00:00"))}," +
                    "${equipamentoJson(2, min = -25.0, max = -18.0, ultimaLeitura = leituraJson(20, 2, -20.0, "2026-11-15T10:05:00+00:00"))}]",
            ),
        )

        val painel = repositorio.painel().valor()

        assertEquals(listOf(1L, 2L), painel.map { it.equipamento.id })
        assertEquals(7.8, painel[0].ultimaLeitura!!.temperatura, 0.0)
        assertEquals(Instant.parse("2026-11-15T10:10:00Z"), painel[0].ultimaLeitura!!.registradaEm)
        assertEquals(Semaforo.VERMELHO, painel[0].semaforo)
        assertEquals(Semaforo.VERDE, painel[1].semaforo)
        assertEquals("uma requisição responde o painel inteiro", 1, servidor.requestCount)
    }

    @Test
    fun `leitura antiga aparece mas nao vale como verde`() = runTest {
        servidor.rotas(
            "/api/equipamentos?ativo=true" to json(
                "[${equipamentoJson(1, ultimaLeitura = leituraJson(5, 1, 2.0, "2026-11-12T08:00:00+00:00"))}]",
            ),
        )

        val item = repositorio.painel().valor().single()

        assertEquals(Instant.parse("2026-11-12T08:00:00Z"), item.ultimaLeitura!!.registradaEm)
        assertEquals(Semaforo.SEM_SINAL, item.semaforo)
    }

    @Test
    fun `equipamento sem nenhuma leitura fica sem cor`() = runTest {
        servidor.rotas("/api/equipamentos?ativo=true" to json("[${equipamentoJson(1)}]"))

        val item = repositorio.painel().valor().single()

        assertNull(item.ultimaLeitura)
        assertEquals(Semaforo.SEM_LEITURA, item.semaforo)
    }

    @Test
    fun `falha na listagem vira erro em vez de tudo cinza`() = runTest {
        servidor.rotas("/api/equipamentos?ativo=true" to MockResponse().setResponseCode(500))

        assertEquals(Resultado.Falha(ErroApp.Servidor), repositorio.painel())
    }

    @Test
    fun `instituicao sem equipamentos devolve lista vazia`() = runTest {
        servidor.rotas("/api/equipamentos?ativo=true" to json("[]"))

        assertTrue(repositorio.painel().valor().isEmpty())
    }

    @Test
    fun `token vencido avisa a sessao`() = runTest {
        servidor.rotas("/api/equipamentos?ativo=true" to json("""{"status":401,"detail":"Token expirado"}""", 401))

        assertEquals(Resultado.Falha(ErroApp.SessaoExpirada), repositorio.painel())
        assertEquals(1, expiracoes)
    }

    @Test
    fun `api desligada vira sem conexao`() = runTest {
        servidor.shutdown()

        assertEquals(Resultado.Falha(ErroApp.SemConexao), repositorio.painel())
    }

    @Test
    fun `cadastro envia a faixa e devolve o criado`() = runTest {
        servidor.enqueue(json(equipamentoJson(7, nome = "Freezer novo", min = -25.0, max = -18.0), 201))

        val criado = repositorio.criar(
            EquipamentoRequest(1, "Freezer novo", TipoEquipamento.FREEZER, -25.0, -18.0, 85.0),
        ).valor()

        assertEquals(7L, criado.id)
        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/api/equipamentos", pedido.path)
        assertEquals(
            """{"instituicaoId":1,"nome":"Freezer novo","tipo":"FREEZER","tempMin":-25.0,"tempMax":-18.0,"umidMax":85.0,"ativo":true}""",
            pedido.body.readUtf8(),
        )
    }

    @Test
    fun `erro 400 do cadastro traz a frase da api`() = runTest {
        servidor.enqueue(json("""{"status":400,"detail":"temp_min deve ser menor que temp_max."}""", 400))

        val resultado = repositorio.criar(EquipamentoRequest(1, "Freezer", TipoEquipamento.FREEZER, 5.0, 1.0, 85.0))

        assertEquals(Resultado.Falha(ErroApp.Validacao("temp_min deve ser menor que temp_max.")), resultado)
    }
}
