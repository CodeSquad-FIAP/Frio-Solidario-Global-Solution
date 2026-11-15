package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
import br.com.fiap.friosolidario.core.mapear
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.dominio.semaforo

enum class Periodo(val api: String, val rotulo: String) {
    H24("24h", "24 horas"),
    D7("7d", "7 dias"),
}

data class EquipamentoNoPainel(
    val equipamento: EquipamentoDto,
    val ultimaLeitura: LeituraDto?,
    val semaforo: Semaforo,
)

class EquipamentoRepository(
    private val api: FrioApi,
    private val aoExpirar: suspend () -> Unit,
    private val agora: () -> java.time.Instant = java.time.Instant::now,
) {

    /**
     * Equipamentos ativos com a última leitura de cada um. A própria listagem já traz a leitura
     * (campo `ultimaLeitura`), então o painel inteiro sai em uma requisição — antes era uma por
     * equipamento, o que ficava caro com a atualização automática de 15 s.
     */
    suspend fun painel(): Resultado<List<EquipamentoNoPainel>> =
        chamar(aoExpirar) { api.equipamentos(ativo = true) }.mapear { lista ->
            lista.map { equipamento ->
                EquipamentoNoPainel(
                    equipamento = equipamento,
                    ultimaLeitura = equipamento.ultimaLeitura,
                    semaforo = semaforo(equipamento, equipamento.ultimaLeitura, agora()),
                )
            }
        }

    suspend fun equipamento(id: Long): Resultado<EquipamentoDto> = chamar(aoExpirar) { api.equipamento(id) }

    /** Série do gráfico, em ordem cronológica. */
    suspend fun leituras(equipamentoId: Long, periodo: Periodo): Resultado<List<LeituraDto>> =
        chamar(aoExpirar) { api.leituras(equipamentoId, periodo.api) }

    suspend fun criar(pedido: EquipamentoRequest): Resultado<EquipamentoDto> =
        chamar(aoExpirar) { api.criarEquipamento(pedido) }
}
