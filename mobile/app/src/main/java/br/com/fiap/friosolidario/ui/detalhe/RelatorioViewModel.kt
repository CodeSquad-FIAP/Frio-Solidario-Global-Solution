package br.com.fiap.friosolidario.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.RelatorioRepository
import br.com.fiap.friosolidario.data.validarPeriodo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class OpcaoDePeriodo(val rotulo: String, val dias: Long?) {
    /** Mesmo padrão da API: hoje e os 7 dias anteriores. */
    SETE_DIAS("Últimos 7 dias", 7),
    TRINTA_DIAS("Últimos 30 dias", 30),
    ESCOLHER("Escolher as datas", null),
}

data class RelatorioEstado(
    val aberto: Boolean = false,
    val opcao: OpcaoDePeriodo = OpcaoDePeriodo.SETE_DIAS,
    val de: LocalDate = LocalDate.now().minusDays(7),
    val ate: LocalDate = LocalDate.now(),
    val baixando: Boolean = false,
    val erro: ErroApp? = null,
    /** PDF pronto no cache, para compartilhar ou imprimir. */
    val arquivo: File? = null,
) {
    val erroDoPeriodo: String? get() = validarPeriodo(de, ate)
}

class RelatorioViewModel(
    private val equipamentoId: Long,
    private val relatorios: RelatorioRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(RelatorioEstado())
    val estado: StateFlow<RelatorioEstado> = _estado.asStateFlow()

    /** Sempre abre no padrão (últimos 7 dias), contado a partir de hoje. */
    fun abrir() {
        val hoje = LocalDate.now()
        _estado.value = RelatorioEstado(aberto = true, de = hoje.minusDays(7), ate = hoje)
    }

    fun fechar() {
        if (_estado.value.baixando) return
        _estado.update { it.copy(aberto = false) }
    }

    fun escolherOpcao(opcao: OpcaoDePeriodo) = _estado.update {
        val hoje = LocalDate.now()
        it.copy(
            opcao = opcao,
            de = opcao.dias?.let(hoje::minusDays) ?: it.de,
            ate = if (opcao.dias != null) hoje else it.ate,
            erro = null,
            arquivo = null,
        )
    }

    fun mudarDe(data: LocalDate) = _estado.update { it.copy(de = data, erro = null, arquivo = null) }

    fun mudarAte(data: LocalDate) = _estado.update { it.copy(ate = data, erro = null, arquivo = null) }

    fun gerar() {
        val atual = _estado.value
        if (atual.baixando || atual.erroDoPeriodo != null) return

        _estado.update { it.copy(baixando = true, erro = null, arquivo = null) }
        viewModelScope.launch {
            val resultado = relatorios.baixar(equipamentoId, atual.de, atual.ate)
            _estado.update {
                when (resultado) {
                    is Resultado.Ok -> it.copy(baixando = false, arquivo = resultado.valor)
                    is Resultado.Falha -> it.copy(baixando = false, erro = resultado.erro)
                }
            }
        }
    }
}
