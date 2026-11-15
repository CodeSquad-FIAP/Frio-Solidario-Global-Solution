package br.com.fiap.friosolidario.ui.detalhe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.diaEHora
import br.com.fiap.friosolidario.core.temperatura
import br.com.fiap.friosolidario.core.tempoDecorrido
import br.com.fiap.friosolidario.core.umidade
import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.data.LeituraDto
import br.com.fiap.friosolidario.data.Periodo
import br.com.fiap.friosolidario.dominio.foraDaFaixa
import br.com.fiap.friosolidario.dominio.resumoDaSerie
import br.com.fiap.friosolidario.dominio.semaforo
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.EnquantoVisivel
import br.com.fiap.friosolidario.ui.comum.ErroComTentarDeNovo
import br.com.fiap.friosolidario.ui.comum.EstadoTela
import br.com.fiap.friosolidario.ui.comum.Etiqueta
import br.com.fiap.friosolidario.ui.comum.FaixaDeErro
import br.com.fiap.friosolidario.ui.comum.agoraQueAnda
import br.com.fiap.friosolidario.ui.comum.cores
import br.com.fiap.friosolidario.ui.comum.icone
import br.com.fiap.friosolidario.ui.comum.vm
import br.com.fiap.friosolidario.ui.tema.LocalCoresDeEstado
import java.time.Instant

