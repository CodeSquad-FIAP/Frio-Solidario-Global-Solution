package br.com.fiap.friosolidario.ui.lotes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.dia
import br.com.fiap.friosolidario.core.diaEHora
import br.com.fiap.friosolidario.core.ehAdmin
import br.com.fiap.friosolidario.core.prazoDeValidade
import br.com.fiap.friosolidario.core.quilos
import br.com.fiap.friosolidario.data.LoteDto
import br.com.fiap.friosolidario.data.StatusLote
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.ErroComTentarDeNovo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import br.com.fiap.friosolidario.ui.comum.Etiqueta
import br.com.fiap.friosolidario.ui.comum.FaixaDeErro
import br.com.fiap.friosolidario.ui.comum.ListaVazia
import br.com.fiap.friosolidario.ui.comum.vm
import br.com.fiap.friosolidario.ui.tema.CorDeEstado
import br.com.fiap.friosolidario.ui.tema.LocalCoresDeEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotesScreen(sessao: Sessao, aoVoltar: () -> Unit, aoNovoLote: () -> Unit) {
    val viewModel = vm { LotesViewModel(it.lotes) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.aoFicarVisivel()
        onPauseOrDispose { }
    }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            viewModel.avisoVisto()
            avisos.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lotes doados") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        floatingActionButton = {
            if (sessao.ehAdmin) {
                ExtendedFloatingActionButton(
                    onClick = aoNovoLote,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Lote") },
                )
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
                is EstadoTela.Conteudo ->
                    if (tela.valor.isEmpty() && estado.erroAoAtualizar == null) {
                        ListaVazia(
                            "Nenhum lote doado registrado",
                            detalhe = if (sessao.ehAdmin) {
                                "Registre as doações que chegam para acompanhar quantos quilos foram distribuídos e quantos se perderam."
                            } else {
                                "O administrador da instituição registra as doações que chegam."
                            },
                            icone = Icons.Outlined.Inventory2,
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (sessao.ehAdmin) 96.dp else 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxSize(),
                            ) {
                                estado.erroAoAtualizar?.let { erro ->
                                    item { FaixaDeErro(erro, aoTentar = viewModel::puxarParaAtualizar) }
                                }
                                item { Indicadores(tela.valor) }
                                items(tela.valor, key = { it.id }) { lote ->
                                    CartaoDoLote(lote, podeRegistrarSaida = sessao.ehAdmin, aoPedirSaida = { viewModel.pedirSaida(lote, it) })
                                }
                            }
                        }
                    }
            }
        }
    }

    estado.saida?.let { saida ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarSaida,
            title = { Text(if (saida.status == StatusLote.DISTRIBUIDO) "Marcar como distribuído?" else "Marcar como perdido?") },
            text = {
                Column {
                    Text("${saida.lote.descricao} · ${quilos(saida.lote.kg)}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A saída do estoque não pode ser desfeita.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    saida.erro?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it.mensagem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmarSaida, enabled = !saida.enviando) {
                    if (saida.enviando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (saida.erro != null) "Tentar de novo" else "Confirmar")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarSaida, enabled = !saida.enviando) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StatusLote.cores(): CorDeEstado = with(LocalCoresDeEstado.current) {
    when (this@cores) {
        StatusLote.DISPONIVEL -> neutro
        StatusLote.DISTRIBUIDO -> ok
        StatusLote.PERDIDO -> critico
    }
}

/** Quilos por situação, somados da lista da API: o que chegou, o que foi salvo e o que se perdeu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Indicadores(lotes: List<LoteDto>) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusLote.entries.forEach { status ->
            Column {
                Text(
                    when (status) {
                        StatusLote.DISPONIVEL -> "Em estoque"
                        StatusLote.DISTRIBUIDO -> "Distribuídos"
                        StatusLote.PERDIDO -> "Perdidos"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(quilos(lotes.filter { it.status == status }.sumOf { it.kg }), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDoLote(lote: LoteDto, podeRegistrarSaida: Boolean, aoPedirSaida: (StatusLote) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Etiqueta(lote.status.rotulo, lote.status.cores())
            Spacer(Modifier.height(10.dp))
            Text(lote.descricao, style = MaterialTheme.typography.titleMedium)
            Text(
                "${lote.categoria.lowercase().replaceFirstChar { it.titlecase() }} · ${quilos(lote.kg)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            val retiradoEm = lote.retiradoEm
            Text(
                if (retiradoEm == null) {
                    "Validade ${dia(lote.validade)} · ${prazoDeValidade(lote.validade)}"
                } else {
                    "Saiu do estoque em ${diaEHora(retiradoEm)} · validade ${dia(lote.validade)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lote.status == StatusLote.DISPONIVEL && podeRegistrarSaida) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { aoPedirSaida(StatusLote.DISTRIBUIDO) }) { Text("Distribuído") }
                    OutlinedButton(onClick = { aoPedirSaida(StatusLote.PERDIDO) }) { Text("Perdido") }
                }
            }
        }
    }
}
