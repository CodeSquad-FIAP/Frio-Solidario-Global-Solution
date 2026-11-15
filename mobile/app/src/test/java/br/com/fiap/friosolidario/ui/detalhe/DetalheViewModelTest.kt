package br.com.fiap.friosolidario.ui.detalhe

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.data.MainDeTeste
import br.com.fiap.friosolidario.data.Periodo
import br.com.fiap.friosolidario.data.RotasMutaveis
import br.com.fiap.friosolidario.data.api
import br.com.fiap.friosolidario.data.equipamentoJson
import br.com.fiap.friosolidario.data.esperar
import br.com.fiap.friosolidario.data.json
import br.com.fiap.friosolidario.data.leituraJson
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DetalheViewModelTest {

    @get:Rule
    val main = MainDeTeste()

    private val servidor = MockWebServer()
    private val rotas = RotasMutaveis()
    private lateinit var viewModel: DetalheViewModel

    private val serie24h = "[${leituraJson(1, 1, 2.0, "2026-11-15T10:00:00+00:00")},${leituraJson(2, 1, 3.0, "2026-11-15T10:10:00+00:00")}]"

    @Before
    fun preparar() {
        rotas["/api/equipamentos/1"] = {
            json(equipamentoJson(1, ultimaLeitura = leituraJson(2, 1, 3.0, "2026-11-15T10:10:00+00:00")))
        }
        rotas["/api/equipamentos/1/leituras?periodo=24h"] = { json(serie24h) }
        rotas["/api/equipamentos/1/leituras?periodo=7d"] = { json(serie24h) }
        servidor.dispatcher = rotas
        servidor.start()
        viewModel = DetalheViewModel(1, EquipamentoRepository(servidor.api(), aoExpirar = {}))
        esperar { viewModel.estado.value.serie is EstadoTela.Conteudo && viewModel.estado.value.equipamento is EstadoTela.Conteudo }
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun pedidosDaSerie() = rotas.pedidos.count { it.endsWith("/leituras?periodo=24h") }

    @Test
    fun `abre com o equipamento, a serie de 24h e a ultima leitura`() {
        val estado = viewModel.estado.value

        assertEquals(Periodo.H24, estado.periodo)
        assertEquals(2, (estado.serie as EstadoTela.Conteudo).valor.leituras.size)
        assertEquals(3.0, estado.ultimaLeitura!!.temperatura, 0.0)
    }

    @Test
    fun `trocar o periodo recarrega so a serie`() {
        val pedidosDoEquipamento = rotas.pedidos.count { it == "GET /api/equipamentos/1" }

        viewModel.mudarPeriodo(Periodo.D7)
        esperar { (viewModel.estado.value.serie as? EstadoTela.Conteudo)?.valor?.periodo == Periodo.D7 }

        assertEquals(pedidosDoEquipamento, rotas.pedidos.count { it == "GET /api/equipamentos/1" })
        assertTrue(viewModel.estado.value.equipamento is EstadoTela.Conteudo)
    }

    @Test
    fun `falha na atualizacao periodica mantem o grafico e mostra a faixa de erro`() {
        rotas["/api/equipamentos/1/leituras?periodo=24h"] = { MockResponse().setResponseCode(500) }

        viewModel.atualizarEmSilencio()
        esperar { viewModel.estado.value.erroAoAtualizar != null }

        assertEquals(ErroApp.Servidor, viewModel.estado.value.erroAoAtualizar)
        assertTrue("a série anterior continua na tela", viewModel.estado.value.serie is EstadoTela.Conteudo)
    }

    @Test
    fun `tentar de novo na faixa de erro busca a serie outra vez`() {
        rotas["/api/equipamentos/1/leituras?periodo=24h"] = { MockResponse().setResponseCode(500) }
        viewModel.atualizarEmSilencio()
        esperar { viewModel.estado.value.erroAoAtualizar != null }
        rotas["/api/equipamentos/1/leituras?periodo=24h"] = { json(serie24h) }
        val antes = pedidosDaSerie()

        viewModel.tentarSerieDeNovo()
        esperar { viewModel.estado.value.erroAoAtualizar == null }

        assertEquals("o botão precisa mesmo chamar a API", antes + 1, pedidosDaSerie())
        assertNull(viewModel.estado.value.erroAoAtualizar)
    }
}
