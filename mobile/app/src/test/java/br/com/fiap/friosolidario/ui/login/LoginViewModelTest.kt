package br.com.fiap.friosolidario.ui.login

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.data.AuthRepository
import br.com.fiap.friosolidario.data.SessaoEmMemoria
import br.com.fiap.friosolidario.data.api
import br.com.fiap.friosolidario.data.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val servidor = MockWebServer()
    private val sessao = SessaoEmMemoria()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor.start()
        viewModel = LoginViewModel(AuthRepository(servidor.api(sessao), sessao))
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
        Dispatchers.resetMain()
    }

    /** A resposta chega em uma thread do OkHttp; espera o envio terminar. */
    private fun esperarResposta() {
        val limite = System.currentTimeMillis() + 5_000
        while (viewModel.estado.value.enviando && System.currentTimeMillis() < limite) Thread.sleep(10)
        assertEquals(false, viewModel.estado.value.enviando)
    }

    private fun preencher(senha: String = "Frio@2026") {
        viewModel.mudarEmail("mariana.costa@caminhosdobem.example")
        viewModel.mudarSenha(senha)
    }

    @Test
    fun `campo vazio mostra o erro sem chamar a api`() {
        viewModel.entrar()

        assertEquals("Informe o e-mail e a senha.", viewModel.estado.value.erroDeCampo)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `depois de entrar a senha nao fica guardada no formulario`() {
        servidor.enqueue(
            json(
                """{"token":"t","expiraEm":"2026-11-15T20:00:00+00:00","nome":"Mariana Costa","papel":"ADMIN",
                    "instituicaoId":1,"instituicao":"Banco"}""",
            ),
        )
        preencher()

        viewModel.entrar()
        esperarResposta()

        assertNotNull(sessao.atual)
        assertEquals("quem sair do app não encontra a senha preenchida", "", viewModel.estado.value.senha)
        assertEquals("o e-mail fica, para o próximo login", "mariana.costa@caminhosdobem.example", viewModel.estado.value.email)
        assertNull(viewModel.estado.value.erro)
    }

    @Test
    fun `senha errada mostra o erro e deixa corrigir`() {
        servidor.enqueue(json("""{"status":401,"detail":"E-mail ou senha invalidos."}""", 401))
        preencher(senha = "errada1")

        viewModel.entrar()
        esperarResposta()

        assertEquals(ErroApp.CredenciaisInvalidas, viewModel.estado.value.erro)
        assertEquals("errada1", viewModel.estado.value.senha)

        viewModel.mudarSenha("Frio@2026")
        assertNull("digitar de novo limpa o erro", viewModel.estado.value.erro)
    }

    @Test
    fun `api desligada mostra sem conexao e mantem o que foi digitado`() {
        servidor.shutdown()
        preencher()

        viewModel.entrar()
        esperarResposta()

        assertEquals(ErroApp.SemConexao, viewModel.estado.value.erro)
        assertEquals("Frio@2026", viewModel.estado.value.senha)
    }
}
