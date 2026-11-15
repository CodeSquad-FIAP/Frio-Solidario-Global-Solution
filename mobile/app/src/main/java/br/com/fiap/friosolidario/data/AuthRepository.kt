package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ArmazemDeSessao
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.chamar

class AuthRepository(private val api: FrioApi, private val sessao: ArmazemDeSessao) {

    /** No login, 401 é senha errada (e não token vencido): não há sessão para limpar. */
    suspend fun entrar(email: String, senha: String): Resultado<Sessao> {
        val resultado = chamar { api.login(LoginRequest(email.trim(), senha)) }
        return when (resultado) {
            is Resultado.Falha ->
                if (resultado.erro == ErroApp.SessaoExpirada) Resultado.Falha(ErroApp.CredenciaisInvalidas) else resultado

            is Resultado.Ok -> {
                val nova = with(resultado.valor) { Sessao(token, expiraEm, nome, papel, instituicaoId, instituicao) }
                sessao.salvar(nova)
                Resultado.Ok(nova)
            }
        }
    }

    suspend fun sair() = sessao.limpar()
}
