package br.com.fiap.friosolidario.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.ui.painel.PainelScreen

object Rotas {
    const val PAINEL = "painel"
}

@Composable
fun TelasInternas(sessao: Sessao) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rotas.PAINEL) {
        composable(Rotas.PAINEL) {
            PainelScreen(sessao = sessao)
        }
    }
}
