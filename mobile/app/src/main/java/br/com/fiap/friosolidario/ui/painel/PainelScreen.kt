package br.com.fiap.friosolidario.ui.painel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.diaEHora
import br.com.fiap.friosolidario.core.ehAdmin
import br.com.fiap.friosolidario.core.grau
import br.com.fiap.friosolidario.core.temperatura
import br.com.fiap.friosolidario.core.tempoDecorrido
import br.com.fiap.friosolidario.core.umidade
import br.com.fiap.friosolidario.data.EquipamentoNoPainel
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.ui.comum.BotaoDaLinha
import br.com.fiap.friosolidario.ui.comum.CabecalhoDaTela
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.EnquantoVisivel
import br.com.fiap.friosolidario.ui.comum.ErroComTentarDeNovo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import br.com.fiap.friosolidario.ui.comum.FaixaDeErro
import br.com.fiap.friosolidario.ui.comum.Fio
import br.com.fiap.friosolidario.ui.comum.LinhaSinal
import br.com.fiap.friosolidario.ui.comum.ListaVazia
import br.com.fiap.friosolidario.ui.comum.MargemDaTela
import br.com.fiap.friosolidario.ui.comum.agoraQueAnda
import br.com.fiap.friosolidario.ui.comum.corSuave
import br.com.fiap.friosolidario.ui.comum.cores
import br.com.fiap.friosolidario.ui.comum.vm
import br.com.fiap.friosolidario.ui.tema.FonteDeNumeros
import java.time.Instant

/** De quanto em quanto tempo o painel se atualiza sozinho enquanto está aberto. */
private const val INTERVALO_DO_PAINEL_MS = 15_000L

