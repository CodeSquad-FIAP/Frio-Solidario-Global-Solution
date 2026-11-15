package br.com.fiap.friosolidario.ui.lotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.dia
import br.com.fiap.friosolidario.data.StatusLote
import br.com.fiap.friosolidario.dominio.Campo
import br.com.fiap.friosolidario.ui.comum.CampoDeTexto
import br.com.fiap.friosolidario.ui.comum.Formulario
import br.com.fiap.friosolidario.ui.comum.vm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Atalhos de digitação; a categoria é texto livre na API. */
private val CATEGORIAS_COMUNS = listOf("Hortifruti", "Frutas", "Proteínas", "Laticínios", "Padaria", "Congelados")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoteFormScreen(aoVoltar: () -> Unit, aoSalvar: () -> Unit) {
    val viewModel = vm { LoteFormViewModel(it.lotes) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var escolhendoData by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.salvo) { if (estado.salvo) aoSalvar() }

    Formulario(
        titulo = "Novo lote doado",
        textoDoBotao = "Registrar lote",
        enviando = estado.enviando,
        erro = estado.erro,
        aoVoltar = aoVoltar,
        aoEnviar = viewModel::salvar,
    ) {
        CampoDeTexto(
            valor = estado.descricao,
            aoMudar = viewModel::mudarDescricao,
            rotulo = "Descrição",
            erro = estado.erros[Campo.DESCRICAO],
            dica = "O que chegou: \"Caixas de tomate italiano\"",
            habilitado = !estado.enviando,
        )
        CampoDeTexto(
            valor = estado.categoria,
            aoMudar = viewModel::mudarCategoria,
            rotulo = "Categoria",
            erro = estado.erros[Campo.CATEGORIA],
            habilitado = !estado.enviando,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORIAS_COMUNS.forEach { categoria ->
                SuggestionChip(
                    onClick = { viewModel.mudarCategoria(categoria) },
                    label = { Text(categoria) },
                    enabled = !estado.enviando,
                )
            }
        }
        CampoDeTexto(
            valor = estado.kg,
            aoMudar = viewModel::mudarKg,
            rotulo = "Peso",
            erro = estado.erros[Campo.KG],
            sufixo = "kg",
            numerico = true,
            ultimo = true,
            habilitado = !estado.enviando,
        )

        // Campo só de leitura que abre o calendário: a área clicável cobre o campo inteiro.
        Box {
            OutlinedTextField(
                value = estado.validade?.let(::dia) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Validade") },
                placeholder = { Text("Toque para escolher") },
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                isError = estado.erros[Campo.VALIDADE] != null,
                supportingText = estado.erros[Campo.VALIDADE]?.let { texto -> { Text(texto) } },
                enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = !estado.enviando,
                        onClickLabel = "Escolher a data de validade",
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { escolhendoData = true },
                    ),
            )
        }

        Text(
            "Situação: ${StatusLote.DISPONIVEL.rotulo}. Todo lote entra em estoque como disponível; " +
                "a saída (distribuído ou perdido) é registrada depois, na lista de lotes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (escolhendoData) {
        val seletor = rememberDatePickerState(
            initialSelectedDateMillis = (estado.validade ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    // O seletor devolve meia-noite UTC do dia escolhido.
                    viewModel.mudarValidade(
                        seletor.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() },
                    )
                    escolhendoData = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = seletor)
        }
    }
}
