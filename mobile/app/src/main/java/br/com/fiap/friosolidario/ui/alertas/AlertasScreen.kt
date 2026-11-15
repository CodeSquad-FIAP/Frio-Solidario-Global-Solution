package br.com.fiap.friosolidario.ui.alertas

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.layout.statusBarsPadding
import br.com.fiap.friosolidario.ui.comum.CabecalhoDaTela
import br.com.fiap.friosolidario.ui.comum.LinhaSinal
import br.com.fiap.friosolidario.ui.comum.BotaoDaLinha
import br.com.fiap.friosolidario.ui.comum.Fio
import br.com.fiap.friosolidario.ui.comum.MargemDaTela
import br.com.fiap.friosolidario.ui.comum.corSuave
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.diaEHora
import br.com.fiap.friosolidario.core.duracao
import br.com.fiap.friosolidario.core.tempoDecorrido
import br.com.fiap.friosolidario.data.AcaoTomada
import br.com.fiap.friosolidario.data.AlertaDto
import br.com.fiap.friosolidario.data.Severidade
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.EnquantoVisivel
import br.com.fiap.friosolidario.ui.comum.ErroComTentarDeNovo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import br.com.fiap.friosolidario.ui.comum.Etiqueta
import br.com.fiap.friosolidario.ui.comum.FaixaDeErro
import br.com.fiap.friosolidario.ui.comum.ListaVazia
import br.com.fiap.friosolidario.ui.comum.agoraQueAnda
import br.com.fiap.friosolidario.ui.comum.vm
import br.com.fiap.friosolidario.ui.tema.CorDeEstado
import br.com.fiap.friosolidario.ui.tema.LocalCoresDeEstado
import java.time.Instant

