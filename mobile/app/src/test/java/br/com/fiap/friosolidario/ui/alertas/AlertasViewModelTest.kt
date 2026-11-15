package br.com.fiap.friosolidario.ui.alertas

import br.com.fiap.friosolidario.data.AcaoTomada
import br.com.fiap.friosolidario.data.AlertaRepository
import br.com.fiap.friosolidario.data.MainDeTeste
import br.com.fiap.friosolidario.data.RotasMutaveis
import br.com.fiap.friosolidario.data.api
import br.com.fiap.friosolidario.data.esperar
import br.com.fiap.friosolidario.data.json
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class AlertasViewModelTest {

    @get:Rule
    val main = MainDeTeste()

    private val servidor = MockWebServer()
    private val rotas = RotasMutaveis()
    private lateinit var viewModel: AlertasViewModel

    private fun alerta(status: String, extra: String = "") =
        """{"id":6,"equipamentoId":1,"equipamento":"Câmara fria","leituraId":9,"tipo":"TEMP_ALTA",
            "severidade":"CRITICA","status":"$status","criadoEm":"2026-11-15T10:00:00+00:00"$extra}"""

    private val resolvido = alerta("RESOLVIDO", ""","resolvidoEm":"2026-11-15T10:05:00+00:00","resolvidoPorId":1,"acaoTomada":"TRANSFERIU_LOTE"""")

    @Before
    fun preparar() {
        rotas["/api/alertas?status=ABERTO"] = { json("[${alerta("ABERTO")}]") }
        rotas["/api/alertas?status=RESOLVIDO"] = { json("[]") }
        rotas["/api/alertas/6/resolver"] = { json(resolvido) }
        servidor.dispatcher = rotas
        servidor.start()
        viewModel = AlertasViewModel(AlertaRepository(servidor.api(), aoExpirar = {}))
        viewModel.carregar()
        esperar { viewModel.estado.value.tela is EstadoTela.Conteudo }
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun listas() = (viewModel.estado.value.tela as EstadoTela.Conteudo).valor

    private fun resolver(acao: AcaoTomada = AcaoTomada.TRANSFERIU_LOTE) {
        viewModel.abrirResolucao(listas().abertos.single())
        viewModel.escolherAcao(acao)
        viewModel.confirmarResolucao()
    }

    private fun proximoAviso(): String = runBlocking { withTimeout(5_000) { viewModel.avisos.first() } }

    @Test
    fun `resolver tira o alerta da fila, poe no historico e avisa`() {
        resolver()

        assertEquals("Alerta resolvido: transferi o lote", proximoAviso())
        assertTrue(listas().abertos.isEmpty())
        assertEquals(AcaoTomada.TRANSFERIU_LOTE, listas().historico.single().acaoTomada)
        assertNull("o diálogo fecha", viewModel.estado.value.resolucao)
    }

    @Test
    fun `o aviso espera por quem vai mostra-lo em vez de se perder`() {
        resolver()
        esperar { viewModel.estado.value.resolucao == null }
        Thread.sleep(100) // a tela só começa a ouvir depois

        assertEquals("Alerta resolvido: transferi o lote", proximoAviso())
    }

    @Test
    fun `atualizacao que ja estava a caminho nao devolve o alerta resolvido para a fila`() {
        // A consulta periódica sai antes da resolução e a resposta dela (ainda com o alerta
        // aberto) chega depois.
        rotas["/api/alertas?status=ABERTO"] = { json("[${alerta("ABERTO")}]").setBodyDelay(600, TimeUnit.MILLISECONDS) }
        viewModel.atualizarEmSilencio()
        Thread.sleep(100)

        resolver()
        proximoAviso()
        Thread.sleep(900)

        assertTrue("o alerta resolvido não volta para Abertos", listas().abertos.isEmpty())
        assertEquals(1, listas().historico.size)
    }

    @Test
    fun `alerta ja resolvido por outra pessoa fecha o dialogo, avisa e atualiza a fila`() {
        rotas["/api/alertas/6/resolver"] = { json("""{"status":409,"detail":"Alerta 6 ja foi resolvido."}""", 409) }
        val alvo = listas().abertos.single()
        rotas["/api/alertas?status=ABERTO"] = { json("[]") }
        rotas["/api/alertas?status=RESOLVIDO"] = { json("[$resolvido]") }

        viewModel.abrirResolucao(alvo)
        viewModel.escolherAcao(AcaoTomada.AJUSTOU)
        viewModel.confirmarResolucao()

        assertEquals("Este alerta já tinha sido resolvido.", proximoAviso())
        esperar { listas().abertos.isEmpty() }
        assertNull(viewModel.estado.value.resolucao)
    }

    @Test
    fun `falha de rede ao resolver mantem o dialogo aberto para tentar de novo`() {
        rotas["/api/alertas/6/resolver"] = { json("""{"status":500}""", 500) }

        resolver()
        esperar { viewModel.estado.value.resolucao?.erro != null }

        assertEquals(false, viewModel.estado.value.resolucao!!.enviando)
        assertEquals(1, listas().abertos.size)
    }
}
