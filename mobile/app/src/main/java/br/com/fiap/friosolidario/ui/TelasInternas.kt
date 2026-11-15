package br.com.fiap.friosolidario.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.friosolidario.ui.alertas.AlertasScreen
import br.com.fiap.friosolidario.ui.alertas.ContadorDeAlertasViewModel
import br.com.fiap.friosolidario.ui.comum.vm
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
    const val ALERTAS = "alertas"
    const val DETALHE = "equipamentos/{id}"
    fun detalhe(id: Long) = "equipamentos/$id"
}

@Composable
fun TelasInternas(sessao: Sessao) {
    val nav = rememberNavController()
    val contador = vm { ContadorDeAlertasViewModel(it.alertas) }
    val alertasAbertos by contador.abertos.collectAsStateWithLifecycle()
    NavHost(navController = nav, startDestination = Rotas.PAINEL) {
        composable(Rotas.PAINEL) {
            PainelScreen(
                sessao = sessao,
                alertasAbertos = alertasAbertos,
                aoAbrirEquipamento = { nav.navigate(Rotas.detalhe(it)) },
                aoAbrirAlertas = { nav.navigate(Rotas.ALERTAS) },
                aoAbrirLotes = {},
                aoNovoEquipamento = {},
                aoAtualizar = contador::atualizar,
            )
        }
        composable(Rotas.ALERTAS) {
            AlertasScreen(
                aoVoltar = { nav.popBackStack() },
                aoAbrirEquipamento = { nav.navigate(Rotas.detalhe(it)) },
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
