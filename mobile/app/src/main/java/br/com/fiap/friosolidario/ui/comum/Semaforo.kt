package br.com.fiap.friosolidario.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.fiap.friosolidario.dominio.Semaforo
import br.com.fiap.friosolidario.ui.tema.CorDeEstado
import br.com.fiap.friosolidario.ui.tema.LocalCoresDeEstado

@Composable
@ReadOnlyComposable
fun Semaforo.cores(): CorDeEstado = with(LocalCoresDeEstado.current) {
    when (this@cores) {
        Semaforo.VERDE -> ok
        Semaforo.AMARELO -> atencao
        Semaforo.VERMELHO -> critico
        Semaforo.SEM_LEITURA -> neutro
    }
}

/** A cor nunca vai sozinha: cada estado tem ícone e texto próprios (daltonismo, sol na tela). */
val Semaforo.icone: ImageVector
    get() = when (this) {
        Semaforo.VERDE -> Icons.Filled.CheckCircle
        Semaforo.AMARELO -> Icons.Filled.Warning
        Semaforo.VERMELHO -> Icons.Filled.Error
        Semaforo.SEM_LEITURA -> Icons.Outlined.HelpOutline
    }

/** Etiqueta colorida com ícone e texto, usada para o semáforo e para a severidade do alerta. */
@Composable
fun Etiqueta(texto: String, cores: CorDeEstado, modifier: Modifier = Modifier, icone: ImageVector? = null) {
    Row(
        modifier
            .background(cores.fundo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = cores.forte, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = cores.sobreFundo)
    }
}
