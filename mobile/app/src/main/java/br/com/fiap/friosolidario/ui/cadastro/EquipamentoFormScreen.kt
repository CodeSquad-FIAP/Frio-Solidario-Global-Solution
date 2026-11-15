package br.com.fiap.friosolidario.ui.cadastro

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.data.TipoEquipamento
import br.com.fiap.friosolidario.dominio.Campo
import br.com.fiap.friosolidario.ui.comum.CampoDeTexto
import br.com.fiap.friosolidario.ui.comum.Formulario
import br.com.fiap.friosolidario.ui.comum.vm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipamentoFormScreen(sessao: Sessao, aoVoltar: () -> Unit, aoSalvar: () -> Unit) {
    val viewModel = vm { EquipamentoFormViewModel(sessao.instituicaoId, it.equipamentos) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // O painel recarrega ao voltar a ficar visível: o equipamento novo aparece sem reabrir o app.
    LaunchedEffect(estado.salvo) { if (estado.salvo) aoSalvar() }

    Formulario(
        titulo = "Novo equipamento",
        textoDoBotao = "Cadastrar equipamento",
        enviando = estado.enviando,
        erro = estado.erro,
        aoVoltar = aoVoltar,
        aoEnviar = viewModel::salvar,
    ) {
        Text(
            "Será cadastrado em ${sessao.instituicao}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoDeTexto(
            valor = estado.nome,
            aoMudar = viewModel::mudarNome,
            rotulo = "Nome",
            erro = estado.erros[Campo.NOME],
            dica = "Como a equipe chama: \"Freezer de proteínas\"",
            habilitado = !estado.enviando,
        )
        Column {
            Text("Tipo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoEquipamento.entries.forEachIndexed { indice, tipo ->
                    SegmentedButton(
                        selected = estado.tipo == tipo,
                        onClick = { viewModel.mudarTipo(tipo) },
                        enabled = !estado.enviando,
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoEquipamento.entries.size, baseShape = FormaDoBotao),
                        icon = {},
                    ) { Text(tipo.rotulo, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        // A faixa é o que o semáforo e a regra de alerta comparam com cada leitura.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoDeTexto(
                valor = estado.tempMin,
                aoMudar = viewModel::mudarTempMin,
                rotulo = "Temp. mínima",
                erro = estado.erros[Campo.TEMP_MIN],
                sufixo = "°C",
                numerico = true,
                habilitado = !estado.enviando,
                modifier = Modifier.weight(1f),
            )
            CampoDeTexto(
                valor = estado.tempMax,
                aoMudar = viewModel::mudarTempMax,
                rotulo = "Temp. máxima",
                erro = estado.erros[Campo.TEMP_MAX],
                sufixo = "°C",
                numerico = true,
                habilitado = !estado.enviando,
                modifier = Modifier.weight(1f),
            )
        }
        CampoDeTexto(
            valor = estado.umidMax,
            aoMudar = viewModel::mudarUmidMax,
            rotulo = "Umidade máxima",
            erro = estado.erros[Campo.UMID_MAX],
            dica = "Acima disso o equipamento entra em atenção",
            sufixo = "%",
            numerico = true,
            ultimo = true,
            habilitado = !estado.enviando,
        )
    }
}
