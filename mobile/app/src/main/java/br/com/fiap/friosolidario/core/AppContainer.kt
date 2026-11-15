package br.com.fiap.friosolidario.core

import android.content.Context
import br.com.fiap.friosolidario.data.AlertaRepository
import br.com.fiap.friosolidario.data.AuthRepository
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.data.FrioApi
import br.com.fiap.friosolidario.data.LoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Monta as dependências uma vez, no início do app. Injeção manual: são poucas peças. */
class AppContainer(contexto: Context) {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val sessao = SessaoStore(contexto.applicationContext, escopo)

    private val api: FrioApi = criarApi(ApiConfig.BASE_URL, criarOkHttp { sessao.tokenAtual() }, criarJson())

    /** Repassado a todo repositório: 401 em qualquer tela limpa a sessão e volta ao login. */
    private val aoExpirar: suspend () -> Unit = { sessao.limpar(expirou = true) }

    val auth = AuthRepository(api, sessao)
    val equipamentos = EquipamentoRepository(api, aoExpirar)
    val alertas = AlertaRepository(api, aoExpirar)
    val lotes = LoteRepository(api, aoExpirar)
}
