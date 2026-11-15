package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
import br.com.fiap.friosolidario.core.mapear

class AlertaRepository(private val api: FrioApi, private val aoExpirar: suspend () -> Unit) {

    /** Fila de trabalho. A API já ordena: mais grave primeiro e, na mesma severidade, o mais recente. */
    suspend fun abertos(): Resultado<List<AlertaDto>> = chamar(aoExpirar) { api.alertas(StatusAlerta.ABERTO) }

    /** Registro do que já foi tratado, da resolução mais recente para a mais antiga. */
    suspend fun historico(): Resultado<List<AlertaDto>> =
        chamar(aoExpirar) { api.alertas(StatusAlerta.RESOLVIDO) }
            .mapear { lista -> lista.sortedByDescending { it.resolvidoEm ?: it.criadoEm } }

    /** Fecha o alerta gravando quem resolveu, quando e o que fez. Repetir devolve 409. */
    suspend fun resolver(id: Long, acao: AcaoTomada): Resultado<AlertaDto> =
        chamar(aoExpirar) { api.resolverAlerta(id, ResolverAlertaRequest(acao)) }
}
