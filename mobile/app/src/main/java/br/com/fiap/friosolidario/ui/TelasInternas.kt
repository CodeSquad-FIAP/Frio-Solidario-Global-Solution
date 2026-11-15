package br.com.fiap.friosolidario.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.ui.detalhe.DetalheScreen
import br.com.fiap.friosolidario.ui.painel.PainelScreen

object Rotas {
    const val PAINEL = "painel"
    const val DETALHE = "equipamentos/{id}"
    fun detalhe(id: Long) = "equipamentos/$id"
}

@Composable
fun TelasInternas(sessao: Sessao) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rotas.PAINEL) {
        composable(Rotas.PAINEL) {
            PainelScreen(
                sessao = sessao,
                alertasAbertos = null,
                aoAbrirEquipamento = { nav.navigate(Rotas.detalhe(it)) },
                aoAbrirAlertas = {},
                aoAbrirLotes = {},
                aoNovoEquipamento = {},
                aoAtualizar = {},
            )
        }
        composable(Rotas.DETALHE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            DetalheScreen(
                equipamentoId = entrada.arguments?.getLong("id") ?: return@composable,
                aoVoltar = { nav.popBackStack() },
            )
        }
    }
}