private const val INTERVALO_DO_DETALHE_MS = 15_000L
private const val LINHAS_DA_TABELA = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheScreen(equipamentoId: Long, aoVoltar: () -> Unit, acoes: @Composable (EquipamentoDto) -> Unit = {}) {
    val viewModel = vm(key = "detalhe-$equipamentoId") { DetalheViewModel(equipamentoId, it.equipamentos) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val agora by agoraQueAnda()

    EnquantoVisivel(INTERVALO_DO_DETALHE_MS) { viewModel.atualizarEmSilencio() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (estado.equipamento as? EstadoTela.Conteudo)?.valor?.nome ?: "Equipamento",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { margens ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(margens),
        ) {
            when (val tela = estado.equipamento) {
                EstadoTela.Carregando -> Carregando()
                is EstadoTela.Erro -> ErroComTentarDeNovo(tela.erro, aoTentar = viewModel::carregar)
                is EstadoTela.Conteudo -> Conteudo(
                    equipamento = tela.valor,
                    estado = estado,
                    agora = agora,
                    aoMudarPeriodo = viewModel::mudarPeriodo,
                    aoTentarSerie = viewModel::tentarSerieDeNovo,
                    acoes = acoes,
                )
            }
        }
    }
}

@Composable
private fun Conteudo(
    equipamento: EquipamentoDto,
    estado: DetalheEstado,
    agora: Instant,
    aoMudarPeriodo: (Periodo) -> Unit,
    aoTentarSerie: () -> Unit,
    acoes: @Composable (EquipamentoDto) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            estado.erroAoAtualizar?.let { FaixaDeErro(it, aoTentar = aoTentarSerie) }

            Cabecalho(equipamento, estado.ultimaLeitura, agora)

            acoes(equipamento)

            SeletorDePeriodo(estado.periodo, aoMudarPeriodo)

            // Só este bloco troca quando o período muda; o cabeçalho acima não é refeito.
            when (val serie = estado.serie) {
                EstadoTela.Carregando -> Moldura {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is EstadoTela.Erro -> Moldura {
                    ErroComTentarDeNovo(serie.erro, aoTentar = aoTentarSerie, modifier = Modifier.height(260.dp))
                }

                is EstadoTela.Conteudo -> {
                    val leituras = serie.valor.leituras
                    Moldura {
                        Column(Modifier.padding(16.dp)) {
                            Text("Temperatura · últimas ${serie.valor.periodo.rotulo}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            if (leituras.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Sem leituras no período",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                GraficoTemperatura(
                                    leituras = leituras,
                                    equipamento = equipamento,
                                    inicio = serie.valor.inicio,
                                    fim = serie.valor.fim,
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Resumo(leituras, equipamento)
                            }
                        }
                    }
                    if (leituras.isNotEmpty()) {
                        Tabela(leituras.takeLast(LINHAS_DA_TABELA).reversed(), equipamento, total = leituras.size)
                    }
                }
            }
        }
    }
}

@Composable
private fun Moldura(conteudo: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) { conteudo() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Cabecalho(equipamento: EquipamentoDto, ultima: LeituraDto?, agora: Instant) {
    val estado = semaforo(equipamento, ultima, agora)
    val cores = estado.cores()
    Column(
        Modifier
            .fillMaxWidth()
            .background(cores.fundo, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ultima?.let { temperatura(it.temperatura) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = cores.sobreFundo,
            )
            Etiqueta(estado.rotulo, cores.copy(fundo = MaterialTheme.colorScheme.surface), icone = estado.icone)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${equipamento.tipo.rotulo} · faixa de ${temperatura(equipamento.tempMin)} a ${temperatura(equipamento.tempMax)} · " +
                "umidade até ${umidade(equipamento.umidMax)}",
            style = MaterialTheme.typography.bodyMedium,
            color = cores.sobreFundo,
        )
        Text(
            if (ultima == null) {
                "Nenhuma leitura nos últimos 7 dias"
            } else {
                "Última leitura ${tempoDecorrido(ultima.registradaEm, agora)} (${diaEHora(ultima.registradaEm)}) · " +
                    "umidade ${umidade(ultima.umidade)} · porta ${if (ultima.portaAberta) "aberta" else "fechada"}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = cores.sobreFundo,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorDePeriodo(atual: Periodo, aoMudar: (Periodo) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Periodo.entries.forEachIndexed { indice, periodo ->
            SegmentedButton(
                selected = periodo == atual,
                onClick = { aoMudar(periodo) },
                shape = SegmentedButtonDefaults.itemShape(indice, Periodo.entries.size),
            ) { Text(periodo.rotulo) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Resumo(leituras: List<LeituraDto>, equipamento: EquipamentoDto) {
    val resumo = resumoDaSerie(leituras, equipamento.tempMin, equipamento.tempMax) ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Numero("Mínima", temperatura(resumo.minima))
        Numero("Média", temperatura(resumo.media))
        Numero("Máxima", temperatura(resumo.maxima))
        Numero(
            "Fora da faixa",
            if (resumo.foraDaFaixa == 0) "nenhuma" else "${resumo.foraDaFaixa} de ${resumo.total}",
            destaque = resumo.foraDaFaixa > 0,
        )
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, destaque: Boolean = false) {
    Column {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            color = if (destaque) LocalCoresDeEstado.current.critico.forte else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Tabela(leituras: List<LeituraDto>, equipamento: EquipamentoDto, total: Int) {
    Moldura {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(
                if (total > leituras.size) "Últimas ${leituras.size} leituras" else "Leituras do período",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Linha("Horário", "Temperatura", "Umidade", "Porta", cabecalho = true)
            leituras.forEach { leitura ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Linha(
                    diaEHora(leitura.registradaEm),
                    temperatura(leitura.temperatura),
                    umidade(leitura.umidade),
                    if (leitura.portaAberta) "Aberta" else "Fechada",
                    fora = foraDaFaixa(leitura, equipamento),
                )
            }
        }
    }
}

@Composable
private fun Linha(horario: String, temperatura: String, umidade: String, porta: String, cabecalho: Boolean = false, fora: Boolean = false) {
    val estilo = if (cabecalho) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val cor = if (cabecalho) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val critico = LocalCoresDeEstado.current.critico
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (fora) critico.fundo else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(horario, style = estilo, color = cor, modifier = Modifier.weight(1.3f))
        Row(Modifier.weight(1.3f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (fora) {
                // Ícone além da cor: a linha fora da faixa se distingue também em preto e branco.
                Icon(Icons.Filled.Error, contentDescription = "Fora da faixa", tint = critico.forte, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                temperatura,
                style = estilo,
                color = if (fora) critico.sobreFundo else cor,
                fontWeight = if (fora) FontWeight.SemiBold else null,
                textAlign = TextAlign.End,
            )
        }
        Text(umidade, style = estilo, color = cor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(porta, style = estilo, color = cor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
