package br.com.fiap.friosolidario.ui.lotes

import br.com.fiap.friosolidario.data.LoteRepository
import br.com.fiap.friosolidario.data.MainDeTeste
import br.com.fiap.friosolidario.data.RotasMutaveis
import br.com.fiap.friosolidario.data.StatusLote
import br.com.fiap.friosolidario.data.api
import br.com.fiap.friosolidario.data.esperar
import br.com.fiap.friosolidario.data.json
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LotesViewModelTest {

    @get:Rule
    val main = MainDeTeste()

    private val servidor = MockWebServer()
    private val rotas = RotasMutaveis()
    private val repositorio by lazy { LoteRepository(servidor.api(), aoExpirar = {}) }

    private fun lote(id: Long, categoria: String, status: String = "DISPONIVEL") =
        """{"id":$id,"instituicaoId":1,"descricao":"Lote $id","categoria":"$categoria","kg":10.0,
            "validade":"2026-11-24","status":"$status","retiradoEm":null}"""

    @Before
    fun preparar() {
        rotas["/api/lotes"] = { json("[${lote(1, "FRUTAS")},${lote(2, "HORTIFRUTI")},${lote(3, "frutas")}]") }
        servidor.dispatcher = rotas
        servidor.start()
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    @Test
    fun `registrar a saida atualiza o lote na lista e avisa`() {
        rotas["/api/lotes/1/retirada"] = { json(lote(1, "FRUTAS", status = "DISTRIBUIDO")) }
        val viewModel = LotesViewModel(repositorio)
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Conteudo }
        val alvo = (viewModel.estado.value.tela as EstadoTela.Conteudo).valor.first { it.id == 1L }

        viewModel.pedirSaida(alvo, StatusLote.DISTRIBUIDO)
        viewModel.confirmarSaida()

        assertEquals("Lote marcado como distribuído.", runBlocking { withTimeout(5_000) { viewModel.avisos.first() } })
        val lista = (viewModel.estado.value.tela as EstadoTela.Conteudo).valor
        assertEquals(StatusLote.DISTRIBUIDO, lista.first { it.id == 1L }.status)
    }

    @Test
    fun `as sugestoes de categoria vem dos lotes da api, sem repetir`() {
        val viewModel = LoteFormViewModel(repositorio)
        esperar { viewModel.estado.value.categoriasSugeridas.isNotEmpty() }

        assertEquals(listOf("Frutas", "Hortifruti"), viewModel.estado.value.categoriasSugeridas)
    }

    @Test
    fun `sem lotes ou sem conexao o formulario fica sem sugestoes e continua utilizavel`() {
        rotas["/api/lotes"] = { MockResponse().setResponseCode(500) }
        val viewModel = LoteFormViewModel(repositorio)
        esperar { rotas.pedidos.isNotEmpty() }
        Thread.sleep(200)

        assertTrue(viewModel.estado.value.categoriasSugeridas.isEmpty())
        viewModel.mudarCategoria("Padaria")
        assertEquals("Padaria", viewModel.estado.value.categoria)
    }
}
