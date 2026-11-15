package br.com.fiap.friosolidario.ui.painel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.ui.comum.container
import kotlinx.coroutines.launch

/** Destino do login. A lista de equipamentos com semáforo entra no próximo commit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(sessao: Sessao) {
    val auth = container().auth
    val escopo = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sessao.instituicao) },
                actions = { TextButton(onClick = { escopo.launch { auth.sair() } }) { Text("Sair") } },
            )
        },
    ) { margens ->
        Column(
            Modifier.fillMaxSize().padding(margens),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Olá, ${sessao.nome}")
        }
    }
}
