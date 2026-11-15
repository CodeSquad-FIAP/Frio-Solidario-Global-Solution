package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
import br.com.fiap.friosolidario.core.mapear
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.dominio.semaforo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

enum class Periodo(val api: String, val rotulo: String) {
    H24("24h", "24 horas"),
    D7("7d", "7 dias"),
}

data class EquipamentoNoPainel(
    val equipamento: EquipamentoDto,
    val ultimaLeitura: LeituraDto?,
    val semaforo: Semaforo,
)

class EquipamentoRepository(private val api: FrioApi, private val aoExpirar: suspend () -> Unit) {

    /**
     * Equipamentos ativos com a última leitura de cada um. A lista da API não traz leitura, então
     * a série de cada equipamento é buscada em paralelo. Uma série que falha deixa só aquele
     * equipamento "sem leitura"; se nenhuma responde, o painel mostra o erro em vez de tudo cinza.
     */
    suspend fun painel(): Resultado<List<EquipamentoNoPainel>> {
        val equipamentos = when (val lista = chamar(aoExpirar) { api.equipamentos(ativo = true) }) {
            is Resultado.Falha -> return lista
            is Resultado.Ok -> lista.valor
        }
        if (equipamentos.isEmpty()) return Resultado.Ok(emptyList())

        val ultimas = coroutineScope {
            equipamentos.map { async { ultimaLeitura(it.id) } }.awaitAll()
        }

        val falhas = ultimas.filterIsInstance<Resultado.Falha>()
        falhas.firstOrNull { it.erro == ErroApp.SessaoExpirada }?.let { return it }
        if (falhas.size == equipamentos.size) return falhas.first()

        return Resultado.Ok(
            equipamentos.zip(ultimas) { equipamento, ultima ->
                val leitura = (ultima as? Resultado.Ok)?.valor
                EquipamentoNoPainel(equipamento, leitura, semaforo(equipamento, leitura))
            },
        )
    }

    /** A leitura mais recente: das últimas 24 h ou, se não houver, dos últimos 7 dias. */
    suspend fun ultimaLeitura(equipamentoId: Long): Resultado<LeituraDto?> {
        val dia = leituras(equipamentoId, Periodo.H24)
        if (dia is Resultado.Falha || (dia as Resultado.Ok).valor.isNotEmpty()) {
            return dia.mapear { it.lastOrNull() }
        }
        return leituras(equipamentoId, Periodo.D7).mapear { it.lastOrNull() }
    }

    suspend fun equipamento(id: Long): Resultado<EquipamentoDto> = chamar(aoExpirar) { api.equipamento(id) }

    /** Série do gráfico, em ordem cronológica. */
    suspend fun leituras(equipamentoId: Long, periodo: Periodo): Resultado<List<LeituraDto>> =
        chamar(aoExpirar) { api.leituras(equipamentoId, periodo.api) }

    suspend fun criar(pedido: EquipamentoRequest): Resultado<EquipamentoDto> =
        chamar(aoExpirar) { api.criarEquipamento(pedido) }
}
