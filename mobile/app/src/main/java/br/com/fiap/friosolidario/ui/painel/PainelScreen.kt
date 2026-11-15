package br.com.fiap.friosolidario.ui.painel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.core.ehAdmin
import br.com.fiap.friosolidario.core.diaEHora
import br.com.fiap.friosolidario.core.hora
import br.com.fiap.friosolidario.core.temperatura
import br.com.fiap.friosolidario.core.tempoDecorrido
import br.com.fiap.friosolidario.core.umidade
import br.com.fiap.friosolidario.data.EquipamentoNoPainel
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.EnquantoVisivel
import br.com.fiap.friosolidario.ui.comum.ErroComTentarDeNovo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import br.com.fiap.friosolidario.ui.comum.Etiqueta
import br.com.fiap.friosolidario.ui.comum.FaixaDeErro
import br.com.fiap.friosolidario.ui.comum.ListaVazia
import br.com.fiap.friosolidario.ui.comum.agoraQueAnda
import br.com.fiap.friosolidario.ui.comum.cores
import br.com.fiap.friosolidario.ui.comum.icone
import br.com.fiap.friosolidario.ui.comum.vm
import java.time.Instant

/** De quanto em quanto tempo o painel se atualiza sozinho enquanto está aberto. */
private const val INTERVALO_DO_PAINEL_MS = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(
    sessao: Sessao,
    alertasAbertos: Int?,
    aoAbrirEquipamento: (Long) -> Unit,
    aoAbrirAlertas: () -> Unit,
    aoAbrirLotes: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(sessao.instituicao, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${sessao.nome} · ${if (sessao.ehAdmin) "Administrador" else "Voluntário"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = aoAbrirAlertas) {
                        BadgedBox(badge = { if ((alertasAbertos ?: 0) > 0) Badge { Text("$alertasAbertos") } }) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = when (alertasAbertos) {
                                    null, 0 -> "Alertas"
                                    1 -> "Alertas: 1 aberto"
                                    else -> "Alertas: $alertasAbertos abertos"
                                },
                            )
                        }
                    }
                    MenuDoPainel(aoAbrirLotes = aoAbrirLotes, aoSair = viewModel::sair)
                },
            )
        },
        floatingActionButton = {
            if (sessao.ehAdmin) {
                ExtendedFloatingActionButton(
                    onClick = aoNovoEquipamento,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Equipamento") },
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
                            icone = Icons.Outlined.Kitchen,
                        )
                    } else {
                        ListaDeEquipamentos(
                            itens = tela.valor,
                            agora = agora,
                            erroAoAtualizar = estado.erroAoAtualizar,
                            aoTentar = viewModel::puxarParaAtualizar,
                            aoAbrir = aoAbrirEquipamento,
                            folgaDoBotao = sessao.ehAdmin,
                        )
                    }
            }
        }
    }
}

@Composable
private fun MenuDoPainel(aoAbrirLotes: () -> Unit, aoSair: () -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { aberto = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text("Lotes doados") },
                leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                onClick = {
                    aberto = false
                    aoAbrirLotes()
                },
            )
            DropdownMenuItem(
                text = { Text("Sair") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                onClick = {
                    aberto = false
                    aoSair()
                },
            )
        }
    }
}

