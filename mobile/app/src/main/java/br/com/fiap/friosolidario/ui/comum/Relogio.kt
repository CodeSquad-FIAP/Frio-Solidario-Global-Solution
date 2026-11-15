package br.com.fiap.friosolidario.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner as DonoDoCiclo
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant

/** "Agora", renovado a cada [intervaloMs], para o "há 4 min" não ficar parado na tela aberta. */
@Composable
fun agoraQueAnda(intervaloMs: Long = 30_000): State<Instant> = produceState(Instant.now()) {
    while (true) {
        delay(intervaloMs)
        value = Instant.now()
    }
}

/** Repete [acao] a cada [intervaloMs] enquanto a tela está visível (e uma vez ao voltar a ela). */
@Composable
fun EnquantoVisivel(intervaloMs: Long, acao: () -> Unit) {
    val dono = DonoDoCiclo.current
    androidx.compose.runtime.LaunchedEffect(dono, intervaloMs) {
        dono.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                acao()
                delay(intervaloMs)
            }
        }
    }
}
