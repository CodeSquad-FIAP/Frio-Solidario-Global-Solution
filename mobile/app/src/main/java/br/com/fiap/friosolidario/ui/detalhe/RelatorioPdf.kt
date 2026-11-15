package br.com.fiap.friosolidario.ui.detalhe

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.dia
import br.com.fiap.friosolidario.data.EquipamentoDto
import br.com.fiap.friosolidario.ui.comum.vm
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Botão do detalhe que gera o log térmico em PDF (`GET /equipamentos/{id}/relatorio`) e o entrega
 * ao Android para compartilhar ou imprimir — a saída em papel que substitui a planilha.
 */
@Composable
fun BotaoDeRelatorio(equipamento: EquipamentoDto) {
    val viewModel = vm(key = "relatorio-${equipamento.id}") { RelatorioViewModel(equipamento.id, it.relatorios) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    OutlinedButton(onClick = viewModel::abrir, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Relatório em PDF")
    }

    if (estado.aberto) {
        DialogoDoRelatorio(
            equipamento = equipamento,
            estado = estado,
            aoEscolherOpcao = viewModel::escolherOpcao,
            aoMudarDe = viewModel::mudarDe,
            aoMudarAte = viewModel::mudarAte,
            aoGerar = viewModel::gerar,
            aoFechar = viewModel::fechar,
        )
    }
}

private enum class DataEmEdicao { DE, ATE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DialogoDoRelatorio(
    equipamento: EquipamentoDto,
    estado: RelatorioEstado,
    aoEscolherOpcao: (OpcaoDePeriodo) -> Unit,
    aoMudarDe: (LocalDate) -> Unit,
    aoMudarAte: (LocalDate) -> Unit,
    aoGerar: () -> Unit,
    aoFechar: () -> Unit,
) {
    val contexto = LocalContext.current
    var dataEmEdicao by remember { mutableStateOf<DataEmEdicao?>(null) }
    var avisoDoAparelho by remember(estado.arquivo) { mutableStateOf<String?>(null) }
    val arquivo = estado.arquivo

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (arquivo != null) "Relatório pronto" else "Relatório em PDF") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(equipamento.nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // O período fica sempre escrito por extenso, inclusive no padrão de 7 dias.
                Text("Leituras de ${dia(estado.de)} a ${dia(estado.ate)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))

                if (arquivo == null) {
                    Column(Modifier.selectableGroup()) {
                        OpcaoDePeriodo.entries.forEach { opcao ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = estado.opcao == opcao,
                                        enabled = !estado.baixando,
                                        role = Role.RadioButton,
                                        onClick = { aoEscolherOpcao(opcao) },
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = estado.opcao == opcao, onClick = null, enabled = !estado.baixando)
                                Spacer(Modifier.width(12.dp))
                                Text(opcao.rotulo, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    if (estado.opcao == OpcaoDePeriodo.ESCOLHER) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { dataEmEdicao = DataEmEdicao.DE }, enabled = !estado.baixando) {
                                Text("De ${dia(estado.de)}")
                            }
                            OutlinedButton(onClick = { dataEmEdicao = DataEmEdicao.ATE }, enabled = !estado.baixando) {
                                Text("Até ${dia(estado.ate)}")
                            }
                        }
                    }
                    val textoDeErro = estado.erroDoPeriodo ?: estado.erro?.mensagem
                    if (textoDeErro != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(textoDeErro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        "O PDF foi gerado pela API com o log do período. Envie, salve ou imprima.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { avisoDoAparelho = compartilhar(contexto, arquivo) }) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartilhar")
                        }
                        OutlinedButton(onClick = { avisoDoAparelho = imprimir(contexto, arquivo) }) {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Imprimir")
                        }
                    }
                    avisoDoAparelho?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            if (arquivo == null) {
                Button(onClick = aoGerar, enabled = !estado.baixando && estado.erroDoPeriodo == null) {
                    if (estado.baixando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Gerando…")
                    } else {
                        Text(if (estado.erro != null) "Tentar de novo" else "Gerar PDF")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar, enabled = !estado.baixando) { Text(if (arquivo != null) "Fechar" else "Cancelar") }
        },
    )

    dataEmEdicao?.let { qual ->
        val atual = if (qual == DataEmEdicao.DE) estado.de else estado.ate
        val seletor = rememberDatePickerState(
            initialSelectedDateMillis = atual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { dataEmEdicao = null },
            confirmButton = {
                TextButton(onClick = {
                    seletor.selectedDateMillis?.let { millis ->
                        val data = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (qual == DataEmEdicao.DE) aoMudarDe(data) else aoMudarAte(data)
                    }
                    dataEmEdicao = null
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { dataEmEdicao = null }) { Text("Cancelar") } },
        ) {
            DatePicker(state = seletor)
        }
    }
}

/** Abre a folha de compartilhamento do Android com o PDF. Devolve a frase de erro, se houver. */
private fun compartilhar(contexto: Context, arquivo: File): String? = try {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.arquivos", arquivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Log térmico - Frio Solidário")
        clipData = ClipData.newRawUri(arquivo.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(envio, "Enviar relatório"))
    null
} catch (_: ActivityNotFoundException) {
    "Este aparelho não tem aplicativo para receber o PDF."
} catch (_: Exception) {
    "Não foi possível abrir o compartilhamento."
}

/** Manda o PDF para a impressão do Android (impressora ou "Salvar como PDF"). */
private fun imprimir(contexto: Context, arquivo: File): String? = try {
    val atividade = contexto.atividade() ?: return "Não foi possível abrir a impressão."
    // Sem o recurso de impressão (TV, alguns tablets) o PrintManager não faz nada e não avisa.
    if (!atividade.packageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING)) return SEM_IMPRESSAO
    val impressao = atividade.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return SEM_IMPRESSAO
    impressao.print("Frio Solidário - ${arquivo.nameWithoutExtension}", AdaptadorDePdf(arquivo), PrintAttributes.Builder().build())
    null
} catch (_: Exception) {
    SEM_IMPRESSAO
}

private const val SEM_IMPRESSAO = "Este aparelho não tem serviço de impressão. Use Compartilhar para enviar o PDF."

private tailrec fun Context.atividade(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.atividade()
    else -> null
}

/** Entrega os bytes do PDF como estão: quem pagina e desenha é o próprio arquivo. */
private class AdaptadorDePdf(private val arquivo: File) : PrintDocumentAdapter() {

    override fun onLayout(
        antigos: PrintAttributes?,
        novos: PrintAttributes?,
        cancelamento: CancellationSignal?,
        retorno: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancelamento?.isCanceled == true) {
            retorno.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(arquivo.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        retorno.onLayoutFinished(info, antigos != novos)
    }

    override fun onWrite(
        paginas: Array<out PageRange>?,
        destino: ParcelFileDescriptor,
        cancelamento: CancellationSignal?,
        retorno: WriteResultCallback,
    ) {
        try {
            FileInputStream(arquivo).use { entrada ->
                FileOutputStream(destino.fileDescriptor).use { saida -> entrada.copyTo(saida) }
            }
            retorno.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (erro: Exception) {
            retorno.onWriteFailed(erro.message)
        }
    }
}
