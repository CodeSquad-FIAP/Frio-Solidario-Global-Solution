package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class RelatorioRepositoryTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val servidor = MockWebServer()
    private lateinit var repositorio: RelatorioRepository

    private val de = LocalDate.of(2026, 11, 9)
    private val ate = LocalDate.of(2026, 11, 15)
    private val pdf = "%PDF-1.7 conteudo binario \u0000\u0001\u0002 fim".toByteArray(Charsets.ISO_8859_1)

    @Before
    fun preparar() {
        servidor.start()
        repositorio = RelatorioRepository(servidor.api(), aoExpirar = {}, pasta = { pasta.root })
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun respostaPdf(disposicao: String? = "attachment; filename=log-termico-1-2026-11-09-a-2026-11-15.pdf") =
        MockResponse().setHeader("Content-Type", "application/pdf").setBody(Buffer().write(pdf)).apply {
            if (disposicao != null) setHeader("Content-Disposition", disposicao)
        }

    @Test
    fun `baixa o pdf do periodo e grava os mesmos bytes`() = runTest {
        servidor.enqueue(respostaPdf())

        val arquivo = (repositorio.baixar(1, de, ate) as Resultado.Ok).valor

        assertEquals("/api/equipamentos/1/relatorio?de=2026-11-09&ate=2026-11-15", servidor.takeRequest().path)
        assertArrayEquals(pdf, arquivo.readBytes())
        assertEquals("log-termico-1-2026-11-09-a-2026-11-15.pdf", arquivo.name)
        assertEquals(pasta.root, arquivo.parentFile)
    }

    @Test
    fun `sem nome na resposta usa um nome com equipamento e periodo`() = runTest {
        servidor.enqueue(respostaPdf(disposicao = null))

        val arquivo = (repositorio.baixar(3, de, ate) as Resultado.Ok).valor

        assertEquals("relatorio-3-2026-11-09-a-2026-11-15.pdf", arquivo.name)
    }

    @Test
    fun `nome vindo do servidor nao escapa da pasta de relatorios`() = runTest {
        servidor.enqueue(respostaPdf(disposicao = """attachment; filename="../../fora.pdf""""))

        val arquivo = (repositorio.baixar(1, de, ate) as Resultado.Ok).valor

        assertEquals(pasta.root, arquivo.parentFile)
        assertTrue(arquivo.name.endsWith(".pdf"))
    }

    @Test
    fun `periodo invalido para a api devolve a frase dela`() = runTest {
        servidor.enqueue(json("""{"status":400,"detail":"O periodo maximo do relatorio e de 92 dias."}""", 400))

        assertEquals(
            Resultado.Falha(ErroApp.Validacao("O periodo maximo do relatorio e de 92 dias.")),
            repositorio.baixar(1, de, ate),
        )
        assertTrue("nenhum arquivo pela metade", pasta.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `api desligada vira sem conexao`() = runTest {
        servidor.shutdown()

        assertEquals(Resultado.Falha(ErroApp.SemConexao), repositorio.baixar(1, de, ate))
    }

    @Test
    fun `conexao que cai no meio do download nao deixa arquivo pela metade`() = runTest {
        servidor.enqueue(
            respostaPdf().setBody(Buffer().write(ByteArray(64 * 1024))).setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        assertEquals(Resultado.Falha(ErroApp.SemConexao), repositorio.baixar(1, de, ate))
        assertTrue(pasta.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `periodo confere a ordem e o limite de 92 dias da api`() {
        assertNull(validarPeriodo(de, ate))
        assertNull(validarPeriodo(de, de))
        assertNull(validarPeriodo(ate.minusDays(91), ate))
        assertEquals("A data inicial precisa ser anterior à final.", validarPeriodo(ate, de))
        assertEquals("O relatório cobre no máximo 92 dias por vez.", validarPeriodo(ate.minusDays(92), ate))
    }

    @Test
    fun `periodo invalido nem chama a rede`() = runTest {
        val resultado = repositorio.baixar(1, ate.minusDays(120), ate)

        assertEquals(Resultado.Falha(ErroApp.Validacao("O relatório cobre no máximo 92 dias por vez.")), resultado)
        assertEquals(0, servidor.requestCount)
    }
}
