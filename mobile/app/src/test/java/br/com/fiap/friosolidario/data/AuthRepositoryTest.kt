package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Papel
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.Sessao
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AuthRepositoryTest {

    private val servidor = MockWebServer()
    private val sessao = SessaoEmMemoria()
    private lateinit var repositorio: AuthRepository

    @Before
    fun preparar() {
        servidor.start()
        repositorio = AuthRepository(servidor.api(sessao), sessao)
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    @Test
    fun `login valido guarda a sessao com token papel e instituicao`() = runTest {
        servidor.enqueue(
            json(
                """{"token":"abc.def.ghi","expiraEm":"2026-11-15T20:00:00+00:00","nome":"Mariana Costa",
                    "papel":"ADMIN","instituicaoId":1,"instituicao":"Banco de Alimentos Caminhos do Bem"}""",
            ),
        )

        val resultado = repositorio.entrar("mariana.costa@caminhosdobem.example", "Frio@2026")

        val esperada = Sessao(
            token = "abc.def.ghi",
            expiraEm = Instant.parse("2026-11-15T20:00:00Z"),
            nome = "Mariana Costa",
            papel = Papel.ADMIN,
            instituicaoId = 1,
            instituicao = "Banco de Alimentos Caminhos do Bem",
        )
        assertEquals(Resultado.Ok(esperada), resultado)
        assertEquals(esperada, sessao.atual)

        val pedido = servidor.takeRequest()
        assertEquals("/api/auth/login", pedido.path)
        assertEquals(
            """{"email":"mariana.costa@caminhosdobem.example","senha":"Frio@2026"}""",
            pedido.body.readUtf8(),
        )
        assertNull("login não manda token", pedido.getHeader("Authorization"))
    }

    @Test
    fun `senha errada vira credenciais invalidas e nao guarda nada`() = runTest {
        servidor.enqueue(json("""{"title":"Credenciais invalidas","status":401,"detail":"E-mail ou senha invalidos."}""", 401))

        val resultado = repositorio.entrar("mariana.costa@caminhosdobem.example", "errada1")

        assertEquals(Resultado.Falha(ErroApp.CredenciaisInvalidas), resultado)
        assertNull(sessao.atual)
        assertEquals("401 no login não é sessão expirada", 0, sessao.expiracoes)
    }

    @Test
    fun `api desligada vira sem conexao`() = runTest {
        servidor.shutdown()

        val resultado = repositorio.entrar("mariana.costa@caminhosdobem.example", "Frio@2026")

        assertEquals(Resultado.Falha(ErroApp.SemConexao), resultado)
    }

    @Test
    fun `muitas tentativas vira 429`() = runTest {
        servidor.enqueue(json("""{"status":429}""", 429))

        assertEquals(Resultado.Falha(ErroApp.MuitasTentativas), repositorio.entrar("a@b.co", "Frio@2026"))
    }

    @Test
    fun `sair limpa a sessao`() = runTest {
        sessao.atual = Sessao("t", Instant.parse("2026-11-15T20:00:00Z"), "Ana", Papel.VOLUNTARIO, 1, "Banco")

        repositorio.sair()

        assertNull(sessao.atual)
        assertTrue(sessao.expiracoes == 0)
    }
}
