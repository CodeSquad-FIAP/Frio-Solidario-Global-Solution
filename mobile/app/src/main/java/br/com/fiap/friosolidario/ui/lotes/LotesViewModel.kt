package br.com.fiap.friosolidario.ui.lotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.LoteDto
import br.com.fiap.friosolidario.data.LoteRepository
import br.com.fiap.friosolidario.data.StatusLote
import br.com.fiap.friosolidario.dominio.Campo
import br.com.fiap.friosolidario.dominio.paraDecimal
import br.com.fiap.friosolidario.dominio.validarLote
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Confirmação da saída do estoque de um lote. */
data class Saida(val lote: LoteDto, val status: StatusLote, val enviando: Boolean = false, val erro: ErroApp? = null)

data class LotesEstado(
    val tela: EstadoTela<List<LoteDto>> = EstadoTela.Carregando,
    val atualizando: Boolean = false,
    val erroAoAtualizar: ErroApp? = null,
    val saida: Saida? = null,
)

class LotesViewModel(private val lotes: LoteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LotesEstado())
    val estado: StateFlow<LotesEstado> = _estado.asStateFlow()

    private val _avisos = Channel<String>(Channel.BUFFERED)
    val avisos: Flow<String> = _avisos.receiveAsFlow()

    private var carga: Job? = null

    fun carregar() {
        _estado.update { it.copy(tela = EstadoTela.Carregando, erroAoAtualizar = null) }
        buscar()
    }

    fun puxarParaAtualizar() {
        _estado.update { it.copy(atualizando = true) }
        buscar()
    }

    /** Ao voltar para a tela (por exemplo, depois do cadastro): o lote novo aparece na lista. */
    fun aoFicarVisivel() {
        if (carga?.isActive == true) return
        if (_estado.value.tela is EstadoTela.Carregando) carregar() else buscar()
    }

    fun pedirSaida(lote: LoteDto, status: StatusLote) = _estado.update { it.copy(saida = Saida(lote, status)) }

    fun cancelarSaida() {
        if (_estado.value.saida?.enviando == true) return
        _estado.update { it.copy(saida = null) }
    }

    fun confirmarSaida() {
        val saida = _estado.value.saida ?: return
        if (saida.enviando) return
        _estado.update { it.copy(saida = saida.copy(enviando = true, erro = null)) }
        // Uma listagem que saiu antes da retirada traria o lote ainda disponível.
        carga?.cancel()

        viewModelScope.launch {
            when (val resultado = lotes.registrarSaida(saida.lote.id, saida.status)) {
                is Resultado.Ok -> {
                    _estado.update { atual ->
                        val tela = atual.tela
                        atual.copy(
                            saida = null,
                            tela = if (tela is EstadoTela.Conteudo) {
                                EstadoTela.Conteudo(tela.valor.map { if (it.id == resultado.valor.id) resultado.valor else it })
                            } else {
                                tela
                            },
                        )
                    }
                    _avisos.send("Lote marcado como ${saida.status.rotulo.lowercase()}.")
                }

                is Resultado.Falha ->
                    if (resultado.erro is ErroApp.Conflito) {
                        // Outra pessoa já registrou a saída: mostra o estado atual.
                        _estado.update { it.copy(saida = null) }
                        _avisos.send("Este lote já tinha saído do estoque.")
                        buscar()
                    } else {
                        _estado.update { it.copy(saida = it.saida?.copy(enviando = false, erro = resultado.erro)) }
                    }
            }
        }
    }

    private fun buscar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            val resultado = lotes.listar()
            _estado.update { atual ->
                when (resultado) {
                    is Resultado.Ok -> atual.copy(tela = EstadoTela.Conteudo(resultado.valor), atualizando = false, erroAoAtualizar = null)
                    is Resultado.Falha ->
                        if (atual.tela is EstadoTela.Conteudo) {
                            atual.copy(atualizando = false, erroAoAtualizar = resultado.erro)
                        } else {
                            atual.copy(tela = EstadoTela.Erro(resultado.erro), atualizando = false)
                        }
                }
            }
        }
    }
}

data class LoteFormEstado(
    val descricao: String = "",
    val categoria: String = "",
    val kg: String = "",
    val validade: LocalDate? = null,
    /** Categorias já usadas nos lotes da instituição, vindas da API, como atalho de digitação. */
    val categoriasSugeridas: List<String> = emptyList(),
    val erros: Map<Campo, String> = emptyMap(),
    val enviando: Boolean = false,
    val erro: ErroApp? = null,
    val salvo: Boolean = false,
)

class LoteFormViewModel(private val lotes: LoteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoteFormEstado())
    val estado: StateFlow<LoteFormEstado> = _estado.asStateFlow()

    init {
        // Só um atalho: se a listagem falhar, o campo de categoria continua livre para digitar.
        viewModelScope.launch {
            val existentes = (lotes.listar() as? Resultado.Ok)?.valor ?: return@launch
            val categorias = existentes
                .map { it.categoria.trim().lowercase().replaceFirstChar(Char::titlecase) }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            _estado.update { it.copy(categoriasSugeridas = categorias) }
        }
    }

    fun mudarDescricao(valor: String) = mudar(Campo.DESCRICAO) { it.copy(descricao = valor) }
    fun mudarCategoria(valor: String) = mudar(Campo.CATEGORIA) { it.copy(categoria = valor) }
    fun mudarKg(valor: String) = mudar(Campo.KG) { it.copy(kg = valor) }
    fun mudarValidade(valor: LocalDate?) = mudar(Campo.VALIDADE) { it.copy(validade = valor) }

    private fun mudar(campo: Campo, alterar: (LoteFormEstado) -> LoteFormEstado) =
        _estado.update { alterar(it).copy(erros = it.erros - campo, erro = null) }

    fun salvar() {
        val atual = _estado.value
        if (atual.enviando) return

        val erros = validarLote(atual.descricao, atual.categoria, atual.kg, atual.validade)
        if (erros.isNotEmpty()) {
            _estado.update { it.copy(erros = erros, erro = null) }
            return
        }

        _estado.update { it.copy(enviando = true, erros = emptyMap(), erro = null) }
        viewModelScope.launch {
            val resultado = lotes.criar(atual.descricao, atual.categoria, paraDecimal(atual.kg)!!, atual.validade!!)
            _estado.update {
                when (resultado) {
                    is Resultado.Ok -> it.copy(enviando = false, salvo = true)
                    is Resultado.Falha -> it.copy(enviando = false, erro = resultado.erro)
                }
            }
        }
    }
}
