package br.com.fiap.friosolidario.data

import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.core.chamar
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
    /** A busca da leitura falhou: "sem leitura" aqui é falta de resposta, não equipamento sem dado. */
    val erroDaLeitura: ErroApp? = null,
)

class EquipamentoRepository(
    private val api: FrioApi,
    private val aoExpirar: suspend () -> Unit,
    private val agora: () -> java.time.Instant = java.time.Instant::now,
) {

    /**
     * Equipamentos ativos com a última leitura de cada um. A lista da API não traz leitura, então
     * a última de cada equipamento é buscada em paralelo. Uma busca que falha deixa só aquele
     * equipamento sem leitura e marcado com o erro (quem mostra decide se mantém a leitura anterior);
     * se nenhuma responde, o painel mostra o erro em vez de tudo cinza.
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
                EquipamentoNoPainel(
                    equipamento,
                    leitura,
                    semaforo(equipamento, leitura, agora()),
                    erroDaLeitura = (ultima as? Resultado.Falha)?.erro,
                )
            },
        )
    }

    /**
     * A leitura mais recente, de quando for (`null` se o equipamento nunca enviou nenhuma). É uma
     * chamada pequena por equipamento; a série inteira só é baixada pela tela de detalhe.
     */
    suspend fun ultimaLeitura(equipamentoId: Long): Resultado<LeituraDto?> = chamar(aoExpirar) {
        val resposta = api.ultimaLeitura(equipamentoId)
        if (!resposta.isSuccessful) throw retrofit2.HttpException(resposta)
        resposta.body()
    }

    suspend fun equipamento(id: Long): Resultado<EquipamentoDto> = chamar(aoExpirar) { api.equipamento(id) }

    /** Série do gráfico, em ordem cronológica. */
    suspend fun leituras(equipamentoId: Long, periodo: Periodo): Resultado<List<LeituraDto>> =
        chamar(aoExpirar) { api.leituras(equipamentoId, periodo.api) }

    suspend fun criar(pedido: EquipamentoRequest): Resultado<EquipamentoDto> =
        chamar(aoExpirar) { api.criarEquipamento(pedido) }
}
