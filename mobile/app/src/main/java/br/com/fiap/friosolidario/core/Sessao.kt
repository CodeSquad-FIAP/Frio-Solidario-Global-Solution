package br.com.fiap.friosolidario.core

import java.time.Instant

enum class Papel { ADMIN, VOLUNTARIO }

/** O que o login devolve e o app guarda até o logout ou até o token vencer (8 h). */
data class Sessao(
    val token: String,
    val expiraEm: Instant,
    val nome: String,
    val papel: Papel,
    val instituicaoId: Long,
    val instituicao: String,
)

/** Só ADMIN cadastra equipamento e lote; a API devolve 403 para VOLUNTARIO. */
val Sessao.ehAdmin: Boolean get() = papel == Papel.ADMIN

fun Sessao.vencida(agora: Instant = Instant.now()): Boolean = !expiraEm.isAfter(agora)

/** Onde a sessão fica guardada. Interface para os repositórios não dependerem do Android. */
interface ArmazemDeSessao {
    /** Token para o cabeçalho `Authorization`; `null` sem login. Chamado fora de corrotina. */
    fun tokenAtual(): String?

    suspend fun salvar(sessao: Sessao)

    /** [expirou] = a API recusou o token (401): o login mostra o aviso de sessão expirada. */
    suspend fun limpar(expirou: Boolean = false)
}
