package br.com.fiap.friosolidario.ui.comum

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.fiap.friosolidario.core.ErroApp

/**
 * Moldura dos dois cadastros: título com voltar, campos roláveis (cabem em tela pequena com o
 * teclado aberto), a frase de erro da API logo acima do botão e o botão travado durante o envio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Formulario(
    titulo: String,
    textoDoBotao: String,
    enviando: Boolean,
    erro: ErroApp?,
    aoVoltar: () -> Unit,
    aoEnviar: () -> Unit,
    campos: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar, enabled = !enviando) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { margens ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(margens)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = MargemDaTela, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                campos()

                // O que o app deixou passar e a API recusou (400), ou falta de rede: a frase
                // aparece aqui e tudo o que foi digitado continua nos campos.
                if (erro != null) {
                    FaixaDeErro(erro)
                }

                Button(shape = FormaDoBotao,
                    onClick = aoEnviar,
                    enabled = !enviando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    if (enviando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Salvando…")
                    } else {
                        Text(if (erro != null) "Tentar de novo" else textoDoBotao)
                    }
                }
            }
        }
    }
}

/** Campo de texto com a frase de erro embaixo (ou a dica, quando não há erro). */
@Composable
fun CampoDeTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    erro: String?,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    dica: String? = null,
    sufixo: String? = null,
    numerico: Boolean = false,
    ultimo: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        enabled = habilitado,
        isError = erro != null,
        supportingText = (erro ?: dica)?.let { texto -> { Text(texto) } },
        suffix = sufixo?.let { texto -> { Text(texto) } },
        keyboardOptions = KeyboardOptions(
            // Teclado de texto nos números: o numérico de vários aparelhos não tem o sinal de menos.
            keyboardType = if (numerico) KeyboardType.Ascii else KeyboardType.Text,
            imeAction = if (ultimo) ImeAction.Done else ImeAction.Next,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