@Composable
fun PainelScreen(
    sessao: Sessao,
    aoAbrirEquipamento: (Long) -> Unit,
    aoAbrirAlertas: () -> Unit,
    aoNovoEquipamento: () -> Unit,
    aoAtualizar: () -> Unit,
) {
    val viewModel = vm { PainelViewModel(it.equipamentos, it.auth) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val agora by agoraQueAnda()

    // Também roda ao voltar do cadastro: o equipamento novo aparece sem reabrir o app.
    EnquantoVisivel(INTERVALO_DO_PAINEL_MS) {
        viewModel.atualizarEmSilencio()
        aoAtualizar()
    }

    val itens = (estado.tela as? EstadoTela.Conteudo)?.valor
    Scaffold(
        topBar = {
            CabecalhoDaTela(
                titulo = sessao.instituicao,
                subtitulo = itens?.takeIf { it.isNotEmpty() }?.let(::resumo)
                    ?: "${sessao.nome} · ${if (sessao.ehAdmin) "administrador" else "voluntário"}",
                modifier = Modifier.statusBarsPadding(),
                acoes = {
                    IconButton(onClick = viewModel::sair) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair")
                    }
                },
            )
        },
        floatingActionButton = {
            if (sessao.ehAdmin) {
                ExtendedFloatingActionButton(
                    onClick = aoNovoEquipamento,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Equipamento") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { margens ->
        PullToRefreshBox(
            isRefreshing = estado.atualizando,
            onRefresh = viewModel::puxarParaAtualizar,
            modifier = Modifier
                .fillMaxSize()
                .padding(margens),
        ) {
            when (val tela = estado.tela) {
                EstadoTela.Carregando -> Carregando()

                is EstadoTela.Erro -> ErroComTentarDeNovo(tela.erro, aoTentar = viewModel::carregar)

                is EstadoTela.Conteudo ->
                    if (tela.valor.isEmpty()) {
                        ListaVazia(
                            "Nenhum equipamento cadastrado",
                            detalhe = if (sessao.ehAdmin) {
                                "Cadastre a primeira geladeira, freezer ou câmara fria para começar a monitorar."
                            } else {
                                "Peça ao administrador da instituição para cadastrar os equipamentos."
                            },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = if (sessao.ehAdmin) 96.dp else 24.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            estado.erroAoAtualizar?.let { erro ->
                                item {
                                    FaixaDeErro(
                                        erro,
                                        aoTentar = viewModel::puxarParaAtualizar,
                                        modifier = Modifier.padding(horizontal = MargemDaTela, vertical = 12.dp),
                                    )
                                    Fio()
                                }
                            }
                            items(tela.valor, key = { it.equipamento.id }) { item ->
                                LinhaDoEquipamento(
                                    item = item,
                                    agora = agora,
                                    aoAbrir = { aoAbrirEquipamento(item.equipamento.id) },
                                    aoAbrirAlertas = aoAbrirAlertas,
                                )
                            }
                        }
                    }
            }
        }
    }
}

/** A resposta de três segundos, em uma linha: está tudo bem? */
private fun resumo(itens: List<EquipamentoNoPainel>): String {
    val vermelhos = itens.count { it.semaforo == Semaforo.VERMELHO }
    val amarelos = itens.count { it.semaforo == Semaforo.AMARELO }
    val semLeitura = itens.count { it.semaforo == Semaforo.SEM_LEITURA }
    val semSinal = itens.count { it.semaforo == Semaforo.SEM_SINAL }
    // A leitura não veio e não há anterior para mostrar: não dá para dizer que está tudo bem.
    val semResposta = itens.count { it.erroDaLeitura != null && it.ultimaLeitura == null }

    val partes = buildList {
        if (vermelhos > 0) add("$vermelhos fora da faixa")
        if (amarelos > 0) add("$amarelos em atenção")
        // Sensor parado nunca entra em "tudo dentro da faixa": não se sabe como ele está.
        if (semSinal > 0) add("$semSinal sem sinal")
        if (semResposta > 0) add("$semResposta sem resposta")
        if (semLeitura - semResposta > 0) add("${semLeitura - semResposta} sem leitura")
    }
    val total = if (itens.size == 1) "1 equipamento" else "${itens.size} equipamentos"
    return when {
        partes.isEmpty() -> "Tudo dentro da faixa · $total"
        else -> "${partes.joinToString(" · ")} · $total"
    }
}

/**
 * Um equipamento por linha. Fora da faixa, a linha inteira fica vermelha, o número cresce e a ação
 * vem junto; em atenção, amarela; em ordem, sem cor nenhuma.
 */
@Composable
private fun LinhaDoEquipamento(item: EquipamentoNoPainel, agora: Instant, aoAbrir: () -> Unit, aoAbrirAlertas: () -> Unit) {
    val equipamento = item.equipamento
    val leitura = item.ultimaLeitura
    val cores = item.semaforo.cores()
    val critico = item.semaforo == Semaforo.VERMELHO
    val faixa = "${grau(equipamento.tempMin)} a ${grau(equipamento.tempMax)}"

    LinhaSinal(cores, aoClicar = aoAbrir, rotuloDoClique = "Abrir o histórico") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(equipamento.nome, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        leitura == null && item.erroDaLeitura != null -> "Não foi possível buscar a leitura agora"
                        leitura == null -> "Nenhuma leitura nos últimos 7 dias"
                        // O horário é o do dado (registradaEm), nunca o de quando a tela abriu.
                        item.semaforo == Semaforo.SEM_SINAL -> "Sem sinal desde ${diaEHora(leitura.registradaEm)}"
                        else -> buildString {
                            append(
                                when (item.semaforo) {
                                    Semaforo.VERMELHO -> "Fora da faixa de $faixa"
                                    Semaforo.AMARELO -> "Em atenção · faixa de $faixa"
                                    else -> "Faixa de $faixa"
                                },
                            )
                            append(" · ${tempoDecorrido(leitura.registradaEm, agora)}")
                            if (leitura.portaAberta) append(" · porta aberta")
                            if (leitura.umidade > equipamento.umidMax) append(" · umidade ${umidade(leitura.umidade)}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = corSuave(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                leitura?.let { grau(it.temperatura) } ?: "—",
                style = TextStyle(
                    fontFamily = FonteDeNumeros,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (critico) 44.sp else 30.sp,
                ),
                color = if (item.semaforo == Semaforo.SEM_SINAL || leitura == null) corSuave() else LocalContentColor.current,
                maxLines = 1,
                modifier = Modifier.semantics {
                    contentDescription = leitura?.let { "Temperatura ${temperatura(it.temperatura)}" } ?: "Sem temperatura"
                },
            )
        }
        if (critico) {
            Spacer(Modifier.height(12.dp))
            BotaoDaLinha("Ver o alerta e registrar o que foi feito", cores, aoAbrirAlertas, Modifier.fillMaxWidth())
        }
    }
}
