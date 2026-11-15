package br.com.fiap.friosolidario.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.friosolidario.core.ErroApp
import br.com.fiap.friosolidario.core.Resultado
import br.com.fiap.friosolidario.data.AcaoTomada
import br.com.fiap.friosolidario.data.AlertaDto
import br.com.fiap.friosolidario.data.AlertaRepository
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AbaDeAlertas(val rotulo: String) { ABERTOS("Abertos"), HISTORICO("Histórico") }

data class Listas(val abertos: List<AlertaDto>, val historico: List<AlertaDto>)

/** Diálogo de resolução: qual alerta, a ação marcada e o andamento do envio. */
data class Resolucao(
    val alerta: AlertaDto,
    val acao: AcaoTomada? = null,
    val enviando: Boolean = false,
    val erro: ErroApp? = null,
)

data class AlertasEstado(
    val aba: AbaDeAlertas = AbaDeAlertas.ABERTOS,
    val tela: EstadoTela<Listas> = EstadoTela.Carregando,
    val atualizando: Boolean = false,
    val erroAoAtualizar: ErroApp? = null,
    val resolucao: Resolucao? = null,
)

class AlertasViewModel(private val alertas: AlertaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AlertasEstado())
    val estado: StateFlow<AlertasEstado> = _estado.asStateFlow()

    // Avisos curtos ("Alerta resolvido: transferi o lote"). Canal, e não campo do estado: a tela
    // mostra cada um até o fim, sem que apagar o campo cancele o aviso que acabou de abrir.
    private val _avisos = Channel<String>(Channel.BUFFERED)
    val avisos: Flow<String> = _avisos.receiveAsFlow()

    private var carga: Job? = null

    fun mudarAba(aba: AbaDeAlertas) = _estado.update { it.copy(aba = aba) }

    fun carregar() {
        _estado.update { it.copy(tela = EstadoTela.Carregando, erroAoAtualizar = null) }
        buscar()
    }

    fun puxarParaAtualizar() {
        _estado.update { it.copy(atualizando = true) }
        buscar()
    }

    /** A cada 10 s com a tela aberta: o alerta do sensor aparece sem ninguém tocar em nada. */
    fun atualizarEmSilencio() {
        if (carga?.isActive == true || _estado.value.resolucao?.enviando == true) return
        if (_estado.value.tela is EstadoTela.Carregando) carregar() else buscar()
    }

    fun abrirResolucao(alerta: AlertaDto) = _estado.update { it.copy(resolucao = Resolucao(alerta)) }

    fun escolherAcao(acao: AcaoTomada) =
        _estado.update { it.copy(resolucao = it.resolucao?.copy(acao = acao, erro = null)) }

    fun fecharResolucao() {
        if (_estado.value.resolucao?.enviando == true) return
        _estado.update { it.copy(resolucao = null) }
    }

    fun confirmarResolucao() {
        val resolucao = _estado.value.resolucao ?: return
        val acao = resolucao.acao ?: return
        if (resolucao.enviando) return

        _estado.update { it.copy(resolucao = resolucao.copy(enviando = true, erro = null)) }
        // Uma consulta que saiu antes da resolução traria o alerta ainda aberto e o devolveria à fila.
        carga?.cancel()
        viewModelScope.launch {
            when (val resultado = alertas.resolver(resolucao.alerta.id, acao)) {
                is Resultado.Ok -> {
                    // Sai da fila e entra no histórico na hora, com o que a API devolveu.
                    _estado.update { atual ->
                        atual.copy(
                            resolucao = null,
                            tela = atual.tela.comListas { listas ->
                                Listas(
                                    abertos = listas.abertos.filterNot { it.id == resultado.valor.id },
                                    historico = listOf(resultado.valor) + listas.historico.filterNot { it.id == resultado.valor.id },
                                )
                            },
                        )
                    }
                    _avisos.send("Alerta resolvido: ${acao.rotulo.lowercase()}")
                }

                is Resultado.Falha ->
                    if (resultado.erro is ErroApp.Conflito || resultado.erro == ErroApp.NaoEncontrado) {
                        // Outra pessoa resolveu antes: fecha o diálogo e mostra a fila como está agora.
                        _estado.update { it.copy(resolucao = null) }
                        _avisos.send("Este alerta já tinha sido resolvido.")
                        buscar()
                    } else {
                        _estado.update { it.copy(resolucao = it.resolucao?.copy(enviando = false, erro = resultado.erro)) }
                    }
            }
        }
    }

    private fun buscar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            val abertos = async { alertas.abertos() }
            val historico = async { alertas.historico() }
            val (a, h) = abertos.await() to historico.await()

            _estado.update { atual ->
                val erro = (a as? Resultado.Falha)?.erro ?: (h as? Resultado.Falha)?.erro
                when {
                    a is Resultado.Ok && h is Resultado.Ok ->
                        atual.copy(tela = EstadoTela.Conteudo(Listas(a.valor, h.valor)), atualizando = false, erroAoAtualizar = null)

                    atual.tela is EstadoTela.Conteudo -> atual.copy(atualizando = false, erroAoAtualizar = erro)

                    else -> atual.copy(tela = EstadoTela.Erro(erro ?: ErroApp.Inesperado), atualizando = false)
                }
            }
        }
    }

    private fun EstadoTela<Listas>.comListas(transformar: (Listas) -> Listas): EstadoTela<Listas> =
        if (this is EstadoTela.Conteudo) EstadoTela.Conteudo(transformar(valor)) else this
}

/** Quantos alertas estão abertos, para o sino do painel. `null` enquanto não se sabe. */
class ContadorDeAlertasViewModel(private val alertas: AlertaRepository) : ViewModel() {
    private val _abertos = MutableStateFlow<Int?>(null)
    val abertos: StateFlow<Int?> = _abertos.asStateFlow()

    private var carga: Job? = null

    fun atualizar() {
        if (carga?.isActive == true) return
        carga = viewModelScope.launch {
            // Em caso de erro fica o último número conhecido; o painel já mostra a falta de conexão.
            (alertas.abertos() as? Resultado.Ok)?.let { _abertos.value = it.valor.size }
        }
    }
}
