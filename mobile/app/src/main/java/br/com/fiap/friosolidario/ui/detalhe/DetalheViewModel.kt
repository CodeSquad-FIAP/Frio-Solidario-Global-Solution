package br.com.fiap.friosolidario.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.data.LeituraDto
import br.com.fiap.friosolidario.data.Periodo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/** Série de um período, com a janela de tempo que ela cobre (o eixo do gráfico). */
data class Serie(val periodo: Periodo, val leituras: List<LeituraDto>, val inicio: Instant, val fim: Instant)

data class DetalheEstado(
    val equipamento: EstadoTela<EquipamentoDto> = EstadoTela.Carregando,
    val periodo: Periodo = Periodo.H24,
    val serie: EstadoTela<Serie> = EstadoTela.Carregando,
    /** Para o cabeçalho; pode ser mais antiga que o período escolhido. */
    val ultimaLeitura: LeituraDto? = null,
    val erroAoAtualizar: ErroApp? = null,
)

class DetalheViewModel(
    private val equipamentoId: Long,
    private val equipamentos: EquipamentoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalheEstado())
    val estado: StateFlow<DetalheEstado> = _estado.asStateFlow()

    private var cargaDaSerie: Job? = null

    init {
        carregar()
    }

    /** Equipamento, série e última leitura: primeira abertura e "tentar de novo". */
    fun carregar() {
        _estado.update { it.copy(equipamento = EstadoTela.Carregando, serie = EstadoTela.Carregando, erroAoAtualizar = null) }
        viewModelScope.launch {
            val periodo = _estado.value.periodo
            val equipamento = async { equipamentos.equipamento(equipamentoId) }
            val serie = async { buscarSerie(periodo) }
            val ultima = async { equipamentos.ultimaLeitura(equipamentoId) }

            when (val resultado = equipamento.await()) {
                is Resultado.Falha -> _estado.update { it.copy(equipamento = EstadoTela.Erro(resultado.erro)) }
                is Resultado.Ok -> {
                    val telaDaSerie = serie.await()
                    _estado.update {
                        it.copy(
                            equipamento = EstadoTela.Conteudo(resultado.valor),
                            serie = if (it.periodo == periodo) telaDaSerie else it.serie,
                            ultimaLeitura = (ultima.await() as? Resultado.Ok)?.valor,
                        )
                    }
                }
            }
        }
    }

    /** Troca só a série: cabeçalho e o resto da tela ficam como estão. */
    fun mudarPeriodo(periodo: Periodo) {
        if (periodo == _estado.value.periodo && _estado.value.serie !is EstadoTela.Erro) return
        _estado.update { it.copy(periodo = periodo, serie = EstadoTela.Carregando) }
        recarregarSerie(periodo, silencioso = false)
    }

    fun tentarSerieDeNovo() = mudarPeriodo(_estado.value.periodo)

    /** Atualização periódica: leitura nova do sensor entra no gráfico sem a tela piscar. */
    fun atualizarEmSilencio() {
        val atual = _estado.value
        if (atual.equipamento !is EstadoTela.Conteudo || atual.serie !is EstadoTela.Conteudo) return
        if (cargaDaSerie?.isActive == true) return
        recarregarSerie(atual.periodo, silencioso = true)
    }

    private fun recarregarSerie(periodo: Periodo, silencioso: Boolean) {
        cargaDaSerie?.cancel()
        cargaDaSerie = viewModelScope.launch {
            val nova = buscarSerie(periodo)
            _estado.update { atual ->
                when {
                    atual.periodo != periodo -> atual
                    nova is EstadoTela.Erro && silencioso -> atual.copy(erroAoAtualizar = nova.erro)
                    nova is EstadoTela.Conteudo -> atual.copy(
                        serie = nova,
                        erroAoAtualizar = null,
                        ultimaLeitura = nova.valor.leituras.lastOrNull() ?: atual.ultimaLeitura,
                    )
                    else -> atual.copy(serie = nova)
                }
            }
        }
    }

    private suspend fun buscarSerie(periodo: Periodo): EstadoTela<Serie> =
        when (val resultado = equipamentos.leituras(equipamentoId, periodo)) {
            is Resultado.Falha -> EstadoTela.Erro(resultado.erro)
            is Resultado.Ok -> {
                val fim = Instant.now()
                val janela = if (periodo == Periodo.H24) Duration.ofHours(24) else Duration.ofDays(7)
                EstadoTela.Conteudo(Serie(periodo, resultado.valor, fim.minus(janela), fim))
            }
        }
}
