package br.com.fiap.friosolidario.ui.lotes

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.fiap.friosolidario.ui.tema.FonteDeNumeros
import br.com.fiap.friosolidario.ui.comum.CabecalhoDaTela
import br.com.fiap.friosolidario.ui.comum.LinhaSinal
import br.com.fiap.friosolidario.ui.comum.Fio
import br.com.fiap.friosolidario.ui.comum.MargemDaTela
import br.com.fiap.friosolidario.ui.comum.corSuave
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
fun LotesScreen(sessao: Sessao, aoNovoLote: () -> Unit) {
    val viewModel = vm { LotesViewModel(it.lotes) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.aoFicarVisivel()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.avisos.collect { avisos.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CabecalhoDaTela(
                titulo = "Lotes doados",
                subtitulo = (estado.tela as? EstadoTela.Conteudo)?.valor?.takeIf { it.isNotEmpty() }?.let(::balanco),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        floatingActionButton = {
            if (sessao.ehAdmin) {
                ExtendedFloatingActionButton(
                    onClick = aoNovoLote,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Lote") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = if (sessao.ehAdmin) 96.dp else 24.dp),
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
                                items(tela.valor, key = { it.id }) { lote ->
                                    LinhaDoLote(lote, podeRegistrarSaida = sessao.ehAdmin, aoPedirSaida = { viewModel.pedirSaida(lote, it) })
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
                Button(shape = FormaDoBotao, onClick = viewModel::confirmarSaida, enabled = !saida.enviando) {
                    if (saida.enviando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (saida.erro != null) "Tentar de novo" else "Confirmar")
                }
            },
            dismissButton = { TextButton(shape = FormaDoBotao, onClick = viewModel::cancelarSaida, enabled = !saida.enviando) { Text("Cancelar") } },
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

/** Quilos por situação, somados da lista da API: o que está guardado, o que foi salvo e o que se perdeu. */
private fun balanco(lotes: List<LoteDto>): String {
    fun soma(status: StatusLote) = quilos(lotes.filter { it.status == status }.sumOf { it.kg })
    return "${soma(StatusLote.DISPONIVEL)} em estoque · ${soma(StatusLote.DISTRIBUIDO)} distribuídos · " +
        "${soma(StatusLote.PERDIDO)} perdidos"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinhaDoLote(lote: LoteDto, podeRegistrarSaida: Boolean, aoPedirSaida: (StatusLote) -> Unit) {
    LinhaSinal(LocalCoresDeEstado.current.neutro) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(lote.descricao, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                val retiradoEm = lote.retiradoEm
                Text(
                    buildString {
                        append(lote.categoria.lowercase().replaceFirstChar { it.titlecase() })
                        if (retiradoEm == null) {
                            append(" · validade ${dia(lote.validade)} · ${prazoDeValidade(lote.validade)}")
                        } else {
                            append(" · saiu em ${diaEHora(retiradoEm)} · validade ${dia(lote.validade)}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = corSuave(),
                )
                if (lote.status != StatusLote.DISPONIVEL) {
                    Spacer(Modifier.height(6.dp))
                    Etiqueta(lote.status.rotulo, lote.status.cores())
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                quilos(lote.kg),
                style = TextStyle(fontFamily = FonteDeNumeros, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                color = if (lote.status == StatusLote.DISPONIVEL) LocalContentColor.current else corSuave(),
                maxLines = 1,
            )
        }
        if (lote.status == StatusLote.DISPONIVEL && podeRegistrarSaida) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(shape = FormaDoBotao, onClick = { aoPedirSaida(StatusLote.DISTRIBUIDO) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Distribuído") }
                OutlinedButton(shape = FormaDoBotao, onClick = { aoPedirSaida(StatusLote.PERDIDO) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Perdido") }
            }
        }
    }
}
