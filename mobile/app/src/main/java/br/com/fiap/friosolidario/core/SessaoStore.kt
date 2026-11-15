package br.com.fiap.friosolidario.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

private val Context.dadosDaSessao by preferencesDataStore(name = "sessao")

sealed interface EstadoSessao {
    /** Ainda lendo o disco, logo depois de abrir o app. */
    data object Carregando : EstadoSessao
    data object Ausente : EstadoSessao
    data class Ativa(val sessao: Sessao) : EstadoSessao
}

/**
 * Guarda a sessão no DataStore para o voluntário não digitar a senha a cada abertura. A navegação
 * observa [estado]: virou [EstadoSessao.Ausente] (logout ou 401), a tela volta ao login.
 */
class SessaoStore(private val contexto: Context, escopo: CoroutineScope) : ArmazemDeSessao {

    private val _estado = MutableStateFlow<EstadoSessao>(EstadoSessao.Carregando)
    val estado: StateFlow<EstadoSessao> = _estado.asStateFlow()

    private val _expirou = MutableStateFlow(false)

    /** Verdadeiro quando a sessão caiu por 401; o login mostra o aviso uma vez. */
    val expirou: StateFlow<Boolean> = _expirou.asStateFlow()

    init {
        escopo.launch {
            val guardada = runCatching { ler() }.getOrNull()
            _estado.value = when {
                guardada == null -> EstadoSessao.Ausente
                guardada.vencida() -> {
                    _expirou.value = true
                    EstadoSessao.Ausente
                }
                else -> EstadoSessao.Ativa(guardada)
            }
        }
    }

    override fun tokenAtual(): String? = (_estado.value as? EstadoSessao.Ativa)?.sessao?.token

    override suspend fun salvar(sessao: Sessao) {
        contexto.dadosDaSessao.edit { dados ->
            dados[TOKEN] = sessao.token
            dados[EXPIRA_EM] = sessao.expiraEm.toEpochMilli()
            dados[NOME] = sessao.nome
            dados[PAPEL] = sessao.papel.name
            dados[INSTITUICAO_ID] = sessao.instituicaoId
            dados[INSTITUICAO] = sessao.instituicao
        }
        _expirou.value = false
        _estado.value = EstadoSessao.Ativa(sessao)
    }

    override suspend fun limpar(expirou: Boolean) {
        // A tela reage primeiro; o disco pode falhar sem prender o usuário logado.
        if (expirou && _estado.value is EstadoSessao.Ativa) _expirou.value = true
        _estado.value = EstadoSessao.Ausente
        runCatching { contexto.dadosDaSessao.edit { it.clear() } }
    }

    fun avisoDeExpiracaoVisto() {
        _expirou.value = false
    }

    private suspend fun ler(): Sessao? {
        val dados = contexto.dadosDaSessao.data.first()
        return Sessao(
            token = dados[TOKEN] ?: return null,
            expiraEm = Instant.ofEpochMilli(dados[EXPIRA_EM] ?: return null),
            nome = dados[NOME] ?: return null,
            papel = runCatching { Papel.valueOf(dados[PAPEL] ?: return null) }.getOrNull() ?: return null,
            instituicaoId = dados[INSTITUICAO_ID] ?: return null,
            instituicao = dados[INSTITUICAO] ?: "",
        )
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val EXPIRA_EM = longPreferencesKey("expira_em")
        val NOME = stringPreferencesKey("nome")
        val PAPEL = stringPreferencesKey("papel")
        val INSTITUICAO_ID = longPreferencesKey("instituicao_id")
        val INSTITUICAO = stringPreferencesKey("instituicao")
    }
}
