package br.com.fiap.friosolidario.ui.detalhe

import br.com.fiap.friosolidario.data.MainDeTeste
import br.com.fiap.friosolidario.data.RelatorioRepository
import br.com.fiap.friosolidario.data.api
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RelatorioViewModelTest {

    @get:Rule
    val main = MainDeTeste()

    @get:Rule
    val pasta = TemporaryFolder()

    private val servidor = MockWebServer()
    private lateinit var viewModel: RelatorioViewModel

    @Before
    fun preparar() {
        servidor.start()
        viewModel = RelatorioViewModel(1, RelatorioRepository(servidor.api(), aoExpirar = {}, pasta = { pasta.root }))
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    /** Dias do calendário cobertos pelo período, contando o primeiro e o último. */
    private fun diasCobertos() = with(viewModel.estado.value) { ChronoUnit.DAYS.between(de, ate) + 1 }

    @Test
    fun `ultimos 7 dias cobre sete dias do calendario, terminando hoje`() {
        viewModel.abrir()

        assertEquals(7, diasCobertos())
        assertEquals(LocalDate.now(), viewModel.estado.value.ate)
    }

    @Test
    fun `ultimos 30 dias cobre trinta dias do calendario`() {
        viewModel.abrir()
        viewModel.escolherOpcao(OpcaoDePeriodo.TRINTA_DIAS)

        assertEquals(30, diasCobertos())
    }

    @Test
    fun `voltar para 7 dias depois de 30 recalcula o inicio`() {
        viewModel.abrir()
        viewModel.escolherOpcao(OpcaoDePeriodo.TRINTA_DIAS)
        viewModel.escolherOpcao(OpcaoDePeriodo.SETE_DIAS)

        assertEquals(7, diasCobertos())
    }
}
