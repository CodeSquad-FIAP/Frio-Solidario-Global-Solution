package br.com.fiap.friosolidario.ui.comum

import br.com.fiap.friosolidario.ui.tema.FormaDoBotao
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.fiap.friosolidario.core.ErroApp

/** O que toda tela de dados pode estar mostrando. */
sealed interface EstadoTela<out T> {
    data object Carregando : EstadoTela<Nothing>
    data class Erro(val erro: ErroApp) : EstadoTela<Nothing>
    data class Conteudo<T>(val valor: T) : EstadoTela<T>
}

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun iconeDo(erro: ErroApp): ImageVector =
    if (erro == ErroApp.SemConexao) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline

private fun tituloDo(erro: ErroApp): String = when (erro) {
    ErroApp.SemConexao -> "Sem conexão"
    ErroApp.Servidor -> "Servidor com problema"
    ErroApp.SemPermissao -> "Sem permissão"
    ErroApp.NaoEncontrado -> "Não encontrado"
    else -> "Não deu certo"
}

/**
 * Tela cheia de erro, a mesma nas cinco telas: o que aconteceu e o botão de tentar de novo. Rola
 * para caber em tela pequena com fonte grande e para funcionar dentro do puxar-para-atualizar.
 */
@Composable
fun ErroComTentarDeNovo(erro: ErroApp, aoTentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargemDaTela, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(tituloDo(erro), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            erro.mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(shape = FormaDoBotao, onClick = aoTentar, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tentar de novo")
        }
    }
}

/** Faixa de erro para quando a tela já tem conteúdo (falha ao atualizar ou ao enviar). */
@Composable
fun FaixaDeErro(erro: ErroApp, modifier: Modifier = Modifier, aoTentar: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(iconeDo(erro), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(erro.mensagem, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (aoTentar != null) {
                TextButton(shape = FormaDoBotao,
                    onClick = aoTentar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                ) { Text("Tentar de novo") }
            }
        }
    }
}

/** Lista sem itens: diz o que apareceria aqui e, quando há, o próximo passo. O ícone não é usado. */
@Composable
fun ListaVazia(
    texto: String,
    modifier: Modifier = Modifier,
    detalhe: String? = null,
    @Suppress("UNUSED_PARAMETER") icone: ImageVector = Icons.Outlined.Inbox,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MargemDaTela, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.headlineMedium)
        if (detalhe != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                detalhe,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }
    }
}
