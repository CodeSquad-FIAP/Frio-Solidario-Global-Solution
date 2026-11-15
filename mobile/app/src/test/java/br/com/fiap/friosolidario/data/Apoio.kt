package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ArmazemDeSessao
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.criarApi
import br.com.fiap.friosolidario.core.criarJson
import br.com.fiap.friosolidario.core.criarOkHttp
import okhttp3.mockwebserver.MockResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

fun equipamentoJson(
    id: Long,
    nome: String = "Equipamento $id",
    min: Double = 0.0,
    max: Double = 4.0,
    ultimaLeitura: String? = null,
): String =
    """{"id":$id,"instituicaoId":1,"instituicao":"Banco","nome":"$nome","tipo":"CAMARA",
        "tempMin":$min,"tempMax":$max,"umidMax":85,"ativo":true${ultimaLeitura?.let { "," } ?: ""}${
        ultimaLeitura?.let { "\"ultimaLeitura\":$it" } ?: ""
    }}"""

fun leituraJson(id: Long, equipamentoId: Long, temperatura: Double, em: String): String =
    """{"id":$id,"equipamentoId":$equipamentoId,"temperatura":$temperatura,"umidade":70,
        "portaAberta":false,"registradaEm":"$em","origem":"SENSOR"}"""

/** Espera uma condição que depende de resposta de rede (thread do OkHttp), com limite de tempo. */
fun esperar(limiteMs: Long = 5_000, condicao: () -> Boolean) {
    val fim = System.currentTimeMillis() + limiteMs
    while (!condicao() && System.currentTimeMillis() < fim) Thread.sleep(10)
    check(condicao()) { "a condição não foi atendida em $limiteMs ms" }
}

/** Respostas por caminho que o teste pode trocar no meio do caminho. */
class RotasMutaveis(private val rotas: MutableMap<String, () -> MockResponse> = mutableMapOf()) :
    okhttp3.mockwebserver.Dispatcher() {
    val pedidos = java.util.concurrent.CopyOnWriteArrayList<String>()

    operator fun set(caminho: String, resposta: () -> MockResponse) { rotas[caminho] = resposta }

    override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
        pedidos += "${request.method} ${request.path}"
        return rotas[request.path]?.invoke() ?: MockResponse().setResponseCode(404)
    }
}

/** Regra do JUnit que troca o Dispatchers.Main, exigido pelo viewModelScope fora do Android. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainDeTeste : org.junit.rules.TestWatcher() {
    override fun starting(description: org.junit.runner.Description) =
        Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
    override fun finished(description: org.junit.runner.Description) = Dispatchers.resetMain()
}
