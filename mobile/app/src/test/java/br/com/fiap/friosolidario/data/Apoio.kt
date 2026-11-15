package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ArmazemDeSessao
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.criarApi
import br.com.fiap.friosolidario.core.criarJson
import br.com.fiap.friosolidario.core.criarOkHttp
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** Sessão guardada em memória, no lugar do DataStore, para os testes de repositório. */
class SessaoEmMemoria(inicial: Sessao? = null) : ArmazemDeSessao {
    var atual: Sessao? = inicial
    var expiracoes = 0

    override fun tokenAtual(): String? = atual?.token
    override suspend fun salvar(sessao: Sessao) { atual = sessao }
    override suspend fun limpar(expirou: Boolean) {
        atual = null
        if (expirou) expiracoes++
    }
}

/** API de verdade (Retrofit + OkHttp + JSON do app) apontada para o MockWebServer. */
fun MockWebServer.api(sessao: ArmazemDeSessao = SessaoEmMemoria(), timeoutSegundos: Long = 2): FrioApi =
    criarApi(url("/").toString(), criarOkHttp(timeoutSegundos) { sessao.tokenAtual() }, criarJson())

fun json(corpo: String, status: Int = 200): MockResponse =
    MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(corpo)

/** Responde por caminho (com query), para testes com chamadas em paralelo; o resto é 404. */
fun MockWebServer.rotas(vararg rotas: Pair<String, MockResponse>) {
    val mapa = rotas.toMap()
    dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
        override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse =
            mapa[request.path] ?: MockResponse().setResponseCode(404)
    }
}

fun equipamentoJson(id: Long, nome: String = "Equipamento $id", min: Double = 0.0, max: Double = 4.0): String =
    """{"id":$id,"instituicaoId":1,"instituicao":"Banco","nome":"$nome","tipo":"CAMARA",
        "tempMin":$min,"tempMax":$max,"umidMax":85,"ativo":true}"""

fun leituraJson(id: Long, equipamentoId: Long, temperatura: Double, em: String): String =
    """{"id":$id,"equipamentoId":$equipamentoId,"temperatura":$temperatura,"umidade":70,
        "portaAberta":false,"registradaEm":"$em","origem":"SENSOR"}"""
