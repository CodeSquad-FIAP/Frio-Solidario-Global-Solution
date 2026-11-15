package br.com.fiap.friosolidario.ui.painel

import br.com.fiap.friosolidario.data.AuthRepository
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.data.MainDeTeste
import br.com.fiap.friosolidario.data.RotasMutaveis
import br.com.fiap.friosolidario.data.SessaoEmMemoria
import br.com.fiap.friosolidario.data.api
import br.com.fiap.friosolidario.data.equipamentoJson
import br.com.fiap.friosolidario.data.esperar
import br.com.fiap.friosolidario.data.json
import br.com.fiap.friosolidario.data.leituraJson
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class PainelViewModelTest {

    @get:Rule
    val main = MainDeTeste()

    private val servidor = MockWebServer()
    private val rotas = RotasMutaveis()
    private lateinit var viewModel: PainelViewModel

    @Before
    fun preparar() {
        rotas["/api/equipamentos?ativo=true"] = {
            json(
                "[${equipamentoJson(1, ultimaLeitura = leituraJson(10, 1, 9.0, "2026-11-15T10:05:00+00:00"))}," +
                    "${equipamentoJson(2, min = -25.0, max = -18.0, ultimaLeitura = leituraJson(20, 2, 2.0, "2026-11-15T10:05:00+00:00"))}]",
            )
        }
        servidor.dispatcher = rotas
        servidor.start()
        val api = servidor.api()
        viewModel = PainelViewModel(
            EquipamentoRepository(api, aoExpirar = {}, agora = { Instant.parse("2026-11-15T10:15:00Z") }),
            AuthRepository(api, SessaoEmMemoria()),
        )
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun itens() = (viewModel.estado.value.tela as EstadoTela.Conteudo).valor
    private fun pedidosDaLista() = rotas.pedidos.count { it.endsWith("/api/equipamentos?ativo=true") }

    private fun carregar() {
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Conteudo }
    }

    private fun atualizar() {
        val antes = pedidosDaLista()
        viewModel.puxarParaAtualizar()
        esperar { pedidosDaLista() > antes && !viewModel.estado.value.atualizando }
    }

    @Test
    fun `mostra a leitura que veio na listagem com o pior equipamento primeiro`() {
        carregar()

        assertEquals(listOf(1L, 2L), itens().map { it.equipamento.id })
        assertEquals(Semaforo.VERMELHO, itens().first { it.equipamento.id == 1L }.semaforo)
        assertEquals(9.0, itens().first { it.equipamento.id == 1L }.ultimaLeitura!!.temperatura, 0.0)
        assertEquals("uma requisição por ciclo de atualização", 1, pedidosDaLista())
        assertNull(viewModel.estado.value.erroAoAtualizar)
    }

    @Test
    fun `atualizacao que falha mantem a lista anterior e avisa`() {
        carregar()

        rotas["/api/equipamentos?ativo=true"] = { MockResponse().setResponseCode(500) }
        atualizar()

        val camara = itens().first { it.equipamento.id == 1L }
        assertEquals(Semaforo.VERMELHO, camara.semaforo)
        assertEquals(9.0, camara.ultimaLeitura!!.temperatura, 0.0)
        assertNotNull(viewModel.estado.value.erroAoAtualizar)
    }

    @Test
    fun `atualizacao que volta a responder limpa o aviso`() {
        rotas["/api/equipamentos?ativo=true"] = { MockResponse().setResponseCode(500) }
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Erro }

        rotas["/api/equipamentos?ativo=true"] = {
            json("[${equipamentoJson(1, ultimaLeitura = leituraJson(11, 1, 3.0, "2026-11-15T10:10:00+00:00"))}]")
        }
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Conteudo }

        assertEquals(Semaforo.VERDE, itens().single().semaforo)
        assertNull(viewModel.estado.value.erroAoAtualizar)
    }

    @Test
    fun `falha na primeira carga mostra erro com tentar de novo`() {
        rotas["/api/equipamentos?ativo=true"] = { MockResponse().setResponseCode(500) }
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Erro }

        assertNotNull((viewModel.estado.value.tela as EstadoTela.Erro).erro)
    }

    @Test
    fun `equipamento sem leitura na listagem aparece sem leitura`() {
        rotas["/api/equipamentos?ativo=true"] = { json("[${equipamentoJson(1)}]") }
        carregar()

        assertNull(itens().single().ultimaLeitura)
        assertEquals(Semaforo.SEM_LEITURA, itens().single().semaforo)
    }
}
