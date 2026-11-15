package br.com.fiap.friosolidario.ui.alertas

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
fun AlertasScreen(aoVoltar: () -> Unit, aoAbrirEquipamento: (Long) -> Unit) {
    val viewModel = vm { AlertasViewModel(it.alertas) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val agora by agoraQueAnda()
    val avisos = remember { SnackbarHostState() }

    EnquantoVisivel(INTERVALO_DOS_ALERTAS_MS) { viewModel.atualizarEmSilencio() }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            viewModel.avisoVisto()
            avisos.showSnackbar(it)
        }
    }

    val listas = (estado.tela as? EstadoTela.Conteudo)?.valor

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Alertas") },
                    navigationIcon = {
                        IconButton(onClick = aoVoltar) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = estado.aba.ordinal) {
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
                                icone = Icons.Outlined.CheckCircle,
                            )
                        } else {
                            ListaVazia(
                                "Nenhum alerta resolvido ainda",
                                detalhe = "Os alertas tratados ficam registrados aqui, com a ação tomada.",
                                icone = Icons.Outlined.History,
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxSize(),
                            ) {
                                estado.erroAoAtualizar?.let { erro ->
                                    item { FaixaDeErro(erro, aoTentar = viewModel::puxarParaAtualizar) }
                                }
                                items(itens, key = { it.id }) { alerta ->
                                    CartaoDoAlerta(
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

private val Severidade.icone: ImageVector
    get() = when (this) {
        Severidade.CRITICA, Severidade.ALTA -> Icons.Filled.Error
        Severidade.MEDIA -> Icons.Filled.Warning
        Severidade.BAIXA -> Icons.Outlined.Info
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDoAlerta(
    alerta: AlertaDto,
    agora: Instant,
    aoResolver: () -> Unit,
    aoAbrirEquipamento: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvidoEm = alerta.resolvidoEm
    Card(
        onClick = aoAbrirEquipamento,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Etiqueta("Severidade ${alerta.severidade.rotulo.lowercase()}", alerta.severidade.cores(), icone = alerta.severidade.icone)
                if (resolvidoEm != null) {
                    Etiqueta("Resolvido", LocalCoresDeEstado.current.ok, icone = Icons.Outlined.CheckCircle)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(alerta.tipo.rotulo, style = MaterialTheme.typography.titleMedium)
            Text(
                alerta.equipamento.ifBlank { "Equipamento ${alerta.equipamentoId}" },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))

            if (resolvidoEm == null) {
                Text(
                    "Aberto ${tempoDecorrido(alerta.criadoEm, agora)} · ${diaEHora(alerta.criadoEm)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = aoResolver, modifier = Modifier.align(Alignment.End)) { Text("Resolver") }
            } else {
                Text(
                    "Resolvido ${tempoDecorrido(resolvidoEm, agora)} · ${diaEHora(resolvidoEm)} · " +
                        "ficou aberto por ${duracao(alerta.criadoEm, resolvidoEm)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    alerta.acaoTomada?.let { "Ação tomada: ${it.rotulo.lowercase()}" } ?: "Ação tomada: não informada",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
            Button(onClick = aoConfirmar, enabled = resolucao.acao != null && !resolucao.enviando) {
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
            TextButton(onClick = aoFechar, enabled = !resolucao.enviando) { Text("Cancelar") }
        },
    )
}