/** O alerta aberto pelo sensor aparece na fila em até este tempo, sem tocar na tela. */
private const val INTERVALO_DOS_ALERTAS_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertasScreen(aoAbrirEquipamento: (Long) -> Unit) {
    val viewModel = vm { AlertasViewModel(it.alertas) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val agora by agoraQueAnda()
    val avisos = remember { SnackbarHostState() }

    EnquantoVisivel(INTERVALO_DOS_ALERTAS_MS) { viewModel.atualizarEmSilencio() }

    LaunchedEffect(viewModel) {
        viewModel.avisos.collect { avisos.showSnackbar(it) }
    }

    val listas = (estado.tela as? EstadoTela.Conteudo)?.valor

    Scaffold(
        topBar = {
            Column {
                CabecalhoDaTela(
                    titulo = "Alertas",
                    subtitulo = when (val abertos = listas?.abertos?.size) {
                        null -> null
                        0 -> "Nenhum aberto"
                        1 -> "1 aberto, esperando ação"
                        else -> "$abertos abertos, esperando ação"
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
                PrimaryTabRow(selectedTabIndex = estado.aba.ordinal, containerColor = MaterialTheme.colorScheme.background) {
                    AbaDeAlertas.entries.forEach { aba ->
                        val quantidade = listas?.abertos?.size?.takeIf { aba == AbaDeAlertas.ABERTOS && it > 0 }
                        Tab(
                            selected = aba == estado.aba,
                            onClick = { viewModel.mudarAba(aba) },
                            text = { Text(if (quantidade != null) "${aba.rotulo} ($quantidade)" else aba.rotulo) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(avisos) },
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
                is EstadoTela.Conteudo -> {
                    val itens = if (estado.aba == AbaDeAlertas.ABERTOS) tela.valor.abertos else tela.valor.historico
                    if (itens.isEmpty() && estado.erroAoAtualizar == null) {
                        if (estado.aba == AbaDeAlertas.ABERTOS) {
                            ListaVazia(
                                "Nenhum alerta aberto",
                                detalhe = "Os equipamentos estão dentro da faixa ou os alertas já foram tratados.",
                            )
                        } else {
                            ListaVazia(
                                "Nenhum alerta resolvido ainda",
                                detalhe = "Os alertas tratados ficam registrados aqui, com a ação tomada.",
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxSize(),
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
                                items(itens, key = { it.id }) { alerta ->
                                    LinhaDoAlerta(
                                        alerta = alerta,
                                        agora = agora,
                                        aoResolver = { viewModel.abrirResolucao(alerta) },
                                        aoAbrirEquipamento = { aoAbrirEquipamento(alerta.equipamentoId) },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    estado.resolucao?.let { resolucao ->
        DialogoDeResolucao(
            resolucao = resolucao,
            aoEscolher = viewModel::escolherAcao,
            aoConfirmar = viewModel::confirmarResolucao,
            aoFechar = viewModel::fecharResolucao,
        )
    }
}

@Composable
private fun Severidade.cores(): CorDeEstado = with(LocalCoresDeEstado.current) {
    when (this@cores) {
        Severidade.CRITICA, Severidade.ALTA -> critico
        Severidade.MEDIA -> atencao
        Severidade.BAIXA -> neutro
    }
}

/**
 * Alerta aberto: a linha inteira leva a cor da severidade e traz o botão de resolver. Resolvido:
 * linha sem cor, com o que foi feito.
 */
@Composable
private fun LinhaDoAlerta(
    alerta: AlertaDto,
    agora: Instant,
    aoResolver: () -> Unit,
    aoAbrirEquipamento: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvidoEm = alerta.resolvidoEm
    val cores = if (resolvidoEm == null) alerta.severidade.cores() else LocalCoresDeEstado.current.neutro
    LinhaSinal(cores, modifier, aoClicar = aoAbrirEquipamento, rotuloDoClique = "Abrir o equipamento") {
        Text(alerta.tipo.rotulo, style = MaterialTheme.typography.titleLarge)
        Text(alerta.equipamento.ifBlank { "Equipamento ${alerta.equipamentoId}" }, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        if (resolvidoEm == null) {
            Text(
                "Severidade ${alerta.severidade.rotulo.lowercase()} · aberto ${tempoDecorrido(alerta.criadoEm, agora)} · " +
                    diaEHora(alerta.criadoEm),
                style = MaterialTheme.typography.bodyMedium,
                color = corSuave(),
            )
            Spacer(Modifier.height(12.dp))
            BotaoDaLinha("Resolver", cores, aoResolver, Modifier.fillMaxWidth())
        } else {
            Text(
                alerta.acaoTomada?.let { "Ação tomada: ${it.rotulo.lowercase()}" } ?: "Ação tomada: não informada",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Resolvido ${tempoDecorrido(resolvidoEm, agora)} · ${diaEHora(resolvidoEm)} · " +
                    "ficou aberto por ${duracao(alerta.criadoEm, resolvidoEm)} · " +
                    "severidade ${alerta.severidade.rotulo.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = corSuave(),
            )
        }
    }
}

@Composable
private fun DialogoDeResolucao(
    resolucao: Resolucao,
    aoEscolher: (AcaoTomada) -> Unit,
    aoConfirmar: () -> Unit,
    aoFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("O que você fez?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${resolucao.alerta.tipo.rotulo} · ${resolucao.alerta.equipamento}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Column(Modifier.selectableGroup()) {
                    AcaoTomada.entries.forEach { acao ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = resolucao.acao == acao,
                                    enabled = !resolucao.enviando,
                                    role = Role.RadioButton,
                                    onClick = { aoEscolher(acao) },
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = resolucao.acao == acao, onClick = null, enabled = !resolucao.enviando)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(acao.rotulo, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    acao.descricao,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                resolucao.erro?.let { erro ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${erro.mensagem} O alerta continua aberto.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(shape = FormaDoBotao, onClick = aoConfirmar, enabled = resolucao.acao != null && !resolucao.enviando) {
                if (resolucao.enviando) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Enviando…")
                } else {
                    Text(if (resolucao.erro != null) "Tentar de novo" else "Marcar como resolvido")
                }
            }
        },
        dismissButton = {
            TextButton(shape = FormaDoBotao, onClick = aoFechar, enabled = !resolucao.enviando) { Text("Cancelar") }
        },
    )
}