@Composable
private fun ListaDeEquipamentos(
    itens: List<EquipamentoNoPainel>,
    agora: Instant,
    erroAoAtualizar: br.com.fiap.friosolidario.core.ErroApp?,
    aoTentar: () -> Unit,
    aoAbrir: (Long) -> Unit,
    folgaDoBotao: Boolean,
) {
    LazyVerticalGrid(
        // Uma coluna no celular; duas ou três em tablet, sem cartão esticado de ponta a ponta.
        columns = GridCells.Adaptive(minSize = 300.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (folgaDoBotao) 96.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (erroAoAtualizar != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FaixaDeErro(erroAoAtualizar, aoTentar = aoTentar)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Resumo(itens) }
        items(itens, key = { it.equipamento.id }) { item ->
            CartaoDoEquipamento(item, agora, aoAbrir = { aoAbrir(item.equipamento.id) })
        }
    }
}

/** A resposta de três segundos: está tudo bem? */
@Composable
private fun Resumo(itens: List<EquipamentoNoPainel>) {
    val vermelhos = itens.count { it.semaforo == Semaforo.VERMELHO }
    val amarelos = itens.count { it.semaforo == Semaforo.AMARELO }
    val semLeitura = itens.count { it.semaforo == Semaforo.SEM_LEITURA }
    val semSinal = itens.count { it.semaforo == Semaforo.SEM_SINAL }

    val (estado, titulo) = when {
        vermelhos > 0 -> Semaforo.VERMELHO to if (vermelhos == 1) "1 equipamento fora da faixa" else "$vermelhos equipamentos fora da faixa"
        amarelos > 0 -> Semaforo.AMARELO to if (amarelos == 1) "1 equipamento pede atenção" else "$amarelos equipamentos pedem atenção"
        // Sensor parado nunca entra em "tudo dentro da faixa": não se sabe como ele está.
        semSinal > 0 -> Semaforo.SEM_SINAL to if (semSinal == 1) "1 equipamento sem sinal" else "$semSinal equipamentos sem sinal"
        semLeitura == itens.size -> Semaforo.SEM_LEITURA to "Nenhum equipamento enviou leitura"
        else -> Semaforo.VERDE to "Tudo dentro da faixa"
    }
    val detalhes = buildList {
        if (vermelhos > 0 && amarelos > 0) add(if (amarelos == 1) "1 em atenção" else "$amarelos em atenção")
        if (semSinal > 0 && (vermelhos > 0 || amarelos > 0)) add("$semSinal sem sinal")
        if (semLeitura in 1 until itens.size) add("$semLeitura sem leitura")
        add(if (itens.size == 1) "1 equipamento monitorado" else "${itens.size} equipamentos monitorados")
    }
    val cores = estado.cores()

    Row(
        Modifier
            .fillMaxWidth()
            .background(cores.fundo, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(estado.icone, contentDescription = null, tint = cores.forte, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = cores.sobreFundo, fontWeight = FontWeight.SemiBold)
            Text(detalhes.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = cores.sobreFundo)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDoEquipamento(item: EquipamentoNoPainel, agora: Instant, aoAbrir: () -> Unit) {
    val equipamento = item.equipamento
    val leitura = item.ultimaLeitura
    val cores = item.semaforo.cores()

    Card(
        onClick = aoAbrir,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            // Faixa lateral na cor do estado: dá para ler a lista inteira de relance.
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxSize()
                    .background(cores.forte),
            )
            Column(Modifier.padding(16.dp)) {
                Text(equipamento.nome, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${equipamento.tipo.rotulo} · faixa ${temperatura(equipamento.tempMin)} a ${temperatura(equipamento.tempMax)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        leitura?.let { temperatura(it.temperatura) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics {
                            contentDescription = leitura?.let { "Temperatura ${temperatura(it.temperatura)}" } ?: "Sem temperatura"
                        },
                    )
                    Etiqueta(item.semaforo.rotulo, cores, icone = item.semaforo.icone)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (leitura == null) {
                        "Nenhuma leitura nos últimos 7 dias"
                    } else {
                        // O horário é o do dado (registradaEm), nunca o de quando a tela abriu.
                        buildString {
                            // Leitura de outro dia leva a data: "14:32" sozinho pareceria de hoje.
                            val quando = if (item.semaforo == Semaforo.SEM_SINAL) diaEHora(leitura.registradaEm) else hora(leitura.registradaEm)
                            append("Última leitura ${tempoDecorrido(leitura.registradaEm, agora)} · $quando")
                            append(" · umidade ${umidade(leitura.umidade)}")
                            if (leitura.portaAberta) append(" · porta aberta")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

