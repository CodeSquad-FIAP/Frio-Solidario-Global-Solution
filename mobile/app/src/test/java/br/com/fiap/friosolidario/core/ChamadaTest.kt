package br.com.fiap.friosolidario.core

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChamadaTest {

    private fun http(status: Int, corpo: String = "", tipo: String = "application/problem+json"): HttpException =
        HttpException(Response.error<Any>(status, corpo.toResponseBody(tipo.toMediaType())))

    private suspend fun erroDe(excecao: Exception): ErroApp {
        val resultado = chamar<Unit> { throw excecao }
        return (resultado as Resultado.Falha).erro
    }

    @Test
    fun `sucesso devolve o valor`() = runTest {
        assertEquals(Resultado.Ok(42), chamar { 42 })
    }

    @Test
    fun `timeout vira sem conexao`() = runTest {
        assertEquals(ErroApp.SemConexao, erroDe(SocketTimeoutException("timeout")))
    }

    @Test
    fun `api desligada vira sem conexao`() = runTest {
        assertEquals(ErroApp.SemConexao, erroDe(ConnectException("Connection refused")))
        assertEquals(ErroApp.SemConexao, erroDe(UnknownHostException("host")))
    }

    @Test
    fun `400 de regra de negocio mostra o detail da api`() = runTest {
        val corpo = """{"title":"Regra de negocio violada","status":400,"detail":"temp_min deve ser menor que temp_max."}"""
        assertEquals(ErroApp.Validacao("temp_min deve ser menor que temp_max."), erroDe(http(400, corpo)))
    }

    @Test
    fun `400 de validacao mostra a primeira mensagem de errors`() = runTest {
        val corpo = """{"title":"One or more validation errors occurred.","status":400,
            "errors":{"UmidMax":["Umidade maxima deve ficar entre 0,01 e 100."],"Nome":["outro"]}}"""
        assertEquals(
            ErroApp.Validacao("Umidade maxima deve ficar entre 0,01 e 100."),
            erroDe(http(400, corpo)),
        )
    }

    @Test
    fun `400 com corpo vazio usa mensagem generica`() = runTest {
        assertEquals(ErroApp.Validacao("Dados inválidos. Confira os campos e tente de novo."), erroDe(http(400)))
    }

    @Test
    fun `401 vira sessao expirada e avisa uma vez`() = runTest {
        var avisos = 0
        val resultado = chamar<Unit>(aoExpirar = { avisos++ }) { throw http(401) }
        assertEquals(ErroApp.SessaoExpirada, (resultado as Resultado.Falha).erro)
        assertEquals(1, avisos)
    }

    @Test
    fun `outros erros nao avisam expiracao`() = runTest {
        var avisos = 0
        chamar<Unit>(aoExpirar = { avisos++ }) { throw http(500) }
        chamar<Unit>(aoExpirar = { avisos++ }) { throw SocketTimeoutException() }
        assertEquals(0, avisos)
    }

    @Test
    fun `403 vira sem permissao`() = runTest {
        assertEquals(ErroApp.SemPermissao, erroDe(http(403)))
    }

    @Test
    fun `404 vira nao encontrado`() = runTest {
        assertEquals(ErroApp.NaoEncontrado, erroDe(http(404)))
    }

    @Test
    fun `409 mostra o detail da api`() = runTest {
        val corpo = """{"status":409,"detail":"Alerta 2 ja foi resolvido."}"""
        assertEquals(ErroApp.Conflito("Alerta 2 ja foi resolvido."), erroDe(http(409, corpo)))
    }

    @Test
    fun `429 vira muitas tentativas`() = runTest {
        assertEquals(ErroApp.MuitasTentativas, erroDe(http(429)))
    }

    @Test
    fun `500 vira erro do servidor`() = runTest {
        assertEquals(ErroApp.Servidor, erroDe(http(500, """{"detail":"Consulte o log da API."}""")))
    }

    @Test
    fun `corpo que nao e ProblemDetails nao quebra`() = runTest {
        assertEquals(ErroApp.Servidor, erroDe(http(502, "<html><body>Bad Gateway</body></html>", "text/html")))
        assertEquals(
            ErroApp.Validacao("Dados inválidos. Confira os campos e tente de novo."),
            erroDe(http(400, "<html>", "text/html")),
        )
    }

    @Test
    fun `resposta que nao da para interpretar vira inesperado`() = runTest {
        assertEquals(ErroApp.Inesperado, erroDe(IllegalStateException("json torto")))
    }

    @Test
    fun `todo erro tem mensagem para a tela`() {
        val erros = listOf(
            ErroApp.SemConexao, ErroApp.SessaoExpirada, ErroApp.CredenciaisInvalidas, ErroApp.SemPermissao,
            ErroApp.NaoEncontrado, ErroApp.MuitasTentativas, ErroApp.Servidor, ErroApp.Inesperado,
        )
        assertTrue(erros.all { it.mensagem.isNotBlank() })
    }
}
