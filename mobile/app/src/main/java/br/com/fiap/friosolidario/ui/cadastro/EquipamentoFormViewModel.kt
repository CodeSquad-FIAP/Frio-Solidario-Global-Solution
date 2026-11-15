package br.com.fiap.friosolidario.ui.cadastro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.EquipamentoRepository
import br.com.fiap.friosolidario.data.EquipamentoRequest
import br.com.fiap.friosolidario.data.TipoEquipamento
import br.com.fiap.friosolidario.dominio.Campo
import br.com.fiap.friosolidario.dominio.paraDecimal
import br.com.fiap.friosolidario.dominio.validarEquipamento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EquipamentoFormEstado(
    val nome: String = "",
    val tipo: TipoEquipamento = TipoEquipamento.GELADEIRA,
    val tempMin: String = "",
    val tempMax: String = "",
    val umidMax: String = "",
    val erros: Map<Campo, String> = emptyMap(),
    val enviando: Boolean = false,
    val erro: ErroApp? = null,
    val salvo: Boolean = false,
)

class EquipamentoFormViewModel(
    private val instituicaoId: Long,
    private val equipamentos: EquipamentoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EquipamentoFormEstado())
    val estado: StateFlow<EquipamentoFormEstado> = _estado.asStateFlow()

    fun mudarNome(valor: String) = mudar(Campo.NOME) { it.copy(nome = valor) }
    fun mudarTipo(valor: TipoEquipamento) = _estado.update { it.copy(tipo = valor, erro = null) }
    fun mudarTempMin(valor: String) = mudar(Campo.TEMP_MIN, Campo.TEMP_MAX) { it.copy(tempMin = valor) }
    fun mudarTempMax(valor: String) = mudar(Campo.TEMP_MAX) { it.copy(tempMax = valor) }
    fun mudarUmidMax(valor: String) = mudar(Campo.UMID_MAX) { it.copy(umidMax = valor) }

    /** Mexeu no campo, some o erro dele (e o da API): a conferência roda de novo ao enviar. */
    private fun mudar(vararg campos: Campo, alterar: (EquipamentoFormEstado) -> EquipamentoFormEstado) =
        _estado.update { alterar(it).copy(erros = it.erros - campos.toSet(), erro = null) }

    fun salvar() {
        val atual = _estado.value
        if (atual.enviando) return

        val erros = validarEquipamento(atual.nome, atual.tempMin, atual.tempMax, atual.umidMax)
        if (erros.isNotEmpty()) {
            _estado.update { it.copy(erros = erros, erro = null) }
            return
        }

        _estado.update { it.copy(enviando = true, erros = emptyMap(), erro = null) }
        viewModelScope.launch {
            val pedido = EquipamentoRequest(
                instituicaoId = instituicaoId,
                nome = atual.nome.trim(),
                tipo = atual.tipo,
                tempMin = paraDecimal(atual.tempMin)!!,
                tempMax = paraDecimal(atual.tempMax)!!,
                umidMax = paraDecimal(atual.umidMax)!!,
            )
            val resultado = equipamentos.criar(pedido)
            _estado.update {
                when (resultado) {
                    is Resultado.Ok -> it.copy(enviando = false, salvo = true)
                    is Resultado.Falha -> it.copy(enviando = false, erro = resultado.erro)
                }
            }
        }
    }
}
