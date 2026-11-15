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
