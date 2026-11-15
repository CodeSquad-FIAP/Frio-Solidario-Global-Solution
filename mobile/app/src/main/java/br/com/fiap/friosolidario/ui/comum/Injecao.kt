package br.com.fiap.friosolidario.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.friosolidario.FrioApp
import br.com.fiap.friosolidario.core.AppContainer

@Composable
fun container(): AppContainer = (LocalContext.current.applicationContext as FrioApp).container

/** ViewModel da tela, criado com as dependências do [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> vm(key: String? = null, crossinline criar: (AppContainer) -> VM): VM {
    val dependencias = container()
    return viewModel(key = key, factory = viewModelFactory { initializer { criar(dependencias) } })
}
