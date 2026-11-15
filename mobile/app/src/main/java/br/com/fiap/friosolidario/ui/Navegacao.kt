package br.com.fiap.friosolidario.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.core.EstadoSessao
import br.com.fiap.friosolidario.ui.comum.Carregando
import br.com.fiap.friosolidario.ui.comum.container
import br.com.fiap.friosolidario.ui.login.LoginScreen

/**
 * A sessão decide o que aparece: sem ela, login; com ela, as telas internas. Logout e 401 mudam a
 * sessão e a troca acontece sozinha, em qualquer tela, sem cada uma saber navegar para o login.
 */
@Composable
fun Navegacao() {
    val armazem = container().sessao
    val estado by armazem.estado.collectAsStateWithLifecycle()
    val expirou by armazem.expirou.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val atual = estado) {
            EstadoSessao.Carregando -> Carregando()

            EstadoSessao.Ausente -> LoginScreen(sessaoExpirou = expirou)

            is EstadoSessao.Ativa -> {
                DisposableEffect(Unit) {
                    armazem.avisoDeExpiracaoVisto()
                    onDispose { }
                }
                // Outra pessoa entrou: pilha de telas e ViewModels começam do zero.
                key(atual.sessao.token) {
                    TelasInternas(atual.sessao)
                }
            }
        }
    }
}
