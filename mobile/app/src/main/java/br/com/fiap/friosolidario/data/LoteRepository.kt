package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
import java.time.LocalDate

class LoteRepository(private val api: FrioApi, private val aoExpirar: suspend () -> Unit) {

    /** Lotes da instituição do token, já ordenados pela validade (o que vence primeiro). */
    suspend fun listar(): Resultado<List<LoteDto>> = chamar(aoExpirar) { api.lotes() }

    /** O lote nasce `DISPONIVEL` na instituição do token; a API não aceita outro status aqui. */
    suspend fun criar(descricao: String, categoria: String, kg: Double, validade: LocalDate): Resultado<LoteDto> =
        chamar(aoExpirar) { api.criarLote(LoteRequest(descricao.trim(), categoria.trim(), kg, validade)) }

    /** Saída do estoque: `DISTRIBUIDO` (kg salvos) ou `PERDIDO` (kg desperdiçados). */
    suspend fun registrarSaida(id: Long, status: StatusLote): Resultado<LoteDto> =
        chamar(aoExpirar) { api.retirarLote(id, RetiradaDeLoteRequest(status)) }
}
