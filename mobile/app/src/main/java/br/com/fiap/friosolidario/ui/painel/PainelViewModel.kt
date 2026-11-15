package br.com.fiap.friosolidario.ui.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.AuthRepository
import br.com.fiap.friosolidario.data.EquipamentoNoPainel
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.dominio.semaforo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PainelEstado(
    val tela: EstadoTela<List<EquipamentoNoPainel>> = EstadoTela.Carregando,
    /** Puxar-para-atualizar em andamento. */
    val atualizando: Boolean = false,
    /** A atualização falhou, no todo ou na leitura de algum equipamento; o que já se sabia continua na tela. */
    val erroAoAtualizar: ErroApp? = null,
)

class PainelViewModel(
    private val equipamentos: EquipamentoRepository,
    private val auth: AuthRepository,
    private val agora: () -> java.time.Instant = java.time.Instant::now,
) : ViewModel() {

    private val _estado = MutableStateFlow(PainelEstado())
    val estado: StateFlow<PainelEstado> = _estado.asStateFlow()

    private var carga: Job? = null

    /** Primeira carga e "tentar de novo": mostra o indicador de tela cheia. */
    fun carregar() {
        _estado.update { it.copy(tela = EstadoTela.Carregando, erroAoAtualizar = null) }
        buscar()
    }

    fun puxarParaAtualizar() {
        _estado.update { it.copy(atualizando = true) }
        buscar()
    }

    /** Atualização periódica e ao voltar para a tela: não mexe em indicador nenhum. */
    fun atualizarEmSilencio() {
        if (carga?.isActive == true) return
        if (_estado.value.tela is EstadoTela.Carregando) carregar() else buscar()
    }

    fun sair() {
        viewModelScope.launch { auth.sair() }
    }

    private fun buscar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            val resultado = equipamentos.painel()
            _estado.update { atual ->
                when (resultado) {
                    is Resultado.Ok -> {
                        val itens = comLeiturasAnteriores(resultado.valor, atual.tela)
                        PainelEstado(
                            tela = EstadoTela.Conteudo(itens.sortedWith(MAIS_GRAVE_PRIMEIRO)),
                            erroAoAtualizar = itens.firstNotNullOfOrNull { it.erroDaLeitura },
                        )
                    }

                    is Resultado.Falha ->
                        if (atual.tela is EstadoTela.Conteudo) {
                            atual.copy(atualizando = false, erroAoAtualizar = resultado.erro)
                        } else {
                            PainelEstado(tela = EstadoTela.Erro(resultado.erro))
                        }
                }
            }
        }
    }

    /**
     * Quando a leitura de um equipamento não veio, fica valendo a última que a tela já tinha. Trocar
     * por "sem leitura" esconderia um equipamento fora da faixa atrás de um "tudo dentro da faixa".
     */
    private fun comLeiturasAnteriores(
        novos: List<EquipamentoNoPainel>,
        telaAtual: EstadoTela<List<EquipamentoNoPainel>>,
    ): List<EquipamentoNoPainel> {
        val anteriores = (telaAtual as? EstadoTela.Conteudo)?.valor.orEmpty().associateBy { it.equipamento.id }
        return novos.map { novo ->
            val leituraAnterior = anteriores[novo.equipamento.id]?.ultimaLeitura
            if (novo.erroDaLeitura == null || leituraAnterior == null) {
                novo
            } else {
                novo.copy(
                    ultimaLeitura = leituraAnterior,
                    semaforo = semaforo(novo.equipamento, leituraAnterior, agora()),
                )
            }
        }
    }

    private companion object {
        /** O que precisa de ação aparece primeiro; dentro do mesmo estado, ordem alfabética. */
        val MAIS_GRAVE_PRIMEIRO = compareBy<EquipamentoNoPainel>(
            {
                when (it.semaforo) {
                    Semaforo.VERMELHO -> 0
                    Semaforo.AMARELO -> 1
                    Semaforo.SEM_SINAL -> 2
                    Semaforo.SEM_LEITURA -> 3
                    Semaforo.VERDE -> 4
                }
            },
            { it.equipamento.nome.lowercase() },
        )
    }
}
