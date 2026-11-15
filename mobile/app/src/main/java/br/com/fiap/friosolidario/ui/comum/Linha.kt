package br.com.fiap.friosolidario.ui.comum

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.fiap.friosolidario.ui.tema.CorDeEstado

/** Margem lateral do conteúdo; as linhas vão de borda a borda e aplicam esta margem por dentro. */
val MargemDaTela = 20.dp

/** Cor do texto secundário dentro da linha atual (muda quando a linha está preenchida). */
private val LocalCorSuave = compositionLocalOf<Color?> { null }

@Composable
fun corSuave(): Color = LocalCorSuave.current ?: MaterialTheme.colorScheme.onSurfaceVariant

/** O fio grosso que separa as linhas, no lugar de cartões. */
@Composable
fun Fio(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onBackground))
}

/**
 * Uma linha de lista de borda a borda. Quando o estado pede ação, a linha inteira é preenchida com
 * a cor dele (o semáforo é a linha); em ordem, fica sem preenchimento. Termina com o fio.
 */
@Composable
fun LinhaSinal(
    cores: CorDeEstado,
    modifier: Modifier = Modifier,
    aoClicar: (() -> Unit)? = null,
    rotuloDoClique: String? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides cores.sobreFundo, LocalCorSuave provides cores.suave) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(cores.fundo)
                    .then(if (aoClicar != null) Modifier.clickable(onClickLabel = rotuloDoClique, onClick = aoClicar) else Modifier)
                    .padding(horizontal = MargemDaTela, vertical = 16.dp),
                content = conteudo,
            )
        }
        Fio()
    }
}

/** Botão de ação dentro de uma linha: inverte as cores da linha para continuar legível sobre ela. */
@Composable
fun BotaoDaLinha(texto: String, cores: CorDeEstado, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    val preenchida = cores.fundo != Color.Transparent
    Button(shape = FormaDoBotao,
        onClick = aoClicar,
        colors = if (preenchida) {
            ButtonDefaults.buttonColors(containerColor = cores.sobreFundo, contentColor = cores.fundo)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier.heightIn(min = 48.dp),
    ) { Text(texto) }
}

/**
 * Cabeçalho das telas principais: o nome em letra larga, uma linha de contexto e as ações à
 * direita, fechado por um fio. Fica no lugar da barra superior padrão.
 */
@Composable
fun CabecalhoDaTela(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acoes: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        androidx.compose.foundation.layout.Row(
            Modifier
                .fillMaxWidth()
                .padding(start = MargemDaTela, end = 8.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            acoes()
        }
        Fio()
    }
}
