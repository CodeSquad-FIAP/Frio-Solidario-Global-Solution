package br.com.fiap.friosolidario.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.friosolidario.core.Sessao
import br.com.fiap.friosolidario.ui.alertas.AlertasScreen
import br.com.fiap.friosolidario.ui.alertas.ContadorDeAlertasViewModel
import br.com.fiap.friosolidario.ui.cadastro.EquipamentoFormScreen
import br.com.fiap.friosolidario.ui.comum.Fio
import br.com.fiap.friosolidario.ui.comum.vm
import br.com.fiap.friosolidario.ui.detalhe.BotaoDeRelatorio
import br.com.fiap.friosolidario.ui.detalhe.DetalheScreen
import br.com.fiap.friosolidario.ui.lotes.LoteFormScreen
import br.com.fiap.friosolidario.ui.lotes.LotesScreen
import br.com.fiap.friosolidario.ui.painel.PainelScreen

object Rotas {
    const val PAINEL = "painel"
    const val ALERTAS = "alertas"
    const val NOVO_EQUIPAMENTO = "equipamentos/novo"
    const val LOTES = "lotes"
    const val NOVO_LOTE = "lotes/novo"
    const val DETALHE = "equipamentos/{id}"
    fun detalhe(id: Long) = "equipamentos/$id"
}

/** As três áreas do app. Ficam na barra de baixo no celular e num trilho lateral em tela larga. */
private enum class Area(val rota: String, val rotulo: String, val icone: ImageVector) {
    PAINEL(Rotas.PAINEL, "Painel", Icons.Filled.Thermostat),
    ALERTAS(Rotas.ALERTAS, "Alertas", Icons.Filled.Notifications),
    LOTES(Rotas.LOTES, "Lotes", Icons.Filled.Inventory2),
}

/** A partir desta largura (tablet, TV) a navegação vai para a lateral. */
private val LARGURA_DO_TRILHO = 600.dp

@Composable
fun TelasInternas(sessao: Sessao) {
    val nav = rememberNavController()
    val contador = vm { ContadorDeAlertasViewModel(it.alertas) }
    val alertasAbertos by contador.abertos.collectAsStateWithLifecycle()
    val entrada by nav.currentBackStackEntryAsState()
    val areaAtual = Area.entries.firstOrNull { it.rota == entrada?.destination?.route }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largo = maxWidth >= LARGURA_DO_TRILHO
        Scaffold(
            bottomBar = {
                // Só nas áreas principais: detalhe e cadastros são telas de passagem, com "voltar".
                if (areaAtual != null && !largo) {
                    Column {
                        Fio()
                        NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                            Area.entries.forEach { area ->
                                NavigationBarItem(
                                    selected = area == areaAtual,
                                    onClick = { nav.irPara(area) },
                                    icon = { IconeDaArea(area, alertasAbertos) },
                                    label = { Text(area.rotulo) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { margens ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(margens)
                    .consumeWindowInsets(margens),
            ) {
                if (areaAtual != null && largo) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
                        Area.entries.forEach { area ->
                            NavigationRailItem(
                                selected = area == areaAtual,
                                onClick = { nav.irPara(area) },
                                icon = { IconeDaArea(area, alertasAbertos) },
                                label = { Text(area.rotulo) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                    Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onBackground))
                }
                Box(Modifier.weight(1f)) {
                    Telas(nav, sessao, aoAtualizarContador = contador::atualizar)
                }
            }
        }
    }
}

@Composable
private fun IconeDaArea(area: Area, alertasAbertos: Int?) {
    val abertos = alertasAbertos?.takeIf { area == Area.ALERTAS && it > 0 }
    BadgedBox(badge = { if (abertos != null) Badge { Text("$abertos") } }) {
        Icon(
            area.icone,
            contentDescription = when {
                abertos == null -> null
                abertos == 1 -> "1 alerta aberto"
                else -> "$abertos alertas abertos"
            },
        )
    }
}

/** Troca de área sem empilhar: voltar a partir de Alertas ou Lotes cai no Painel. */
private fun NavHostController.irPara(area: Area) {
    navigate(area.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun Telas(nav: NavHostController, sessao: Sessao, aoAtualizarContador: () -> Unit) {
    NavHost(navController = nav, startDestination = Rotas.PAINEL) {
        composable(Rotas.PAINEL) {
            PainelScreen(
                sessao = sessao,
                aoAbrirEquipamento = { nav.navigate(Rotas.detalhe(it)) },
                aoAbrirAlertas = { nav.irPara(Area.ALERTAS) },
                aoNovoEquipamento = { nav.navigate(Rotas.NOVO_EQUIPAMENTO) },
                aoAtualizar = aoAtualizarContador,
            )
        }
        composable(Rotas.NOVO_EQUIPAMENTO) {
            EquipamentoFormScreen(sessao = sessao, aoVoltar = { nav.popBackStack() }, aoSalvar = { nav.popBackStack() })
        }
        composable(Rotas.LOTES) {
            LotesScreen(sessao = sessao, aoNovoLote = { nav.navigate(Rotas.NOVO_LOTE) })
        }
        composable(Rotas.NOVO_LOTE) {
            LoteFormScreen(aoVoltar = { nav.popBackStack() }, aoSalvar = { nav.popBackStack() })
        }
        composable(Rotas.ALERTAS) {
            AlertasScreen(aoAbrirEquipamento = { nav.navigate(Rotas.detalhe(it)) })
        }
        composable(Rotas.DETALHE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            DetalheScreen(
                equipamentoId = entrada.arguments?.getLong("id") ?: return@composable,
                aoVoltar = { nav.popBackStack() },
                acoes = { equipamento -> BotaoDeRelatorio(equipamento) },
            )
        }
    }
}
